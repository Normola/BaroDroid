#!/usr/bin/env python3
"""Writes the release notes: what changed, and how to get the APK onto a phone."""
from __future__ import annotations

import argparse
import hashlib
import subprocess
from pathlib import Path


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], check=True, capture_output=True, text=True
    ).stdout.strip()


def previous_tag(tag: str) -> str | None:
    """The release before this one, or None when this is the first."""
    for start in (f"{tag}^", "HEAD^"):
        try:
            return git("describe", "--tags", "--abbrev=0", "--match", "v*", start)
        except subprocess.CalledProcessError:
            continue
    return None


def commits(since: str | None) -> list[tuple[str, str]]:
    revision_range = f"{since}..HEAD" if since else "HEAD"
    log = git("log", "--no-merges", "--pretty=%h\x1f%s", revision_range)
    entries = []
    for line in log.splitlines():
        if "\x1f" not in line:
            continue
        sha, subject = line.split("\x1f", 1)
        entries.append((sha, subject))
    return entries


def human_size(size: int) -> str:
    return f"{size / (1024 * 1024):.1f} MB"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def build(args: argparse.Namespace) -> str:
    apk = Path(args.apk)
    since = args.previous_tag or previous_tag(args.tag)
    changes = commits(since)

    lines: list[str] = []
    lines.append(f"## BaroDroid {args.version_name}")
    lines.append("")
    lines.append("Scan the code with the phone you want it on, or use the link underneath.")
    lines.append("")
    lines.append(
        f'<img src="{args.qr_url}" alt="QR code linking to {apk.name}" width="220" height="220">'
    )
    lines.append("")
    lines.append(f"[**{apk.name}**]({args.download_url}) · {human_size(apk.stat().st_size)} · Android 8.0 (API 26) and later")
    lines.append("")

    if args.signing != "release":
        lines.append(
            "> **Debug-signed build.** No release keystore is configured for this "
            "repository, so the APK is signed with the standard debug key: Android "
            "will call it an unknown-source install, and it cannot be installed over "
            "a copy signed with a different key. Add the signing secrets described in "
            "the README to publish properly signed builds."
        )
        lines.append("")

    lines.append("### What changed")
    lines.append("")
    if changes:
        for sha, subject in changes:
            lines.append(f"- {subject} (`{sha}`)")
    else:
        lines.append("- No commits since the previous release.")
    lines.append("")

    lines.append("### Verify the download")
    lines.append("")
    lines.append("```")
    lines.append(f"sha256  {sha256(apk)}")
    lines.append("```")
    lines.append("")

    if since:
        lines.append(
            f"**Full changelog**: https://github.com/{args.repo}/compare/{since}...{args.tag}"
        )
    else:
        lines.append(f"**Commits**: https://github.com/{args.repo}/commits/{args.tag}")
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", required=True)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--apk", required=True)
    parser.add_argument("--download-url", required=True)
    parser.add_argument("--qr-url", required=True)
    parser.add_argument("--signing", default="debug", choices=["debug", "release"])
    parser.add_argument("--previous-tag", default=None)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    Path(args.output).write_text(build(args), encoding="utf-8")
    print(f"Wrote {args.output}")


if __name__ == "__main__":
    main()
