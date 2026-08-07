#!/usr/bin/env python3
"""Apply the ColorOS volume-curve category compatibility patch.

The input must be the unmodified ColorOS 401 mini
libaudiopolicyenginedefault.so. Only two bytes in the AArch64 CMP
instruction are changed; every other byte is preserved.
"""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path


INPUT_SHA256 = "84abd687407f0eb2941105b97e5e3f129f764d0b316e852e5b295493493c261d"
OUTPUT_SHA256 = "84a6982cb242eb561543f6f2a43d5ccddff42c24eea446ab9d18c85717213d2e"
PATCH_OFFSET = 0x484BD
BEFORE = bytes.fromhex("1900")
AFTER = bytes.fromhex("fd03")


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    data = bytearray(args.input.read_bytes())
    actual = sha256(data)
    if actual != INPUT_SHA256:
        raise SystemExit(
            f"Refusing unknown input: {actual}\nExpected: {INPUT_SHA256}"
        )
    if data[PATCH_OFFSET : PATCH_OFFSET + len(BEFORE)] != BEFORE:
        raise SystemExit("Expected instruction bytes were not found")

    data[PATCH_OFFSET : PATCH_OFFSET + len(AFTER)] = AFTER
    patched = bytes(data)
    actual_output = sha256(patched)
    if actual_output != OUTPUT_SHA256:
        raise SystemExit(
            f"Patched output mismatch: {actual_output}\nExpected: {OUTPUT_SHA256}"
        )

    args.output.write_bytes(patched)
    print(f"Patched {args.output}")
    print(f"SHA-256 {actual_output}")


if __name__ == "__main__":
    main()
