# SECURITY.md

This document defines baseline security rules for this repository.

## Security Rules

- Never commit secrets, tokens, private keys, certificates, keystores, or production credentials.
- Release signing config comes from CI secrets / a local untracked file, never from the repo.
- Validate input at trust boundaries: URLs shared into the app (`WebCastActivity`, `MediaActionActivity`),
  HTTP requests to the phone's local servers, data coming back from TVs.
- Do not log pairing codes, Samsung tokens (`TOKEN_SS_REMOTE`), Android TV client certs, or purchase tokens.
- Keep dependencies updated and remove unused packages.
- No ads or analytics SDKs without an ADR (enforced by `scripts/ci/verify_architecture.sh`).

## Authentication and Authorization

There are no user accounts. Trust is per-device on the LAN:

- Samsung: the TV shows an allow prompt; the returned token is stored in prefs and reused on port 8002.
- Android TV: TLS pairing with a 6-digit code shown on the TV; the client certificate is generated on the phone.
- Sony: PIN registration through `/sony/accessControl`.
- Fire TV: the user enables ADB debugging and accepts the phone's ADB key on the TV.
- Roku, DLNA, Cast: no auth (protocol design).
- Premium: Play Billing is the only source of truth; the cached `is_premium` flag is a convenience, refreshed on
  every billing connection. Purchases are acknowledged client-side (no server validation).

## Known Exposure (accepted for parity, tracked in tech debt)

- The phone runs HTTP servers on the LAN (8080, 8093, 8686/8268) with no auth: anyone on the same Wi-Fi can read
  the mirror stream or served files while they run. Keep them stopped when not casting.
- Cleartext traffic is allowed globally (`network_security_config.xml`) because TVs only speak HTTP.
- `WebCastActivity` lets the user proceed past SSL errors after a warning dialog.
- `debug` builds export every Activity — never ship a debug build.

## Security Review Checklist

- [ ] No secrets committed
- [ ] Input validation added where needed (shared intents, local servers)
- [ ] New local server endpoints reviewed for what they expose on the LAN
- [ ] Sensitive logs avoided
- [ ] Error messages do not leak internals
- [ ] Dependency changes reviewed
