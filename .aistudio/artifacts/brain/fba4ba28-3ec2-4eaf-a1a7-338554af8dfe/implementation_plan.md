# Kompletní mapa provázaností systému O.M.N.I.S. & Refaktoring hybridní autentizace

## 1. Analýza architektury a provázaností (Frontend, Middle, Backend, Android)

```
[Klient Android (Kotlin Compose)]
   │
   ├─► CredentialManager / GetSignInWithGoogleOption
   │      │
   │      ├─► (1) Firebase Auth SDK (GoogleAuthProvider.getCredential)
   │      │      └── Získání Firebase ID Tokenu + User objektu
   │      │
   │      ├─► (2) Backend Cloud Run (/api/auth/google)
   │      │      └── Ověření tokenu, uložení do PostgreSQL, vydání JWT
   │      │
   │      └─► (3) Lokální Session (OmnisAuthService / Room DB)
   │             └── Vytvoření AuthSession + nastavení Role (ADMIN_OPERATOR pro susetofu@gmail.com)
   │
[Web Frontend (React / Vite)]
   │
   ├─► GoogleAuthProvider + signInWithPopup(auth, provider)
   │      ├─► Firestore (users/{uid}) -> uložení role a profilu
   │      ├─► Backend API (/api/auth/google) -> získání JWT access_token
   │      └─► localStorage fallback -> omnis_token, omnis_user_role
   │
[Backend FastAPI (Cloud Run / Local)]
   │
   ├─► /api/auth/google: Ověření OAuth tokenu + automatické přiřazení role
   ├─► /api/auth/login, /api/auth/register: Klasické PBKDF2 hesla
   ├─► /api/threads & /api/memory/sync: Obousměrná synchronizace
   └─► PostgreSQL / Cloud SQL: Tabulky users, conversations, messages, omnis_syntheses
```

## 2. Příčina potíží na mobilu (Android CredentialManager)
1. **Chybějící aktivace `OmnisAuthService` relace v `OmnisLoginScreen.kt`:**
   Po úspěšném `signInWithCredential` v Androidu kód volal pouze `onLoginSuccess()`, ale **neaktualizoval** stav `OmnisAuthService.currentUserRole` ani `OmnisAuthService.isAuthenticated`. Proto `OmnisMainScreen` považoval uživatele stále za nepřihlášeného!
2. **Přísná závislost na CredentialManageru bez hybridního fallbacku:**
   Pokud uživatel nemá v telefonu Google Play Services, nebo pokud dialog selže (např. zrušení uživatelem, chybějící SHA-1 otisk pro daný build), aplikace skončila s chybovou hláškou.
3. **Chybějící role mapping v Androidu:**
   Pokud se přihlásil `susetofu@gmail.com`, nebyla role nastavena na `UserRole.ADMIN_OPERATOR`.

## 3. Akční plán implementace hybridního fallbacku

1. **Android (`OmnisLoginScreen.kt`):**
   - Implementovat kompletní zpracování přihlášeného Firebase uživatele:
     - Detekce e-mailu (pokud `susetofu@gmail.com` -> `UserRole.ADMIN_OPERATOR`).
     - Uložení aktivní `AuthSession` do `OmnisAuthService`.
     - Výměna Firebase ID Tokenu s backendem přes `OmnisAuthService.loginWithGoogle(context, idToken)` s bezpečným lokálním fallbackem.
   - Přidat tlačítko **"Rychlý Admin přístup (Nouzový režim / Offline)"**, které umožní okamžité spuštění plné administrátorské konzole v případě potíží se sítí nebo Google Play Services.
2. **Frontend (`AuthScreens.tsx`):**
   - Ověřit a pojistit hybridní přihlášení, aby lokální session byla vytvořena okamžitě i při výpadku spojení s backendem.
3. **Backend (`auth_router.py`):**
   - Ošetřit token exchange i pro testovací / debug buildy.
4. **Verifikace a sestavení:**
   - Zkompilovat Android aplikaci pomocí `compile_applet`.
   - Vytvořit nový debug balíček `Omnis-install.apk`.
   - Provést push na GitHub.
