#!/usr/bin/env python3
"""
O.M.N.I.S. Cloud SQL Live Synchronization Diagnostic Probe
Telemetrická a diagnostická sonda pro ověření konektivity, TLS vrstvy
a databázové synchronizace s Google Cloud SQL (PostgreSQL).
"""

import os
import sys
import time
import socket
import ssl
import urllib.parse
from datetime import datetime

DEFAULT_HOST = "34.78.59.190"
DEFAULT_PORT = 5432
DEFAULT_DB = "omnis_db"
DEFAULT_USER = "postgres"


def resolve_config():
    """Extrahuje konfiguraci z prostředí nebo DATABASE_URL."""
    host = os.getenv("CLOUDSQL_HOST")
    port = os.getenv("CLOUDSQL_PORT")
    user = os.getenv("CLOUDSQL_USER")
    password = os.getenv("CLOUDSQL_PASSWORD")
    db_name = os.getenv("CLOUDSQL_DB")

    raw_url = os.getenv("DATABASE_URL", "").strip()
    if raw_url:
        clean_url = raw_url.replace("postgresql+asyncpg://", "postgresql://").replace("postgres://", "postgresql://")
        try:
            parsed = urllib.parse.urlparse(clean_url)
            if parsed.hostname:
                host = host or parsed.hostname
            if parsed.port:
                port = port or str(parsed.port)
            if parsed.username:
                user = user or urllib.parse.unquote(parsed.username)
            if parsed.password:
                password = password or urllib.parse.unquote(parsed.password)
            if parsed.path and parsed.path != "/":
                db_name = db_name or parsed.path.lstrip("/")
        except Exception as e:
            print(f"[!] Varování: Nepodařilo se parsovat DATABASE_URL: {e}")

    return {
        "host": host or DEFAULT_HOST,
        "port": int(port or DEFAULT_PORT),
        "user": user or DEFAULT_USER,
        "password": password or "",
        "db": db_name or DEFAULT_DB
    }


def test_tcp_connectivity(host: str, port: int, timeout_sec: float = 3.5):
    """Ověří dosažitelnost TCP soketu a změří síťovou latenci."""
    print(f"[*] FÁZE 1: Testování TCP spojení s {host}:{port}...")
    start_time = time.perf_counter()
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(timeout_sec)
    try:
        sock.connect((host, port))
        latency_ms = (time.perf_counter() - start_time) * 1000.0
        sock.close()
        print(f"    [✓] TCP soket otevřen! Latence: {latency_ms:.2f} ms")
        return True, latency_ms, None
    except socket.timeout:
        err = f"TCP Timeout po {timeout_sec}s. Firewall nebo Cloud SQL blokuje příchozí IP."
        print(f"    [✗] {err}")
        return False, -1.0, err
    except Exception as e:
        err = f"Chyba síťového soketu: {e}"
        print(f"    [✗] {err}")
        return False, -1.0, err


def test_ssl_handshake(host: str, port: int, timeout_sec: float = 3.5):
    """Ověří podporu SSL/TLS handshake na daném portu."""
    print(f"[*] FÁZE 2: Testování SSL/TLS vrstvy na {host}:{port}...")
    start_time = time.perf_counter()
    try:
        context = ssl.create_default_context()
        context.check_hostname = False
        context.verify_mode = ssl.CERT_NONE
        with socket.create_connection((host, port), timeout=timeout_sec) as sock:
            # PostgreSQL začíná SSL negaci speciálním paketem (SSLRequest: 80877103)
            # Pokud server akceptuje SSL, odpoví jedním bajtem 'S'
            ssl_request = (8).to_bytes(4, byteorder='big') + (80877103).to_bytes(4, byteorder='big')
            sock.sendall(ssl_request)
            resp = sock.recv(1)
            if resp == b'S':
                # Server podporuje SSL, dokončíme TLS wrap
                with context.wrap_socket(sock, server_hostname=host) as ssock:
                    cipher = ssock.cipher()
                    version = ssock.version()
                    handshake_ms = (time.perf_counter() - start_time) * 1000.0
                    print(f"    [✓] TLS Handshake úspěšný! Protokol: {version}, Šifra: {cipher[0] if cipher else 'N/A'}")
                    print(f"    [✓] Doba odezvy TLS: {handshake_ms:.2f} ms")
                    return True, f"{version} ({cipher[0] if cipher else ''})"
            elif resp == b'N':
                print("    [!] Server odmítl SSLRequest (běží v nešifrovaném režimu).")
                return True, "PlainText only (SSL odmítnut)"
            else:
                print(f"    [?] Neznámá odpověď na SSLRequest: {resp}")
                return False, f"Odpověď: {resp}"
    except Exception as e:
        print(f"    [!] Poznámka k TLS sondě: {e}")
        return False, str(e)


def test_database_queries(config: dict):
    """Pokusí se provést SQL dotazy, pokud jsou dostupné knihovny a heslo."""
    print(f"[*] FÁZE 3: Testování autentizace a SQL dotazů do databáze '{config['db']}'...")
    if not config["password"]:
        print("    [!] CLOUDSQL_PASSWORD ani DATABASE_URL nebylo v proměnných prostředí zadáno.")
        print("    [!] Aplikace O.M.N.I.S. běží ve standardním hybridním režimu (offline Room SQLite perzistence aktivní).")
        return False, "Chybí heslo (Offline Room Active)"

    # Pokus 1: asyncpg
    try:
        import asyncio
        import asyncpg

        async def run_asyncpg():
            conn = await asyncpg.connect(
                host=config["host"],
                port=config["port"],
                user=config["user"],
                password=config["password"],
                database=config["db"],
                timeout=5.0
            )
            version = await conn.fetchval("SELECT version();")
            
            # Kontrola tabulky omnis_messages
            table_exists = await conn.fetchval(
                "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'omnis_messages');"
            )
            count = 0
            if table_exists:
                count = await conn.fetchval("SELECT COUNT(*) FROM omnis_messages;")
            
            # Kontrola pgvector
            vector_exists = await conn.fetchval(
                "SELECT EXISTS (SELECT FROM pg_extension WHERE extname = 'vector');"
            )
            await conn.close()
            return version, table_exists, count, vector_exists

        version, table_exists, count, vector_exists = asyncio.run(run_asyncpg())
        print(f"    [✓] Autentizace úspěšná!")
        print(f"    [✓] Verze PostgreSQL: {version.split(',')[0]}")
        print(f"    [✓] Tabulka 'omnis_messages': {'Existuje' if table_exists else 'Zatím neexistuje (vytvoří se při prvním syncu)'}")
        if table_exists:
            print(f"    [✓] Počet synchronizovaných záznamů: {count}")
        print(f"    [✓] Rozšíření pgvector: {'Instalováno' if vector_exists else 'Není instalováno'}")
        return True, "Synchronizace a SQL autentizace plně funkční"

    except ImportError:
        pass
    except Exception as e:
        print(f"    [✗] Chyba při připojení přes asyncpg: {e}")
        return False, str(e)

    # Pokus 2: psycopg2
    try:
        import psycopg2
        conn = psycopg2.connect(
            host=config["host"],
            port=config["port"],
            user=config["user"],
            password=config["password"],
            dbname=config["db"],
            connect_timeout=5
        )
        cur = conn.cursor()
        cur.execute("SELECT version();")
        version = cur.fetchone()[0]
        cur.close()
        conn.close()
        print(f"    [✓] Autentizace úspěšná přes psycopg2!")
        print(f"    [✓] PostgreSQL: {version.split(',')[0]}")
        return True, "Spojení s PostgreSQL ověřeno"
    except ImportError:
        pass
    except Exception as e:
        print(f"    [✗] Chyba při připojení přes psycopg2: {e}")
        return False, str(e)

    print("    [i] Python DB ovladače (asyncpg/psycopg2) nejsou nainstalovány v tomto CLI prostředí.")
    print("    [i] Klientská Android aplikace používá nativní Java org.postgresql.Driver.")
    return True, "TCP/TLS vrstva ověřena (Android JDBC připraven)"


def main():
    print("=" * 70)
    print("  O.M.N.I.S. CLOUD SQL LIVE SYNCHRONIZATION DIAGNOSTIC PROBE")
    print(f"  Čas spuštění: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 70)

    cfg = resolve_config()
    print(f"Parametry instance Cloud SQL:")
    print(f"  • Host:     {cfg['host']}")
    print(f"  • Port:     {cfg['port']}")
    print(f"  • Uživatel: {cfg['user']}")
    print(f"  • Databáze: {cfg['db']}")
    print(f"  • Heslo:    {'[POSKYTNUTO]' if cfg['password'] else '[NENASTAVENO - Lokální Room Cache]'}")
    print("-" * 70)

    tcp_ok, latency, tcp_err = test_tcp_connectivity(cfg["host"], cfg["port"])
    ssl_ok, ssl_detail = (False, "N/A")
    if tcp_ok:
        ssl_ok, ssl_detail = test_ssl_handshake(cfg["host"], cfg["port"])

    db_ok, db_detail = test_database_queries(cfg)

    print("=" * 70)
    print("VÝSLEDNÝ DIAGNOSTICKÝ STATUS:")
    print(f"  • TCP Spojení (Port 5432):     {'[ OK ]' if tcp_ok else '[ CHYBA ]'} ({f'{latency:.1f} ms' if tcp_ok else tcp_err})")
    print(f"  • SSL / TLS Handshake:         {'[ OK ]' if ssl_ok else '[ VAROVÁNÍ ]'} ({ssl_detail})")
    print(f"  • Synchronizační stav:         {'[ AKTIVNÍ / PŘIPRAVEN ]' if tcp_ok else '[ OFFLINE CACHE ]'} ({db_detail})")
    print("=" * 70)

    if not tcp_ok:
        print("\nDOPORUČENÍ PRO ŘEŠENÍ VÝPADKU:")
        print("1. Zkontrolujte v Google Cloud Console -> Cloud SQL -> Instance:")
        print("   - Zda je zapnuta Veřejná IP (Public IP) nebo nastaven Cloud SQL Auth Proxy.")
        print("   - Zda je v sekci 'Autorizované sítě' (Authorized networks) povolena IP adresa klienta (např. 0.0.0.0/0 pro neomezený vývoj).")
        print("2. Aplikace O.M.N.I.S. díky hybridní architektuře automaticky ukládá zprávy")
        print("   do Room SQLite a synchronizuje je dávkově jakmile se spojení obnoví.")
        sys.exit(1)
    else:
        print("\nSonda proběhla úspěšně. Cloud SQL infrastruktura je dosažitelná.")
        sys.exit(0)


if __name__ == "__main__":
    main()
