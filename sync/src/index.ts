// HATI² live sync. The server keeps one opaque, end-to-end encrypted blob per
// room and a version number. It never sees the key or any plaintext.
// Protocol: ../README.md.
import { DurableObject } from "cloudflare:workers";

export interface Env {
  ROOMS: DurableObjectNamespace<Room>;
  WRITE_LIMIT?: RateLimit;
}

export const ALLOWED_ORIGINS = new Set([
  "https://p1xellzxc.github.io",
  "https://appassets.androidplatform.net",
]);
export const MAX_BLOB_BYTES = 512 * 1024;
export const IDLE_MS = 180 * 24 * 60 * 60 * 1000;
export const MAX_SOCKETS = 64;
const ROOM_ID = /^[A-Za-z0-9_-]{22,64}$/;
const B64URL = /^[A-Za-z0-9_-]+$/;
const TOUCH_EVERY_MS = 24 * 60 * 60 * 1000;

type Blob = { v: 1; iv: string; ct: string };
type Stored = { version: number; blob: Blob | null; touched: number };

const json = (body: unknown, status = 200, headers: HeadersInit = {}) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json", "cache-control": "no-store", ...headers },
  });

function cors(origin: string | null): Record<string, string> {
  if (!origin || !ALLOWED_ORIGINS.has(origin)) return { vary: "Origin" };
  return {
    "access-control-allow-origin": origin,
    "access-control-allow-methods": "GET, PUT, OPTIONS",
    "access-control-allow-headers": "content-type",
    "access-control-max-age": "86400",
    vary: "Origin",
  };
}

export function isBlob(b: unknown): b is Blob {
  if (!b || typeof b !== "object") return false;
  const o = b as Record<string, unknown>;
  return (
    Object.keys(o).length === 3 &&
    o.v === 1 &&
    typeof o.iv === "string" && o.iv.length === 16 && B64URL.test(o.iv) && // 96-bit IV
    typeof o.ct === "string" && o.ct.length >= 22 && B64URL.test(o.ct) // at least the 16-byte tag
  );
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    const origin = req.headers.get("origin");
    // Browsers from other sites get nothing. Requests without an Origin
    // (curl, tests) are allowed: CORS protects users, not the server.
    if (origin && !ALLOWED_ORIGINS.has(origin)) return json({ error: "origin" }, 403, cors(null));
    const h = cors(origin);

    const m = new URL(req.url).pathname.match(/^\/v1\/rooms\/([^/]+)(\/ws)?$/);
    if (!m) return json({ error: "not_found" }, 404, h);
    if (!ROOM_ID.test(m[1])) return json({ error: "bad_room" }, 400, h);
    const ws = Boolean(m[2]);

    if (req.method === "OPTIONS" && !ws) return new Response(null, { status: 204, headers: h });
    if (ws) {
      if (req.method !== "GET" || req.headers.get("upgrade")?.toLowerCase() !== "websocket")
        return json({ error: "upgrade_required" }, 426, h);
    } else if (req.method === "PUT") {
      const len = Number(req.headers.get("content-length") ?? 0);
      if (len > MAX_BLOB_BYTES + 1024) return json({ error: "too_large" }, 413, h);
      if (env.WRITE_LIMIT) {
        const ip = req.headers.get("cf-connecting-ip") ?? "unknown";
        const { success } = await env.WRITE_LIMIT.limit({ key: ip });
        if (!success) return json({ error: "rate_limited" }, 429, { ...h, "retry-after": "60" });
      }
    } else if (req.method !== "GET") {
      return json({ error: "method" }, 405, { ...h, allow: "GET, PUT, OPTIONS" });
    }

    const stub = env.ROOMS.get(env.ROOMS.idFromName(m[1]));
    const res = await stub.fetch(req);
    if (ws) return res; // 101 responses can't carry new headers; CORS doesn't apply to WebSockets.
    const out = new Response(res.body, res);
    for (const [k, v] of Object.entries(h)) out.headers.set(k, v);
    return out;
  },
} satisfies ExportedHandler<Env>;

export class Room extends DurableObject<Env> {
  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);
    // Keepalive pings are answered without waking the object.
    ctx.setWebSocketAutoResponse(new WebSocketRequestResponsePair("ping", "pong"));
  }

  private async load(): Promise<Stored> {
    return (await this.ctx.storage.get<Stored>("room")) ?? { version: 0, blob: null, touched: 0 };
  }

  private async touch(s: Stored): Promise<void> {
    s.touched = Date.now();
    await this.ctx.storage.put("room", s);
    await this.ctx.storage.setAlarm(s.touched + IDLE_MS);
  }

  async fetch(req: Request): Promise<Response> {
    if (req.headers.get("upgrade")?.toLowerCase() === "websocket") return this.join();
    const s = await this.load();
    if (req.method === "GET") {
      if (s.blob && Date.now() - s.touched > TOUCH_EVERY_MS) await this.touch(s);
      return json({ version: s.version, blob: s.blob });
    }

    // PUT: compare-and-set on the version.
    const text = await req.text();
    if (text.length > MAX_BLOB_BYTES + 1024) return json({ error: "too_large" }, 413);
    let body: { baseVersion?: unknown; blob?: unknown };
    try {
      body = JSON.parse(text);
    } catch {
      return json({ error: "bad_json" }, 400);
    }
    const { baseVersion, blob } = body ?? {};
    if (!Number.isSafeInteger(baseVersion) || (baseVersion as number) < 0 || !isBlob(blob))
      return json({ error: "bad_body" }, 400);
    if (JSON.stringify(blob).length > MAX_BLOB_BYTES) return json({ error: "too_large" }, 413);
    if (baseVersion !== s.version) return json({ version: s.version, blob: s.blob }, 409);

    const next: Stored = { version: s.version + 1, blob, touched: 0 };
    await this.touch(next);
    const msg = JSON.stringify({ version: next.version });
    for (const peer of this.ctx.getWebSockets()) {
      try {
        peer.send(msg);
      } catch {
        // closed between getWebSockets() and send(); the client reconnects and GETs.
      }
    }
    return json({ version: next.version });
  }

  private async join(): Promise<Response> {
    if (this.ctx.getWebSockets().length >= MAX_SOCKETS) return json({ error: "room_full" }, 429);
    const pair = new WebSocketPair();
    this.ctx.acceptWebSocket(pair[1]);
    // Tell the newcomer where the room is now, so it can catch up at once.
    pair[1].send(JSON.stringify({ version: (await this.load()).version }));
    return new Response(null, { status: 101, webSocket: pair[0] });
  }

  // Clients only listen; anything else they send is ignored.
  async webSocketMessage(): Promise<void> {}

  async webSocketClose(ws: WebSocket, code: number): Promise<void> {
    try {
      ws.close([1005, 1006, 1015].includes(code) ? 1000 : code, "bye");
    } catch {
      // already closed
    }
  }

  async alarm(): Promise<void> {
    const s = await this.ctx.storage.get<Stored>("room");
    if (s && Date.now() - s.touched < IDLE_MS) {
      await this.ctx.storage.setAlarm(s.touched + IDLE_MS);
      return;
    }
    for (const ws of this.ctx.getWebSockets()) ws.close(1001, "expired");
    await this.ctx.storage.deleteAlarm();
    await this.ctx.storage.deleteAll();
  }
}
