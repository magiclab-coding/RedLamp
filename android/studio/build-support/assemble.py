import sys
from pathlib import Path
from zipfile import ZipFile


base, rebuilt, dex, html, output = map(Path, sys.argv[1:])
with ZipFile(rebuilt) as rebuilt_apk:
    manifest = rebuilt_apk.read("AndroidManifest.xml")

replacements = {
    "AndroidManifest.xml": manifest,
    "assets/studio.html": html.read_bytes(),
}

if dex != Path("-"):
    replacements["classes3.dex"] = dex.read_bytes()

with ZipFile(base) as base_apk, ZipFile(output, "w") as output_apk:
    for member in base_apk.infolist():
        name = member.filename
        if name.startswith("META-INF/") and name.endswith(
            ("/MANIFEST.MF", ".SF", ".RSA", ".DSA", ".EC")
        ):
            continue
        data = replacements[name] if name in replacements else base_apk.read(member)
        output_apk.writestr(member, data)
