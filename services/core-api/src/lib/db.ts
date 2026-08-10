import { drizzle } from "drizzle-orm/node-postgres";
import { Pool } from "pg";
import { env } from "../config/env";
import * as schema from "../db/schema";

export const pool = new Pool({
  connectionString: env.DATABASE_URL,
  // Defaults are unbounded, which lets a burst of slow queries queue forever behind a full pool
  // instead of failing fast. The app opens several catalog requests at once on launch.
  max: 10,
  connectionTimeoutMillis: 10_000,
  idleTimeoutMillis: 30_000,
  // Nothing this API serves should legitimately run for half a minute; without a ceiling a single
  // pathological query holds a connection until the client gives up.
  statement_timeout: 20_000,
});

// Without this handler an error on an *idle* client is emitted as an unhandled 'error' event on the
// pool, which terminates the process. Running a single replica, that means every request during the
// restart window gets a platform 502 - the intermittent, self-healing 502s that look like nothing
// is wrong by the time you check. Postgres drops idle connections routinely, so this fires in
// normal operation; pg discards the dead client and the next query gets a fresh one.
pool.on("error", (error) => {
  console.error("Idle Postgres client error (connection discarded):", error);
});

export const db = drizzle(pool, { schema });

export async function closeDb() {
  await pool.end();
}
