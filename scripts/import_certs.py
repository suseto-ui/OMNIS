#!/usr/bin/env python3
import os
import shutil
import ssl
import subprocess
import sys

def main():
    print("[O.M.N.I.S. Cert Manager] Searching for Cloud SQL certificates...")
    certs_dir = "/certs"
    os.makedirs(certs_dir, exist_ok=True)

    targets = ["server-ca.pem", "client-cert.pem", "client-key.pem"]
    found = {}

    # Check root, certs_dir, and current directory
    search_dirs = [certs_dir, "/", "."]
    for filename in targets:
        for sdir in search_dirs:
            candidate = os.path.join(sdir, filename)
            if os.path.isfile(candidate):
                dst = os.path.join(certs_dir, filename)
                if candidate != dst:
                    shutil.copyfile(candidate, dst)
                found[filename] = dst
                break

    print(f"[O.M.N.I.S. Cert Manager] Found status: { {k: (k in found) for k in targets} }")

    if "client-key.pem" in found:
        pk8_path = os.path.join(certs_dir, "client-key.pk8")
        try:
            cmd = [
                "openssl", "pkcs8", "-topk8", "-inform", "PEM",
                "-outform", "DER", "-in", found["client-key.pem"],
                "-out", pk8_path, "-nocrypt"
            ]
            subprocess.run(cmd, check=True, capture_output=True)
            print(f"[O.M.N.I.S. Cert Manager] Successfully converted client-key.pem -> {pk8_path} (PKCS#8)")
        except Exception as e:
            print(f"[O.M.N.I.S. Cert Manager] Error converting to PKCS#8: {e}")

    # Verify if all 3 certificates are present
    all_ready = all(k in found for k in targets)
    if all_ready:
        print("[O.M.N.I.S. Cert Manager] All 3 certificates installed in /certs!")
        print("[O.M.N.I.S. Cert Manager] Verifying mTLS handshake with Cloud SQL (34.78.59.190:5432)...")
        import socket
        try:
            s = socket.socket()
            s.settimeout(5.0)
            s.connect(('34.78.59.190', 5432))
            s.sendall(int(8).to_bytes(4, 'big') + int(80877103).to_bytes(4, 'big'))
            resp = s.recv(1)
            if resp == b'S':
                ctx = ssl.create_default_context(cafile=found["server-ca.pem"])
                ctx.check_hostname = False
                ctx.verify_mode = ssl.CERT_REQUIRED
                ctx.load_cert_chain(certfile=found["client-cert.pem"], keyfile=found["client-key.pem"])
                tls = ctx.wrap_socket(s, server_hostname='34.78.59.190')
                print("[O.M.N.I.S. Cert Manager] mTLS HANDSHAKE SUCCESSFUL! Cipher:", tls.cipher())
                tls.close()
            else:
                s.close()
                print("[O.M.N.I.S. Cert Manager] Server did not negotiate SSL.")
        except Exception as err:
            print(f"[O.M.N.I.S. Cert Manager] TLS Test error: {err}")
    else:
        missing = [k for k in targets if k not in found]
        print(f"[O.M.N.I.S. Cert Manager] Missing certificates: {missing}")

if __name__ == "__main__":
    main()
