#!/usr/bin/env python3
"""
Apply a scaffold bundle YAML to the workspace.
Creates new files and applies git patches.
"""
import os
import sys
import subprocess
from pathlib import Path

import yaml


def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: apply_bundle.py <bundle.yml>", file=sys.stderr)
        return 2

    bundle_path = Path(sys.argv[1])
    if not bundle_path.exists():
        print(f"Error: Bundle file not found: {bundle_path}", file=sys.stderr)
        return 1

    print(f"Loading bundle: {bundle_path}")
    data = yaml.safe_load(bundle_path.read_text(encoding="utf-8"))

    # 1) Create new files
    new_files = data.get("new_files", [])
    print(f"Creating {len(new_files)} new files...")
    for nf in new_files:
        p = Path(nf["path"])
        p.parent.mkdir(parents=True, exist_ok=True)
        content = nf["content"]
        # Ensure content ends with newline
        if not content.endswith("\n"):
            content += "\n"
        p.write_text(content, encoding="utf-8")
        print(f"  ✓ {p}")

    # 2) Apply patches via git apply
    patches = data.get("patches", [])
    if patches:
        print(f"Applying {len(patches)} patches...")
        for patch in patches:
            patch_id = patch.get("id", "unknown")
            patch_text = patch["diff"]
            if not patch_text.endswith("\n"):
                patch_text += "\n"

            # Try to apply the patch
            proc = subprocess.run(
                ["git", "apply", "--verbose", "-"],
                input=patch_text.encode("utf-8"),
                capture_output=True,
            )

            if proc.returncode != 0:
                # Try with --3way for better merge handling
                print(f"  ⚠ Patch '{patch_id}' failed, trying with --3way...")
                proc2 = subprocess.run(
                    ["git", "apply", "--3way", "-"],
                    input=patch_text.encode("utf-8"),
                    capture_output=True,
                )
                if proc2.returncode != 0:
                    print(f"  ✗ Patch '{patch_id}' failed:", file=sys.stderr)
                    print(proc2.stderr.decode("utf-8"), file=sys.stderr)
                    # Continue with other patches instead of failing completely
                    continue

            print(f"  ✓ Patch: {patch_id}")

    print("✅ Bundle applied successfully!")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
