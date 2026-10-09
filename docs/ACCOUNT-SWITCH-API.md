# In-game account switching: launcher bridge (contract)

The game never signs in to Microsoft itself and never writes tokens to disk. While a Shard
instance runs, Shard Launcher serves a tiny HTTP API on the loopback interface; the client asks it
for the accounts and, when the player switches, for a fresh session of the chosen account.

## Discovery

`launcher-info.json` (written before every launch) gains an optional object:

```json
"accountBridge": { "url": "http://127.0.0.1:53123", "secret": "<64 hex chars>" }
```

- Port: random free port, bound to `127.0.0.1` only, opened when the game is launched and closed
  when that game process exits.
- Secret: 32 random bytes as hex, new for every launch. Every request must send
  `Authorization: Bearer <secret>`; anything else gets 401. Requests with an `Origin` header get 403
  (browsers cannot reach it).
- Absent `accountBridge` (other launchers, older launchers): the client hides account switching.

## Endpoints (JSON, UTF-8)

`GET /v1/accounts`
```json
{ "active": "<accountId>", "accounts": [ { "id": "<accountId>", "name": "OhMarkerr", "uuid": "<32 hex, no dashes>" } ] }
```

`POST /v1/accounts/<accountId>/session` (empty body). The launcher refreshes the account's
Minecraft token if needed and answers:
```json
{ "name": "OhMarkerr", "uuid": "<32 hex>", "accessToken": "<minecraft access token>", "xuid": "<optional>", "clientId": "<optional>" }
```
Errors: `404 {"error":"unknown account"}`, `409 {"error":"sign in again in Shard Launcher"}`
(refresh failed), `500 {"error":"..."}`. The launcher also marks that account as selected, so the
next launch uses it.

`POST /v1/accounts/add` (empty body): the launcher window comes to the front and starts its normal
Microsoft sign-in. Answers `202 {}` immediately; the client polls `GET /v1/accounts`.

## Client behaviour

- The title screen and the pause-free multiplayer screen show the current account (head + name)
  with a switcher listing the bridge's accounts and "Add account".
- Switching: fetch the session, replace `Minecraft.user` (mixin accessor), reset the profile key
  pair manager / session-bound services as 1.21.11 requires, and re-run Shard's API sign-in so
  capes and tokens follow the new account. Only allowed outside a world.
- The access token stays in memory only.
