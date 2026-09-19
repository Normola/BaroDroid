#!/usr/bin/env python3
"""Renders a QR code for the APK download, so a phone can grab the build directly.

Pure Python: qrcode writes the PNG through pypng, with no native dependencies.
"""
from __future__ import annotations

import argparse

import qrcode
from qrcode.image.pure import PyPNGImage


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("url")
    parser.add_argument("output")
    parser.add_argument("--box-size", type=int, default=10)
    parser.add_argument("--border", type=int, default=4)
    args = parser.parse_args()

    code = qrcode.QRCode(
        # Medium correction survives a bit of screen glare without bloating the
        # grid, which matters because release URLs are long.
        error_correction=qrcode.constants.ERROR_CORRECT_M,
        box_size=args.box_size,
        border=args.border,
    )
    code.add_data(args.url)
    code.make(fit=True)
    code.make_image(image_factory=PyPNGImage).save(args.output)

    modules = code.modules_count + args.border * 2
    print(f"Wrote {args.output}: version {code.version}, {modules}x{modules} modules, for {args.url}")


if __name__ == "__main__":
    main()
