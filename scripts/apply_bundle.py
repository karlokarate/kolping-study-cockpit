#!/usr/bin/env python3
import os, sys, subprocess
from pathlib import Path
import yaml

def main():
    if len(sys.argv) != 2:
        print("Usage: apply_bundle.py <bundle.yml>", file=sys.stderr)
        return 2
    bundle_path = Path(sys.argv[1])
    data = yaml.safe_load(bundle_path.read_text(encoding="utf-8"))

    # 1) create new files
    for nf in data.get("new_files", []):
        p = Path(nf["path"])
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(nf["content"], encoding="utf-8")

    # 2) apply patches via git apply
    patch_text = ""
    for patch in data.get("patches", []):
        patch_text += patch["diff"]
        if not patch_text.endswith("\n"):
            patch_text += "\n"

    if patch_text.strip():
        proc = subprocess.run(["git", "apply", "-"], input=patch_text.encode("utf-8"))
        if proc.returncode != 0:
            print("git apply failed", file=sys.stderr)
            return proc.returncode

    return 0

if __name__ == "__main__":
    raise SystemExit(main())
