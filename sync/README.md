# HATI² sync server

Live sync for shared volumes, with no accounts. A Cloudflare Worker routes each volume's **room** to its own Durable Object (SQLite-backed, free plan). The room keeps one end-to-end encrypted blob and a version number. The server never sees the key or any plaintext: no names, no amounts, no titles.

The app works fully offline. The server is only a mailbox.

## Invite link

```
https://p1xellzxc.github.io/HATI_v2/app/#join=<room>.<key>
```

- `room`: 16 random bytes, base64url without padding (22 chars). The server accepts `[A-Za-z0-9_-]{22,64}`.
- `key`: 32 random bytes (AES-GCM-256 raw key), base64url without padding (43 chars).
- Both live in the `#fragment`, so browsers never send them to any server (GitHub Pages included). The QR code is the same link.
- Whoever has the link can read and write the volume. Treat it like a house key.

Generate both with `crypto.getRandomValues`. Keep `{room, key}` with the volume on the device (for example `volume.sync = {room, key, version}`), never in exports sent elsewhere unless that is a deliberate re-share.

## Blob format

```json
{ "v": 1, "iv": "<16 base64url chars>", "ct": "<base64url>" }
```

- `iv`: a fresh 12-byte random IV for **every** write. Never reuse an IV with the same key.
- `ct`: `AES-GCM(key, iv, plaintext)`, tag included (WebCrypto's output as is). Plaintext is `UTF-8(JSON.stringify(volume))`; compress it first if you like (then bump to `v: 2`).
- Additional data: the room id as UTF-8 bytes (`additionalData` in WebCrypto), so a blob copied into another room fails to decrypt.
- base64url: `+`→`-`, `/`→`_`, no `=` padding.
- The server checks only the shape: exactly these three keys, `v === 1`, an IV of 16 chars, base64url text. The serialized blob may be at most **512 KB**.

```js
const key = await crypto.subtle.importKey("raw", b64urlDecode(k), "AES-GCM", false, ["encrypt", "decrypt"]);
const iv = crypto.getRandomValues(new Uint8Array(12));
const ad = new TextEncoder().encode(room);
const ct = await crypto.subtle.encrypt({ name: "AES-GCM", iv, additionalData: ad }, key, new TextEncoder().encode(JSON.stringify(vol)));
const blob = { v: 1, iv: b64url(iv), ct: b64url(new Uint8Array(ct)) };
```

## HTTP

Base URL: the Worker's address, printed by the deploy step (`https://hati-sync.<account>.workers.dev`). Keep it in one constant in the client.

| Request | Answer |
|---|---|
| `GET /v1/rooms/:room` | `200 {"version": n, "blob": Blob \| null}`. A room nobody wrote to is `{"version": 0, "blob": null}`. |
| `PUT /v1/rooms/:room` body `{"baseVersion": n, "blob": Blob}` | `200 {"version": n+1}` when `baseVersion` equals the room's version. |
| | `409 {"version": m, "blob": Blob}` when it does not: someone wrote first. |
| `GET /v1/rooms/:room/ws` (WebSocket) | See below. |

Errors are `{"error": "..."}`: `400 bad_room` (id), `400 bad_json` / `bad_body` (shape), `403 origin`, `404 not_found`, `413 too_large`, `426 upgrade_required`, `429 rate_limited` (60 writes a minute per IP, `Retry-After: 60`) or `room_full` (64 sockets).

Send `content-type: application/json` on PUT. Responses are `cache-control: no-store`.

## Write loop (no write is ever lost)

1. `local` = the volume after the user's change. `base` = the version you last saw.
2. `PUT {baseVersion: base, blob: encrypt(local)}`.
3. `200` → store the new version. Done.
4. `409` → decrypt the returned blob, `merged = cleanVolume(mergeVolumes(local, remote))` after `validVolume(remote)`, set `base` to the returned version, go to 2.
5. Network error, 429 or 5xx → keep the change in the outbox, retry with backoff (1 s, 2 s, 4 s … max 60 s) and on `online` / socket reconnect.

The server does compare-and-set only; merging is the client's job, with the existing `mergeVolumes` (merge by id, deletions as tombstones), so concurrent edits from two phones both survive.

## WebSocket

`GET /v1/rooms/:room/ws` with `Upgrade: websocket`.

- On connect the server sends `{"version": n}` at once.
- After every successful PUT it sends `{"version": n}` to every socket in the room, the writer's own included.
- When a message's version is greater than yours: `GET` the room, decrypt, merge into the local volume (if you have unsent changes, run the write loop after merging).
- Send the text `ping` to keep the socket open; the server answers `pong` without waking up. Every 30 s is enough. Anything else you send is ignored.
- Reconnect with backoff when it closes. Close code `1001 expired` means the room was deleted.
- The socket only says "something changed". Data always goes through GET, so a missed message costs nothing: a `GET` on reconnect catches up.

## Limits and lifetime

- Blob: 512 KB serialized.
- Writes: 60 a minute per client IP (Cloudflare rate-limiting binding `WRITE_LIMIT`).
- A room unused (no write, and no read for a day) for **180 days** is deleted by a Durable Object alarm. The volume itself stays on every phone.
- CORS allows only `https://p1xellzxc.github.io` (web app) and `https://appassets.androidplatform.net` (the APK's WebView). Browser requests from any other origin get `403`, WebSockets included. Requests without an `Origin` header (tools) are allowed.

## Develop

```sh
cd sync
npm ci
npm test          # vitest in the Workers runtime (@cloudflare/vitest-plugin)
npm run typecheck
npx wrangler dev  # local server on http://localhost:8787
```

Deploy is `.github/workflows/sync-deploy.yml`: tests on every PR touching `sync/`, deploy on push to `main`. It needs the repo secrets `CLOUDFLARE_API_TOKEN` (a token with the "Edit Cloudflare Workers" template) and `CLOUDFLARE_ACCOUNT_ID`; without them the deploy is skipped with a notice, not failed.
