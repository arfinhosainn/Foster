#!/usr/bin/env python3
"""
Builds the PUBLIC versions of privacy.html / terms.html from the source
Markdown (../PRIVACY_POLICY.md, ../TERMS_AND_CONDITIONS.md).

Run after any edit to the Markdown sources:

    cd website && python3 make_public.py

(expects `pandoc` on PATH). The script strips internal-only material that must
not appear on the public site:
  - the "draft / not legal advice / audit appendix" closing notes
  - references to internal repo files (STORE_DISCLOSURES.md, COMPLIANCE_TODO)
"""
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).parent

INTERNAL_PATTERNS = [
    # The closing draft/lawyer notes (whole <p>...</p> block)
    r"<p><strong>Note:</strong>[^<]*draft[^<]*legal advice.*?</p>\s*",
]

FILE_REFS = [
    (r"\s*The audit appendix backing every claim above is in "
     r"<code>STORE_DISCLOSURES\.md</code> \(Appendix A\)\.", ""),
    (r"via backup rules \((?:<code>[^<]+</code>, )?<code>[^<]+</code>\)",
     "via the app's backup settings"),
]


def publicize(html: str) -> str:
    for pattern in INTERNAL_PATTERNS:
        html = re.sub(pattern, "", html, flags=re.DOTALL)
    for pattern, repl in FILE_REFS:
        html = re.sub(pattern, repl, html, flags=re.DOTALL)
    # Trailing whitespace left by removed blocks
    html = re.sub(r"\n{3,}", "\n\n", html)
    return html


def build(md_name: str, html_name: str, title: str) -> None:
    md = HERE.parent / md_name
    out = HERE / html_name
    subprocess.run(
        ["pandoc", "-s", "-f", "gfm",
         "--include-in-header", str(HERE / "header.html"),
         "--metadata", f"title={title}",
         str(md), "-o", str(out)],
        check=True,
    )
    out.write_text(publicize(out.read_text()))
    leftovers = [ln for ln in out.read_text().splitlines()
                 if "legal advice" in ln or "STORE_DISCLOSURES" in ln
                 or "COMPLIANCE_TODO" in ln or "NEEDS VERIFICATION" in ln]
    status = "CLEAN" if not leftovers else f"WARNING: {leftovers}"
    print(f"{html_name}: built, {status}")


if __name__ == "__main__":
    try:
        build("PRIVACY_POLICY.md", "privacy.html", "Foster — Privacy Policy")
        build("TERMS_AND_CONDITIONS.md", "terms.html", "Foster — Terms & Conditions")
    except FileNotFoundError:
        sys.exit("pandoc not found — install it (brew install pandoc)")
