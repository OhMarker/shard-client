# Lists every mixin target method (by name) whose descriptor differs between two versions:
# handler signatures (@Inject arguments, @ModifyReturnValue types) that need a versioned block.
# Usage: python tools/mixin-sigdiff.py <older mc> <newer mc>   (after compiling the newer node)
import os, re, subprocess, sys, json
old, new = sys.argv[1], sys.argv[2]
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
def jar(v):
    p = os.path.expanduser(rf"~\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged-deobf\{v}\minecraft-merged-deobf-{v}.jar")
    if os.path.exists(p): return p
    # 1.21.x: the Mojang-mapped (named) jar Loom remapped for that version.
    import glob
    found = glob.glob(os.path.expanduser(rf"~\.gradle\caches\fabric-loom\minecraftMaven\net\minecraft\minecraft-merged\{v}-loom.mappings*\*.jar"))
    return found[0] if found else p
def descs(v, cls):
    r = subprocess.run(["javap", "-p", "-s", "-cp", jar(v), cls], capture_output=True, text=True)
    if r.returncode: return None
    out = {}; lines = r.stdout.splitlines()
    for i, l in enumerate(lines):
        m = re.search(r"([\w$<>.]+)\((.*)\)", l)
        if m and i + 1 < len(lines) and "descriptor:" in lines[i+1]:
            out.setdefault(m.group(1).split('.')[-1], set()).add(lines[i+1].split("descriptor:")[1].strip())
    return out
gen = os.path.join(root, "versions", new, "build", "generated", "stonecutter", "main", "java", "gg", "shard", "client", "mixin")
for f in sorted(os.listdir(gen)):
    code = open(os.path.join(gen, f), encoding="utf-8").read()
    code = re.sub(r"/\*.*?\*/", "", code, flags=re.S); code = re.sub(r"//[^\n]*", "", code)
    imports = dict((m.group(1).split('.')[-1], m.group(1)) for m in re.finditer(r"import ([\w.]+);", code))
    tm = re.search(r"@Mixin\((?:value\s*=\s*)?\{?([\w.]+)\.class", code)
    targets = []
    if tm:
        t = tm.group(1); p = t.split('.')
        targets.append(t if t[0].islower() else imports.get(p[0], p[0]) + ''.join('$' + x for x in p[1:]))
    targets += [m.group(1) for m in re.finditer(r'targets\s*=\s*"([^"]+)"', code)]
    names = set()
    for m in re.finditer(r'method\s*=\s*(\{[^}]*\}|"[^"]*")', code):
        for n in re.findall(r'"([^"(]+)', m.group(1)): names.add(n)
    for t in targets:
        a, b = descs(old, t), descs(new, t)
        if b is None: continue
        for n in names:
            da, db = (a or {}).get(n), b.get(n)
            if db and da != db: print(f"{f}: {t}.{n}\n   {old}: {da}\n   {new}: {db}")
