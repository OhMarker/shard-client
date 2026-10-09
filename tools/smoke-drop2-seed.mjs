// Prepares a local Shard API (`npx wrangler dev --local --port 8787 --var DEV_AUTH:1` in shard-api)
// for `-PsmokeOnly=drop2`: the owner grants ShardSmoke and ShardFriend the second drop (Shard,
// Moonlit Tide and Halloween capes, Halloween shield and bandana), and ShardFriend wears the
// Halloween set. The smoke pass equips ShardSmoke's itself.
// Usage: node tools/smoke-drop2-seed.mjs [baseUrl]
const BASE = process.argv[2] ?? 'http://127.0.0.1:8787'
const OWNER = { name: 'OhMarkerr', uuid: '4a5e875e479a43f1bfc16c6bd326643d' }
const SMOKE = { name: 'ShardSmoke', uuid: 'cc971d241e2e3c7e91408043ba1bd54a' }
const FRIEND = { name: 'ShardFriend', uuid: '5f3c1a2e0000400080000000000000aa' }
const ITEMS = ['cape-shard', 'cape-ocean', 'cape-halloween', 'shield-halloween', 'bandana-halloween']
async function call(method, path, { token, body } = {}) {
  const res = await fetch(BASE + path, { method, headers: { 'content-type': 'application/json', ...(token ? { authorization: `Bearer ${token}` } : {}) }, body: body === undefined ? undefined : JSON.stringify(body) })
  return { status: res.status, data: await res.json().catch(() => null) }
}
async function signIn(p) {
  const { data: c } = await call('POST', '/v1/auth/challenge')
  return (await call('POST', '/v1/auth/verify', { body: { username: p.name, serverId: c.serverId, devUuid: p.uuid } })).data.session
}
const owner = await signIn(OWNER)
const friend = await signIn(FRIEND)
await signIn(SMOKE) // creates the player row
for (const p of [SMOKE, FRIEND]) for (const id of ITEMS) {
  const r = await call('POST', '/v1/admin/grant', { token: owner, body: { player: p.name, id } })
  console.log(`grant ${id} to ${p.name}: ${r.status}`)
}
for (const [slot, id] of [['cape', 'cape-halloween'], ['shield', 'shield-halloween'], ['bandana', 'bandana-halloween']]) {
  const r = await call('POST', '/v1/equip', { token: friend, body: { slot, id } })
  console.log(`ShardFriend equips ${id}: ${r.status}`)
}
console.log(JSON.stringify((await call('GET', `/v1/equipped?v=2&uuids=${FRIEND.uuid},${SMOKE.uuid}`)).data))
