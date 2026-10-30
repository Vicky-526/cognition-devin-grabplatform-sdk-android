# Threat model — GrabId Partner SDK for Android

Repository: `Vicky-526/cognition-devin-grabplatform-sdk-android` (drafted from the code). No infrastructure code exists in this repo (only `.gitlab-ci.yml`), and no infrastructure repository was provided: Grab's identity servers (discovery, authorization, token, id-token-verification endpoints) are operated by Grab and are outside this model.

Status: system description drafted from the code; scope confirmed by the user (2026-10-30): all assets matter equally, all three attacker profiles are in scope, nothing is out of scope or accepted risk, no infrastructure repository.

## System overview

An Android library that partner apps embed to sign users in with their Grab account via OAuth 2.0 Authorization Code + PKCE (S256) and OpenID Connect. Flow:
1. `initialize()` reads partner config from the host app's manifest `<meta-data>` (`com.grab.partner.sdk.ClientId`, `RedirectURI`, `Scope`, `ServiceDiscoveryUrl`, …) — `utils/Utility.kt#getPartnerInfo`.
2. `loadLoginSession()` creates `state`/`nonce` (`UUID.randomUUID()`) and a 32-byte `SecureRandom` code verifier — `GrabIdPartner.kt`, `Utility.kt`.
3. `login()` calls the discovery URL, then launches either the native Grab app (`com.grabtaxi.passenger[.beta]`, chosen by `Utility.isPackageInstalled` via `getPackageInfo(pkg, 0)` + min version — no signature check), a Chrome Custom Tab, or the Play Store — `utils/LaunchAppForAuthorization.kt`, `utils/ChromeTabLauncher.kt`.
4. Grab redirects to the partner's redirect URI (samples: `grabweblogin://open`, an exported BROWSABLE activity). `exchangeToken()` checks `state`, then POSTs code + verifier to the token endpoint — `GrabIdPartner.kt`, `api/`.
5. `getIdTokenInfo()` calls the id-token verification endpoint (server-side verification; the SDK checks `nonce`, `exp`, `nbf`, not the JWT signature).
6. Login session and IdTokenInfo are cached in SharedPreferences, encrypted with RSA/ECB/PKCS1 keys in the AndroidKeyStore — `keystore/`, `Utility.saveObjectsToSharedPref`.

## Components

| Component | Code | Notes |
|---|---|---|
| **Auth core** | `GrabIdPartnerSDK/.../sdk/GrabIdPartner.kt`, `GrabIdPartnerProtocol.kt`, `models/`, `repo/`, `utils/` (except launchers) | OAuth/OIDC state machine, PKCE, state/nonce, token/ID-token validation, partner config |
| **Login launcher** | `utils/LaunchAppForAuthorization.kt`, `utils/ChromeTabLauncher.kt`, `utils/IntentProvider.kt`, `wrapper/chrometabmanager/` | Builds the authorize URI; picks Grab app / Custom Tab / Play Store; `ChromeManagerActivity` (not exported) |
| **Redirect handling** | `wrapper/deeplink/DeepLinkActivity.kt`, `wrapper/manager/` | Wrapper flow: receives redirect URI (exported when the host app declares it), maps `state` → session, triggers exchange |
| **Networking** | `api/`, `di/modules/NetworkModule.kt`, `di/modules/TLSSocketFactory.kt`, `di/` | Retrofit/OkHttp; TLS 1.2/1.3, system trust; `HttpLoggingInterceptor` BODY in debug builds |
| **Token storage** | `keystore/AndroidKeyStoreWrapper.kt`, `keystore/CipherWrapper.kt`, SharedPreferences code in `Utility.kt` | RSA keystore keys per alias; encrypted cache of LoginSession / IdTokenInfo |
| **Sample apps** | `sampleapp-kotlin/`, `sampleapp-kotlin-wrapper/`, `sampleapp-java/` | Demo integrations; `allowBackup="true"`, `debug-overrides` trusting user CAs, placeholder config |

## Trust boundaries & entry points

- **Other apps on the device → host app**: the exported redirect activity (`grabweblogin://open` in samples; `DeepLinkActivity` in the wrapper sample) accepts any `VIEW` intent; extras are cleared (`replaceExtras(null)`), data URI is attacker-controlled. Any app can also register the same custom scheme (auth-code interception; PKCE is the mitigation).
- **Grab app package resolution**: the SDK sends the authorize request to whichever installed app has package `com.grabtaxi.passenger[.beta]` at a sufficient version — no signing-certificate check.
- **Network**: discovery, token, id-token verification and user-info calls over HTTPS with the platform trust store; no certificate pinning. Discovery response determines all other endpoints.
- **Host partner app**: trusted — it supplies config and receives tokens via callbacks.
- **Device storage**: SharedPreferences (app-private) holding keystore-encrypted session data.

## Assets (all weighted equally — confirmed by the user)

- Access token, refresh token, ID token (and the cached LoginSession) — highest sensitivity (account access to the partner's Grab-protected resources).
- Integrity of the login flow: authorization code, PKCE verifier, `state`, `nonce` — compromise ⇒ login CSRF / session fixation / code injection.
- User PII in IdTokenInfo / user-info responses.
- Partner config (client_id, redirect URI, scopes) — public, low sensitivity.

## Attacker profiles (all in scope — confirmed by the user)

1. **Malicious app on the same device** (no special permissions): sends crafted intents/deep links, registers the same redirect scheme, or spoofs the Grab package name.
2. **Network attacker** (rogue Wi-Fi, malicious proxy) without a trusted CA on the device.
3. **Attacker with physical / rooted / backup access** to the device.

Not modelled: a malicious host partner app (it is the SDK's customer and already receives the tokens), and compromise of Grab's servers.

## Existing controls

- PKCE S256 with `SecureRandom` verifier; `state` compared in `exchangeToken`; `nonce` compared on ID-token info.
- ID token checked for `exp`/`nbf`; signature verification delegated to Grab's verification endpoint.
- Exported deep-link activity strips all extras on every Android version.
- Logout/cache invalidation deletes only the SDK's own keystore aliases.
- TLS 1.2/1.3 only; release builds log no HTTP bodies.

## Out of scope / accepted risk

None. The user asked for everything to be reported at the scanner's severity, including the sample apps and debug-only behaviour.

## Open questions

- None currently.
