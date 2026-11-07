import { SELF, env, runDurableObjectAlarm, runInDurableObject } from "cloudflare:test";
import { describe, expect, it } from "vitest";
import { IDLE_MS, MAX_BLOB_BYTES } from "../src/index";

const BASE = "https://sync.test/v1/rooms/";
const APP = "https://p1xellzxc.github.io";
const APK = "https://appassets.androidplatform.net";
let n = 0;
const room = () => `room${String(++n).padStart(4, "0")}_abcdefghijklmnop`; // 25 chars
const blob = (ct = "A".repeat(40)) => ({ v: 1, iv: "B".repeat(16), ct });

function put(id: string, baseVersion: unknown, b: unknown = blob(), ip = "1.1.1.1", origin = APP) {
  return SELF.fetch(BASE + id, {
    method: "PUT",
    headers: { "content-type": "application/json", origin, "cf-connecting-ip": ip },
    body: JSON.stringify({ baseVersion, blob: b }),
  });
}

async function openSocket(id: string): Promise<{ ws: WebSocket; msgs: unknown[]; next: () => Promise<unknown> }> {
  const res = await SELF.fetch(BASE + id + "/ws", { headers: { upgrade: "websocket", origin: APK } });
  expect(res.status).toBe(101);
  const ws = res.webSocket!;
  const msgs: unknown[] = [];
  const waiters: ((m: unknown) => void)[] = [];
  ws.addEventListener("message", (e) => {
    const m = JSON.parse(e.data as string);
    const w = waiters.shift();
    w ? w(m) : msgs.push(m);
  });
  ws.accept();
  const next = () =>
    msgs.length
      ? Promise.resolve(msgs.shift())
      : new Promise<unknown>((resolve, reject) => {
          waiters.push(resolve);
          setTimeout(() => reject(new Error("no message")), 2000);
        });
  return { ws, msgs, next };
}

describe("rooms", () => {
  it("starts empty at version 0", async () => {
    const res = await SELF.fetch(BASE + room(), { headers: { origin: APP } });
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({ version: 0, blob: null });
  });

  it("stores a blob and returns it with its version", async () => {
    const id = room();
    const res = await put(id, 0, blob("CIPHERTEXT_ONLY_1234567890"));
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({ version: 1 });
    const got = await (await SELF.fetch(BASE + id)).json();
    expect(got).toEqual({ version: 1, blob: blob("CIPHERTEXT_ONLY_1234567890") });
  });

  it("rejects a stale baseVersion with 409 and the current blob, and no write is lost", async () => {
    const id = room();
    expect((await put(id, 0, blob("A".repeat(30)))).status).toBe(200);
    // A second phone also started from version 0.
    const stale = await put(id, 0, blob("B".repeat(30)));
    expect(stale.status).toBe(409);
    expect(await stale.json()).toEqual({ version: 1, blob: blob("A".repeat(30)) });
    // It merges and retries on top of version 1.
    const retry = await put(id, 1, blob("C".repeat(30)));
    expect(await retry.json()).toEqual({ version: 2 });
    // A version from the future is also a conflict.
    expect((await put(id, 9)).status).toBe(409);
  });

  it("enforces the 512 KB blob limit", async () => {
    const id = room();
    const fits = "A".repeat(MAX_BLOB_BYTES - 64);
    expect((await put(id, 0, blob(fits))).status).toBe(200);
    const big = await put(id, 1, blob("A".repeat(MAX_BLOB_BYTES)));
    expect(big.status).toBe(413);
    expect((await (await SELF.fetch(BASE + id)).json() as { version: number }).version).toBe(1);
  });

  it("rejects bad room ids", async () => {
    for (const bad of ["short", "a".repeat(21), "has.dot_aaaaaaaaaaaaaaaaaaaa", "a".repeat(65), "%2e%2e%2faaaaaaaaaaaaaaaaaaaaa"]) {
      const res = await SELF.fetch(BASE + bad);
      expect(res.status, bad).toBe(400);
    }
    expect((await SELF.fetch("https://sync.test/v1/rooms/")).status).toBe(404);
  });

  it("rejects bodies that are not an encrypted blob", async () => {
    const id = room();
    expect((await put(id, 0, { v: 1, iv: "B".repeat(16), ct: "A".repeat(30), name: "Ana" })).status).toBe(400);
    expect((await put(id, 0, { v: 2, iv: "B".repeat(16), ct: "A".repeat(30) })).status).toBe(400);
    expect((await put(id, 0, { v: 1, iv: "short", ct: "A".repeat(30) })).status).toBe(400);
    expect((await put(id, 0, { v: 1, iv: "B".repeat(16), ct: "not base64url!!!!!!!!!!!" })).status).toBe(400);
    expect((await put(id, -1)).status).toBe(400);
    expect((await put(id, "0")).status).toBe(400);
    const raw = await SELF.fetch(BASE + id, { method: "PUT", body: "{nope" });
    expect(raw.status).toBe(400);
  });

  it("rate-limits writes per IP", async () => {
    const id = room();
    let version = 0;
    for (let i = 0; i < 60; i++) {
      const res = await put(id, version, blob(), "9.9.9.9");
      expect(res.status).toBe(200);
      version++;
    }
    const limited = await put(id, version, blob(), "9.9.9.9");
    expect(limited.status).toBe(429);
    // Someone else is not affected.
    expect((await put(id, version, blob(), "8.8.8.8")).status).toBe(200);
  });
});

describe("CORS", () => {
  it("answers preflight for the two app origins", async () => {
    for (const origin of [APP, APK]) {
      const res = await SELF.fetch(BASE + room(), {
        method: "OPTIONS",
        headers: { origin, "access-control-request-method": "PUT", "access-control-request-headers": "content-type" },
      });
      expect(res.status).toBe(204);
      expect(res.headers.get("access-control-allow-origin")).toBe(origin);
      expect(res.headers.get("access-control-allow-methods")).toContain("PUT");
      expect(res.headers.get("access-control-allow-headers")).toContain("content-type");
    }
  });

  it("adds the allow-origin header to real responses, including 409", async () => {
    const id = room();
    await put(id, 0);
    const res = await put(id, 0, blob(), "2.2.2.2", APK);
    expect(res.status).toBe(409);
    expect(res.headers.get("access-control-allow-origin")).toBe(APK);
  });

  it("refuses other origins, for HTTP and WebSocket", async () => {
    for (const origin of ["https://evil.example", "http://p1xellzxc.github.io", "https://p1xellzxc.github.io.evil.example"]) {
      const res = await SELF.fetch(BASE + room(), { headers: { origin } });
      expect(res.status, origin).toBe(403);
      expect(res.headers.get("access-control-allow-origin")).toBeNull();
      const ws = await SELF.fetch(BASE + room() + "/ws", { headers: { origin, upgrade: "websocket" } });
      expect(ws.status).toBe(403);
    }
  });
});

describe("WebSocket", () => {
  it("sends the current version on connect and broadcasts each new version to every peer", async () => {
    const id = room();
    await put(id, 0);
    const a = await openSocket(id);
    const b = await openSocket(id);
    expect(await a.next()).toEqual({ version: 1 });
    expect(await b.next()).toEqual({ version: 1 });
    expect((await put(id, 1)).status).toBe(200);
    expect(await a.next()).toEqual({ version: 2 });
    expect(await b.next()).toEqual({ version: 2 });
    // A rejected write broadcasts nothing.
    expect((await put(id, 0)).status).toBe(409);
    expect((await put(id, 2)).status).toBe(200);
    expect(await a.next()).toEqual({ version: 3 });
    a.ws.close();
    b.ws.close();
  });

  it("does not leak to other rooms", async () => {
    const id = room();
    const other = await openSocket(room());
    expect(await other.next()).toEqual({ version: 0 });
    await put(id, 0);
    await expect(other.next()).rejects.toThrow("no message");
    other.ws.close();
  });

  it("needs an upgrade header", async () => {
    expect((await SELF.fetch(BASE + room() + "/ws")).status).toBe(426);
  });
});

describe("expiry", () => {
  it("keeps a room alive while it is used and deletes it after 180 idle days", async () => {
    const id = room();
    await put(id, 0);
    const stub = env.ROOMS.get(env.ROOMS.idFromName(id));
    const alarm = await runInDurableObject(stub, (_o, state) => state.storage.getAlarm());
    expect(alarm).toBeGreaterThan(Date.now() + IDLE_MS - 60_000);

    // Alarm fires while the room is still fresh: it is kept and rescheduled.
    expect(await runDurableObjectAlarm(stub)).toBe(true);
    expect((await (await SELF.fetch(BASE + id)).json() as { version: number }).version).toBe(1);

    // Pretend the last use was 181 days ago.
    await runInDurableObject(stub, async (_o, state) => {
      const s = (await state.storage.get<{ touched: number }>("room"))!;
      await state.storage.put("room", { ...s, touched: Date.now() - IDLE_MS - 86_400_000 });
    });
    expect(await runDurableObjectAlarm(stub)).toBe(true);
    expect(await (await SELF.fetch(BASE + id)).json()).toEqual({ version: 0, blob: null });
  });
});
