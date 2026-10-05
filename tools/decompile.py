#!/usr/bin/env python3
"""
One-shot: decompile the original mod jars into Gradle subprojects.

- Decompiles with Vineflower.
- Renames Minecraft SRG names (m_12345_ / f_12345_) to Mojang official names
  (MCPConfig joined.tsrg + Mojang client mappings for 1.20.1), so the sources match
  the `official` mappings used by ForgeGradle.
- Copies resources (assets, data, mods.toml, mixin configs) from the jars.
Refuses to overwrite existing sources unless FORCE=1.
"""
import io, json, os, pathlib, re, shutil, subprocess, sys, urllib.request, zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
TMP = ROOT / "build-tmp"
VINEFLOWER = "https://repo1.maven.org/maven2/org/vineflower/vineflower/1.10.1/vineflower-1.10.1.jar"
MC_VERSION = "1.20.1"
MODS = [
    ("original-jars/viltrumiteflight-forge-1.6.7.jar", "viltrumiteflight"),
    ("original-jars/viltrumitecore-forge-1.10.3.jar", "viltrumitecore"),
]
SRG_RE = re.compile(r"\b([mf]_\d+_)\b")
PRIM = {"void": "V", "boolean": "Z", "byte": "B", "char": "C", "short": "S",
        "int": "I", "long": "J", "float": "F", "double": "D"}


def fetch(url):
    req = urllib.request.Request(url, headers={"User-Agent": "viltrumite-decompile"})
    with urllib.request.urlopen(req, timeout=180) as r:
        return r.read()


def load_mojang():
    manifest = json.loads(fetch("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
    ver = next(v for v in manifest["versions"] if v["id"] == MC_VERSION)
    meta = json.loads(fetch(ver["url"]))
    text = fetch(meta["downloads"]["client_mappings"]["url"]).decode("utf-8")
    classes, off2obf, cur = {}, {}, None
    for line in text.splitlines():
        if not line or line.startswith("#"):
            continue
        if not line.startswith(" "):
            off, obf = line.strip().rstrip(":").split(" -> ")
            cur = {"fields": {}, "methods": []}
            classes[obf] = cur
            off2obf[off] = obf
            continue
        left, obf = line.strip().split(" -> ")
        if "(" in left:
            m = re.match(r"(?:\d+:\d+:)?(\S+) ([^\s(]+)\(([^)]*)\)", left)
            if m:
                cur["methods"].append((m.group(2), m.group(1), m.group(3), obf))
        else:
            _typ, name = left.split(" ")
            cur["fields"][obf] = name
    return classes, off2obf


def to_desc(t, off2obf):
    dims = 0
    while t.endswith("[]"):
        t, dims = t[:-2], dims + 1
    d = PRIM.get(t) or ("L" + off2obf.get(t, t).replace(".", "/") + ";")
    return "[" * dims + d


def load_tsrg():
    meta = fetch("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/maven-metadata.xml").decode()
    vers = sorted(v for v in re.findall(r"<version>([^<]+)</version>", meta) if v.startswith(MC_VERSION + "-"))
    v = vers[-1]
    print("MCPConfig", v)
    z = zipfile.ZipFile(io.BytesIO(fetch(
        f"https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/{v}/mcp_config-{v}.zip")))
    return z.read("config/joined.tsrg").decode("utf-8")


def build_srg_map():
    classes, off2obf = load_mojang()
    srg2off, cur = {}, None
    for line in load_tsrg().splitlines():
        if not line.strip() or line.startswith("tsrg2") or line.startswith("\t\t"):
            continue
        tok = line.strip().split()
        if not line.startswith("\t"):
            cur = classes.get(tok[0].replace("/", "."))
            if cur is not None and "mdesc" not in cur:
                cur["mdesc"] = {}
                for off, ret, args, obf in cur["methods"]:
                    a = "".join(to_desc(x.strip(), off2obf) for x in args.split(",") if x.strip())
                    cur["mdesc"][(obf, "(" + a + ")" + to_desc(ret, off2obf))] = off
            continue
        if cur is None:
            continue
        if len(tok) >= 3 and tok[1].startswith("("):
            srg, off = tok[2], cur["mdesc"].get((tok[0], tok[1]))
        else:
            srg = tok[-2] if len(tok) >= 3 else tok[1]
            off = cur["fields"].get(tok[0])
        if off and re.fullmatch(r"[mf]_\d+_", srg):
            srg2off.setdefault(srg, off)
    print("SRG -> official names:", len(srg2off))
    return srg2off


def decompile(jar, name):
    vf = TMP / "vineflower.jar"
    if not vf.exists():
        vf.write_bytes(fetch(VINEFLOWER))
    raw = TMP / ("raw-" + name)
    shutil.rmtree(raw, ignore_errors=True)
    raw.mkdir(parents=True)
    subprocess.run(["java", "-jar", str(vf), "-dgs=1", "-asc=1", "-rsy=1", "-udv=1", "-ump=1",
                    "-log=WARN", str(jar), str(raw)], check=True)
    for p in list(raw.rglob("*")):  # some Vineflower versions write a sources jar
        if p.is_file() and p.suffix in (".jar", ".zip"):
            with zipfile.ZipFile(p) as z:
                z.extractall(raw)
            p.unlink()
    return raw


def assemble(jar_rel, modid, srg2off):
    jar = ROOT / jar_rel
    proj = ROOT / modid
    java_dir, res_dir = proj / "src/main/java", proj / "src/main/resources"
    shutil.rmtree(java_dir, ignore_errors=True)
    shutil.rmtree(res_dir, ignore_errors=True)
    raw = decompile(jar, modid)
    unmapped, broken, n = set(), [], 0

    def sub(m):
        s = m.group(1)
        if s in srg2off:
            return srg2off[s]
        unmapped.add(s)
        return s

    for src in sorted(raw.rglob("*.java")):
        rel = src.relative_to(raw)
        text = SRG_RE.sub(sub, src.read_text(encoding="utf-8"))
        if "Couldn't be decompiled" in text:
            broken.append(str(rel))
        dst = java_dir / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        dst.write_text(text, encoding="utf-8")
        n += 1

    with zipfile.ZipFile(jar) as z:
        for e in z.namelist():
            if e.endswith("/") or e.endswith(".class") or e == "META-INF/MANIFEST.MF" or e.endswith("refmap.json"):
                continue
            data = z.read(e)
            if e == "META-INF/mods.toml":
                data = re.sub(rb'(?m)^version\s*=\s*"[^"]*"', b'version = "@MOD_VERSION@"', data, count=1)
            dst = res_dir / e
            dst.parent.mkdir(parents=True, exist_ok=True)
            dst.write_bytes(data)

    lines = [f"## {modid}", "", f"- Java files: {n}",
             f"- Unmapped SRG names: {len(unmapped)}" + (f" (e.g. {', '.join(sorted(unmapped)[:15])})" if unmapped else ""),
             f"- Methods the decompiler couldn't fully recover: {len(broken)}"]
    lines += [f"  - `{b}`" for b in broken]
    return "\n".join(lines) + "\n"


def main():
    force = os.environ.get("FORCE") == "1"
    existing = [m for _, m in MODS if (ROOT / m / "src/main/java").exists()]
    if existing and not force:
        sys.exit(f"Sources already exist in {existing}; refusing to overwrite (set FORCE=1).")
    TMP.mkdir(exist_ok=True)
    srg2off = build_srg_map()
    report = ["# Decompile report", "",
              "Sources were recovered from `original-jars/` with Vineflower and remapped to Mojang official names.", ""]
    for jar_rel, modid in MODS:
        report.append(assemble(jar_rel, modid, srg2off))
    (ROOT / "DECOMPILE_REPORT.md").write_text("\n".join(report), encoding="utf-8")
    print("\n".join(report))


if __name__ == "__main__":
    main()