# Threat Model

CivicEcho's threat model reflects the core design tension: the system must provide meaningful anonymity to citizens while preventing token abuse, and the components that route traffic must not be trusted with report contents. This document describes what CivicEcho protects, what each component is trusted to do, and where the design falls short.

## Assets

**Report contents** — the primary data CivicEcho is designed to protect. Each report contains a category, description, coordinates, and location. Confidentiality means no party other than the destination municipality can read a submitted report.

**Citizen identity linkage** — the association between a registered citizen and a specific report. Even partial linkage, knowing that citizen X submitted a report to Lisbon at approximately timestamp T, is a confidentiality breach independent of report contents.

**Token validity** — write tokens and read tokens issued by the authentication server. A forged, replayed, or prematurely refreshed token undermines rate limiting.

**Municipality private keys** — the RSA private keys used to decrypt incoming session keys. Compromise exposes past and future report contents for that municipality. There is no application-layer forward secrecy.

**Authentication server signing key** — used to sign tokens and act as CA for all component certificates. Compromise allows arbitrary token minting and the issuance of certificates trusted by the entire system.

**Audit log integrity and confidentiality** — the shared `audit.log` records plaintext report contents after decryption at the municipality. Integrity matters for accountability; confidentiality matters because the log is a second path to report contents.

## Trust boundaries

**Citizens** are untrusted principals. A citizen is trusted to hold its own private key and to construct syntactically valid requests. A citizen is not trusted to self-limit submission rate, to restrict itself to its registered municipality, or to submit truthful content. The system does not attempt to verify report truthfulness.

**Authentication server** is trusted to authenticate citizens against the hardcoded registration list, to issue correctly signed tokens, to enforce the 10-minute cooldown per citizen ID, and to protect its signing key. It is not trusted with report contents, it issues tokens and certificates but sees no submitted report. It is not trusted to dynamically revoke citizens or update municipality assignments; the registration map is fixed at startup in `EnrollmentHandler`. Denial of service against the authentication server blocks enrollment and token refresh but does not compromise confidentiality of already-submitted reports.

**Centralized party** is trusted to verify token signatures and expiry, to enforce the write-token replay set, to route by location, and to forward the encrypted payload to the municipality unchanged. It is explicitly not trusted with report contents, with the session key embedded in encrypted payloads, or with citizen identity beyond the certificate subject field. A fully compromised centralized party still cannot decrypt report contents or read response payloads: it holds neither the municipality's RSA private key nor the citizen's per-request session key.

**Municipalities** are trusted to hold their RSA private keys, to correctly decrypt and store reports, and to encrypt read responses with the citizen's session key. Each municipality holds an independent key pair; compromise of one does not expose reports stored at the other. Municipalities are trusted not to record which certificate subject submitted which report, the decrypted report contains no citizen identifier.

**Database** is trusted to store and return reports accurately and to enforce TLS 1.3 for connections. Access control is enforced at the network layer: the database machine's iptables accepts port 5432 only from municipality addresses in the database subnet (192.168.2.1, 192.168.2.2) and drops traffic from the centralized party's internal interface (192.168.1.254). The database is not trusted to restrict access by application logic alone.

## Threat actors

**Compromised centralized party** — an adversary with full control of the centralized party process. They can read all network traffic in and out, modify routing logic, inject responses, and dump in-memory state including the replay map and temp files. They cannot decrypt report contents or response payloads because neither decryption key is accessible to the centralized party by design.

**Network adversary on VMnet2** — an adversary who can observe and modify packets between citizens and the centralized party or authentication server. This is the ARP poisoning scenario tested by `mitm_attack.sh`.

**Compromised authentication server** — an adversary with full control of the authentication server. They can issue tokens for any location, enroll arbitrary identities, and deny token issuance to legitimate citizens. The authentication server is the root of trust for the entire PKI; its compromise affects all system components.

**Malicious registered citizen** — a legitimately enrolled citizen attempting token replay, cross-municipality access, rate-limit circumvention, or injection attacks via report content.

## Attack vectors and mitigations

### Compromised centralized party

| Attack | Status |
|--------|--------|
| Read report contents | Fully mitigated — contents encrypted under municipality RSA key before arrival |
| Read response payload sent back to citizen | Fully mitigated — response encrypted with citizen's session key, which is inside the RSA-encrypted layer |
| Modify routing AAD to redirect to wrong municipality | Fully mitigated — GCM tag covers AAD; modification detected at municipality, request rejected |
| Replay a write token it has already seen | Fully mitigated — token is in replay set; `verifyUsedToken` rejects it |
| Modify the encrypted payload before forwarding | Fully mitigated — GCM authentication tag fails at municipality |
| Inject false error responses to citizen | Not mitigated — citizen has no out-of-band channel to verify response authenticity from a potentially compromised proxy |
| Selectively drop or delay requests | Not mitigated — no cryptographic defence against selective dropping |

### Network adversary on VMnet2

| Attack | Status |
|--------|--------|
| Passive session capture | Fully mitigated — TLS 1.3 ephemeral key exchange provides forward secrecy |
| TLS interception | Fully mitigated — client verifies server certificate against auth CA; adversary cannot produce a valid cert |
| Token capture and replay from intercepted traffic | Fully mitigated against passive adversary — token is inside the TLS session |
| ARP poisoning with packet corruption | Fully mitigated — TLS record layer detects corrupted records; GCM rejects modified application payloads |
| Connection flood from a single source IP | Mostly mitigated — iptables caps 10 concurrent connections and 3 handshakes/second per IP |
| Distributed flood from many source IPs | Not mitigated — per-IP limits do not address distributed origin |

### Compromised authentication server

| Attack | Status |
|--------|--------|
| Issue tokens for arbitrary locations | Not mitigated — centralized party trusts the auth server signature unconditionally |
| Mint tokens bypassing the cooldown | Not mitigated — cooldown is enforced only locally at the auth server |
| Issue certificates for arbitrary identities | Not mitigated — the auth server IS the CA; compromise breaks the entire PKI |
| Read report contents | Fully mitigated — auth server holds no municipality private key |
| Read reports submitted in past sessions | Fully mitigated — no retrospective key access |

### Malicious registered citizen

| Attack | Status |
|--------|--------|
| Replay a write token | Fully mitigated — centralized party replay set rejects previously seen tokens |
| Request tokens before cooldown expires | Fully mitigated — auth server enforces 10-minute cooldown by citizen ID |
| Read from a different municipality | Fully mitigated — read token encodes location; centralized party verifies match against AAD |
| Submit a report to a different municipality | Fully mitigated — write token encodes location; centralized party verifies; GCM tag also covers location in AAD |
| SQL injection via report fields | Fully mitigated — municipality uses `PreparedStatement` throughout |

## Known limitations

**Replay set is in-memory only.** The centralized party's token blacklist is not persisted to disk. A restart clears it. Any write token still within its 10-minute validity window at the time of a restart can be reused after it.

**Traffic analysis is not addressed.** An observer who records connection timing and volume without reading TLS session content can potentially correlate the time of a citizen's submission with a report appearing in a municipality. TLS and application-layer encryption protect content, not metadata.

**No application-layer forward secrecy.** TLS 1.3 provides forward secrecy at the transport layer. At the application layer, the municipality's RSA private key decrypts every incoming session key. If that key is compromised, all historical payloads captured from the network can be decrypted retroactively. There is no mechanism to rotate the municipality's encryption key or to re-encrypt stored reports under a new key.

**Citizen registration is static.** The enrolled citizens (`citizen1 → Lisbon`, `citizen2 → Porto`) are hardcoded in `EnrollmentHandler`. Adding citizens, revoking them, or changing a municipality assignment requires a server restart. There is no runtime administration interface.

**Token cooldown resets on auth server restart.** The authentication server tracks last-issuance time per citizen in an in-memory map. A restart clears all cooldown records. A citizen who observes a restart can immediately request a new token batch regardless of when the previous batch was issued.

**Hardcoded deployment credentials.** All keystores use the password `changeme` (set in `javax.net.ssl.keyStorePassword` system properties and used by `generate-fixed-certificates.sh`). The database uses `municipality_user`/`municipality_pass`. These are not rotated by any included tooling.

## Out of scope

- Physical security of VMs and host systems
- Endpoint compromise of a citizen machine (local private key theft, malware that intercepts plaintext before encryption)
- Denial-of-service attacks from multiple distributed source IPs
- Social engineering of the authentication server administrator
- Report content truthfulness verification
