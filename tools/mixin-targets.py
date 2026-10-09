# Static mixin check for an unobfuscated (26.x) node: every injector's method and @At target
# (INVOKE / FIELD owner, name and descriptor, NEW constructor) must be in the target method's
# bytecode on that version's jar. Run `./gradlew :<mc>:compileJava :<mc>:processResources` first.
# Usage: python tools/mixin-targets.py <mc>        (prints "problems: 0" when everything matches)
import os, re, subprocess, sys, json
ver = sys.argv[1]
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
gen = os.path.join(root, "versions", ver, "build", "generated", "stonecutter", "main", "java", "gg", "shard", "client", "mixin")
jar = os.path.expanduser(rf"~\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\{ver}\minecraft-merged-deobf-{ver}.jar")
cfg = json.load(open(os.path.join(root, "versions", ver, "build", "resources", "main", "shard.mixins.json")))
active = set(cfg.get("client", []))
cache = {}
def bodies(cls):
    if cls in cache: return cache[cls]
    r = subprocess.run(["javap", "-p", "-c", "-s", "-cp", jar, cls], capture_output=True, text=True)
    if r.returncode: cache[cls] = None; return None
    res = {}; cur = None
    for l in r.stdout.splitlines():
        m = re.match(r"  \S.*?([\w$<>]+)\((.*)\)( throws .*)?;$", l)
        if m:
            nm = m.group(1); 
            if nm == cls or nm == cls.split('.')[-1]: nm = "<init>"
            cur = res.setdefault(nm, []); cur.append([None, []]); continue
        if l.strip().startswith("descriptor:") and cur is not None and cur[-1][0] is None:
            cur[-1][0] = l.split("descriptor:")[1].strip(); continue
        if cur is not None and "//" in l: cur[-1][1].append(l.split("//",1)[1].strip())
    cache[cls] = res; return res
def balanced(s, i):
    d = 0
    for j in range(i, len(s)):
        if s[j] == '(': d += 1
        elif s[j] == ')':
            d -= 1
            if d == 0: return s[i+1:j]
    return s[i+1:]
problems = 0
for f in sorted(os.listdir(gen)):
    name = f[:-5]
    if name not in active: continue
    code = open(os.path.join(gen, f), encoding="utf-8").read()
    code = re.sub(r"/\*.*?\*/", "", code, flags=re.S); code = re.sub(r"//[^\n]*", "", code)
    imports = dict((m.group(1).split('.')[-1], m.group(1)) for m in re.finditer(r"import ([\w.]+);", code))
    tm = re.search(r"@Mixin\((?:value\s*=\s*)?\{?([\w.]+)\.class", code)
    targets = []
    if tm:
        t = tm.group(1); parts = t.split('.')
        if t[0].islower(): targets.append(t)
        else: targets.append(imports.get(parts[0], parts[0]) + ''.join('$' + p for p in parts[1:]))
    targets += [m.group(1).replace('/', '.') for m in re.finditer(r'targets\s*=\s*\{?\s*"([^"]+)"', code)]
    for m in re.finditer(r"@(Inject|WrapOperation|ModifyArg|ModifyArgs|Redirect|ModifyVariable|ModifyExpressionValue|ModifyReturnValue|WrapWithCondition|ModifyConstant)\(", code):
        ann = balanced(code, m.end() - 1)
        meths = re.findall(r'"([^"]+)"', re.search(r'method\s*=\s*(\{[^}]*\}|"[^"]*")', ann).group(1)) if 'method' in ann else []
        tg = re.search(r'target\s*=\s*"([^"]+)"', ann)
        val = re.search(r'value\s*=\s*"(\w+)"', ann) or re.search(r'@At\("(\w+)"\)', ann)
        val = val.group(1) if val else None
        for meth in meths:
            if meth == '*': continue
            mn = meth.split('(')[0]; md = meth[len(mn):] or None
            found = False; hit = False
            for t in targets:
                b = bodies(t)
                if not b or mn not in b: continue
                for desc, insns in b[mn]:
                    if md and desc != md: continue
                    found = True
                    if not tg: hit = True; continue
                    tgt = tg.group(1)
                    if val == "NEW":
                        own = re.search(r"\)L([^;]+);$", tgt); own = own.group(1) if own else tgt
                        want = f'Method {own}."<init>":{tgt[:tgt.rindex(")")+1]}V' if '(' in tgt else f'class {own}'
                        if any(want in i or (('(' not in tgt) and i == f'class {own}') for i in insns): hit = True
                    else:
                        mm = re.match(r"L([^;]+);([\w$<>]+)(:?)(.*)", tgt)
                        if not mm: hit = True; continue
                        own, n, colon, d = mm.groups()
                        selfown = t.replace('.', '/')
                        def ok(i, kind):
                            mm2 = re.match(kind + r" (?:(\S+)\.)?" + re.escape(n) + ":" + re.escape(d) + "$", i)
                            if not mm2: return False
                            o = mm2.group(1) or selfown
                            return o == own
                        if colon:
                            if any(ok(i, "Field") for i in insns): hit = True
                        else:
                            if any(ok(i, "(?:Interface)?Method") for i in insns): hit = True
            if not found:
                print(f"{name}: method {meth} not found in {targets}"); problems += 1
            elif not hit:
                print(f"{name}: {m.group(1)} {meth} -> target {tg.group(1) if tg else ''} not in body"); problems += 1
print("problems:", problems)
