import type { Env as SyncEnv } from "../src/index";

declare global {
  namespace Cloudflare {
    interface Env extends SyncEnv {}
  }
}
