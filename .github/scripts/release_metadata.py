#!/usr/bin/env python3
"""Works out what a release is called, from the git ref that triggered it.

Prints KEY=VALUE lines suitable for $GITHUB_OUTPUT. A tag push (v1.2.3) makes a
real release; anything else is a dry run, named after the commit.
"""
from __future__ import annotations

import argparse
import re
import sys

TAG_PATTERN = re.compile(r"^v(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?$")


def parse(ref: str, sha: str, run_number: int) -> dict[str, str]:
    if ref.startswith("refs/tags/"):
        tag = ref[len("refs/tags/"):]
        match = TAG_PATTERN.match(tag)
        if not match:
            sys.exit(
                f"Tag {tag!r} is not a release tag. Use vMAJOR.MINOR.PATCH, "
                "optionally with a pre-release suffix such as v1.2.0-beta.1."
            )
        major, minor, patch, prerelease = match.groups()
        version_name = tag[1:]
        # Room for 100 patches and 100 minors per major, which is plenty and
        # keeps the number readable: 1.2.3 -> 10203.
        version_code = int(major) * 10000 + int(minor) * 100 + int(patch)
        is_release = True
    else:
        tag = f"dev-{sha[:7]}"
        version_name = f"0.0.0-dev.{sha[:7]}"
        version_code = max(run_number, 1)
        prerelease = "dev"
        is_release = False

    apk_name = f"BaroDroid-{version_name}.apk"
    return {
        "tag": tag,
        "version_name": version_name,
        "version_code": str(version_code),
        "apk_name": apk_name,
        "qr_name": f"BaroDroid-{version_name}-qr.png",
        "prerelease": "true" if prerelease else "false",
        "is_release": "true" if is_release else "false",
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ref", required=True, help="e.g. refs/tags/v1.2.3")
    parser.add_argument("--sha", required=True)
    parser.add_argument("--run-number", type=int, default=1)
    parser.add_argument("--repo", required=True, help="owner/repo")
    args = parser.parse_args()

    values = parse(args.ref, args.sha, args.run_number)
    base = f"https://github.com/{args.repo}/releases/download/{values['tag']}"
    values["download_url"] = f"{base}/{values['apk_name']}"
    values["qr_url"] = f"{base}/{values['qr_name']}"

    for key, value in values.items():
        print(f"{key}={value}")


if __name__ == "__main__":
    main()
