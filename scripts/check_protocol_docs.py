#!/usr/bin/env python3
"""Fails if a response code or status in the protocol file is missing from the guide.

Every key starting CODE_, STATUS_ or PAYMENT_STATUS_ in protocol/gift-intent-protocol.properties
must appear as a backticked token: codes in docs/pages/reference/response-codes.md, statuses in
docs/pages/reference/intent-protocol.md. Extras and limits are covered by the protocol page, which
embeds the whole file. Run from the repository root.
"""
import pathlib
import re
import sys

root = pathlib.Path(__file__).resolve().parent.parent
protocol = root / "protocol" / "gift-intent-protocol.properties"
pages = root / "docs" / "pages" / "reference"


def backticked(path: pathlib.Path) -> set[str]:
    return set(re.findall(r"`([^`\n]+)`", path.read_text(encoding="utf-8")))


entries = {}
for line in protocol.read_text(encoding="utf-8").splitlines():
    if line and not line.startswith("#"):
        key, _, value = line.partition("=")
        entries[key] = value

codes_doc = backticked(pages / "response-codes.md")
statuses_doc = backticked(pages / "intent-protocol.md")
missing = []
for key, value in entries.items():
    if key.startswith("CODE_") and value not in codes_doc:
        missing.append(f"{key}={value} not in response-codes.md")
    elif key.startswith(("STATUS_", "PAYMENT_STATUS_")) and value not in statuses_doc:
        missing.append(f"{key}={value} not in intent-protocol.md")

checked = sum(1 for k in entries if k.startswith(("CODE_", "STATUS_", "PAYMENT_STATUS_")))
if checked < 15:
    missing.append(f"only {checked} codes and statuses found in {protocol.name}")
if missing:
    print("\n".join(missing))
    sys.exit(1)
print(f"{checked} codes and statuses documented")
