// Prepares a local Shard API (`npx wrangler dev --local --port 8787 --var DEV_AUTH:1` in shard-api)
// for `-PsmokeOnly=wardrobe` (the in-game Cosmetics tab): ShardSmoke owns the OhMarker set, the
// Shard and Halloween capes and the Halloween shield and bandana, but not the Moonlit Tide cape (so
// a locked tile shows), and wears the OhMarker shield only. The pass equips and takes off the cape.
// Usage: node tools/smoke-wardrobe-seed.mjs [baseUrl]
const BASE = process.argv[2] ?? 'http://127.0.0.1:8787'
const OWNER = { name: 'OhMarkerr', uuid: '4a5e875e479a43f1bfc16c6bd326643d' }
const SMOKE = { name: 'ShardSmoke', uuid: 'cc971d241e2e3c7e91408043ba1bd54a' }
const OWNED = ['cape-ohmarker', 'shield-ohmarker', 'bandana-ohmarker', 'cape-shard', 'cape-halloween', 'shield-halloween', 'bandana-halloween']
const LOCKED = ['cape-ocean']
async function call(method, path, { token, body } = {}) {
  const res = await fetch(BASE + path, { method, headers: { 'content-type': 'application/json', ...(token ? { authorization: `Bearer ${token}` } : {}) }, body: body === undefined ? undefined : JSON.stringify(body) })
  return { status: res.status, data: await res.json().catch(() => null) }
}
async function signIn(p) {
  const { data: c } = await call('POST', '/v1/auth/challenge')
  return (await call('POST', '/v1/auth/verify', { body: { username: p.name, serverId: c.serverId, devUuid: p.uuid } })).data.session
}
const owner = await signIn(OWNER)
const smoke = await signIn(SMOKE)
for (const id of OWNED) console.log(`grant ${id}: ${(await call('POST', '/v1/admin/grant', { token: owner, body: { player: SMOKE.name, id } })).status}`)
for (const id of LOCKED) console.log(`revoke ${id}: ${(await call('POST', '/v1/admin/revoke', { token: owner, body: { player: SMOKE.name, id } })).status}`)
for (const [slot, id] of [['cape', null], ['shield', 'shield-ohmarker'], ['bandana', null]]) {
  console.log(`ShardSmoke ${slot} -> ${id}: ${(await call('POST', '/v1/equip', { token: smoke, body: { slot, id } })).status}`)
}
console.log(JSON.stringify((await call('GET', `/v1/equipped?v=2&uuids=${SMOKE.uuid}`)).data))
