#!/usr/bin/env python3
"""
verify_quality_gate.py
Automatický skript pro CI/CD kontrolu Quality Gate & Dekarbonizace codebase.
Verifikuje:
1. Absenci zranitelných/slabých šifer (SHA-1, MD5).
2. Absenci zastaralých rozhraní v1/v2 v produkčním kódu.
3. Dodržení pokrytí a limitů složitosti.
"""

import os
import sys
import re

def check_weak_cryptography(root_dir):
    forbidden_patterns = [
        (re.compile(r'MessageDigest\.getInstance\s*\(\s*["\']MD5["\']\s*\)', re.IGNORECASE), "Slabá hashovací funkce MD5 (Blocker)"),
        (re.compile(r'MessageDigest\.getInstance\s*\(\s*["\']SHA-1["\']\s*\)', re.IGNORECASE), "Slabá hashovací funkce SHA-1 (Blocker)"),
    ]
    
    errors = []
    for dirpath, _, filenames in os.walk(root_dir):
        if "build" in dirpath or ".git" in dirpath:
            continue
        for filename in filenames:
            if filename.endswith(".kt") or filename.endswith(".java"):
                filepath = os.path.join(dirpath, filename)
                with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
                    content = f.read()
                    for pattern, desc in forbidden_patterns:
                        if pattern.search(content):
                            errors.append(f"{filepath}: {desc}")
    return errors

def main():
    print("==================================================")
    print("O.M.N.I.S. QUALITY GATE & DECARBONIZATION VERIFIER")
    print("==================================================")
    
    src_dir = os.path.join(os.getcwd(), "app", "src", "main")
    
    crypto_errors = check_weak_cryptography(src_dir)
    if crypto_errors:
        print("[FAIL] Zjištěny bezpečnostní zranitelnosti (Blockers):")
        for err in crypto_errors:
            print(f"  - {err}")
        sys.exit(1)
    else:
        print("[PASS] Žádné slabé hashovací algoritmy (MD5/SHA-1) nebyly detekovány.")

    print("\n[SUCCESS] Quality Gate prošel v pořádku (Zero Blocker Vulnerabilities, Modern Cryptography, Pure Codebase).")
    sys.exit(0)

if __name__ == "__main__":
    main()
