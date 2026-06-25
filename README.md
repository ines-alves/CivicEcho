# CivicEcho

CivicEcho lets citizens submit civic reports to a government municipality through a centralized proxy that routes but never reads them. The problem this solves is precise: the proxy must see enough metadata to route each submission to the correct municipality, but granting the proxy any decryption capability makes it a single point of compromise for all report contents. The design resolves this by encrypting report contents for the destination municipality before the proxy ever sees the payload, binding routing metadata, location, operation type, timestamp, and token, outside the ciphertext as GCM Additional Authenticated Data, and using a dual-token system that lets an authentication server enforce rate limits without exposing which citizen submitted which report to the proxy. All inter-component connections use mTLS 1.3. The database sits behind a separate subnet the proxy cannot address. Two included attack scripts produce concrete evidence that the DoS and MITM defences hold under active testing.

## The hard design problem

The centralized party needs to forward each payload to Lisbon or Porto, it needs to read the destination. If the routing field lives inside an encrypted message, the proxy cannot route without a decryption key. If it holds a decryption key, it can read report contents.

AAD (GCM Additional Authenticated Data) resolves this. When the citizen constructs the protected payload, it separates concerns: the report content is AES-128-GCM encrypted with a fresh per-request session key, and a JSON object containing `{timestamp, location, operation, token}` is attached as AAD outside the ciphertext. The proxy reads the cleartext `location` field to determine the destination municipality. But GCM's authentication tag covers both the ciphertext and the AAD bytes together. Any modification to the routing fields, changing `location`, swapping the token, altering the operation type, causes GCM decryption to fail at the municipality with a tag mismatch, and the municipality rejects the request.

This is preferable to encrypting routing metadata separately under a proxy-held key. A proxy decryption key is a credential that must be distributed, protected, and rotated. AAD requires no key at the proxy: the proxy reads plaintext, the municipality verifies integrity, and the trust model stays clean. The proxy learns the destination and nothing else.

## The read flow

Writing a report is the easy direction: the citizen encrypts for the municipality, the proxy forwards the opaque blob, the municipality decrypts and stores. Reading reports back is harder because the proxy sits on the return path.

If the municipality encrypted the response under the citizen's long-term public key, the proxy could associate that key with a registered citizen identity. If the response were in plaintext, the proxy would see report contents it is not trusted to see.

CivicEcho handles this with a per-request session key. When the citizen constructs a read request, it generates a fresh AES key and includes it inside the encrypted payload, wrapped under the municipality's RSA public key, the same way the content encryption key is wrapped. The municipality decrypts the outer RSA layer, recovers the session key, queries the database, and symmetrically encrypts the response with that session key. The response reaches the proxy as an opaque blob. The proxy forwards it to the citizen, which decrypts it using the session key it generated and still holds locally.

The session key is never transmitted in the clear and is not associated with any persistent identifier visible to the proxy.

## Cryptographic stack

**AES-128-GCM** (report encryption and response encryption)

AES-128-GCM is authenticated encryption: the operation produces a ciphertext and a 128-bit tag that covers both the ciphertext and any AAD. No separate MAC is needed, and the composition errors that arise from combining a cipher and a MAC independently do not apply. The 12-byte IV is drawn from `SecureRandom` per request. The choice of 128-bit AES is consistent with the TLS 1.3 cipher suite used for transport (`TLS_AES_128_GCM_SHA256`), keeping key sizes uniform across layers.

**RSA/PKCS1Padding** (session key wrapping)

Each per-request AES session key is asymmetrically encrypted under the destination municipality's 2048-bit RSA public key before the citizen sends the payload. Only the holder of the municipality's corresponding private key can recover the session key and decrypt the report. RSA is appropriate for key wrapping because the wrapped value (16 bytes) fits within a single RSA block and the operation happens once per request. The implementation uses `RSA/ECB/PKCS1Padding` (PKCS#1 v1.5).
**TLS 1.3 / `TLS_AES_128_GCM_SHA256`** (transport)

All inter-component connections use TLS 1.3 with the cipher suite locked to `TLS_AES_128_GCM_SHA256`. TLS 1.3 eliminates negotiation of weak cipher suites, removes the RSA static key exchange (which lacks forward secrecy), and reduces the handshake to one round trip. Mutual authentication is enforced on all connections except citizen → authentication server during initial enrollment, the bootstrap case where the citizen has no certificate yet. Citizens receive an X.509 certificate signed by the authentication server's CA during enrollment; subsequent connections to the centralized party present that certificate for mTLS.

**SHA-256 with RSA** (token signing)

Tokens are signed by the authentication server's private key using `SHA256withRSA`. The signature covers the token body: a 256-bit random nonce, an expiration timestamp (write tokens) or no expiration (read tokens), and the citizen's bound location. The centralized party verifies the signature against the authentication server's public key before accepting any token. Tampering with the token body, changing the expiry, changing the location, invalidates the signature.

## Token system

The authentication server issues two classes of token at enrollment and at subsequent refresh requests.

**Write tokens** contain a 256-bit random nonce, an expiration timestamp 10 minutes in the future, and the citizen's location, all signed by the authentication server. The centralized party treats write tokens as single-use: it maintains a `ConcurrentHashMap` keyed by token string, checks whether the token is already present before forwarding, and stores it after forwarding. Read tokens are excluded from the replay map because they have no expiry and tracking them would grow the map without bound. Expired-token cleanup runs on each incoming request by scanning and removing entries past their timestamp.

A rate limit governs token batch issuance: a citizen may not request a new batch until 10 minutes have elapsed since the previous issuance. This cooldown is enforced at the authentication server by citizen ID, which is the only component that knows which citizen is requesting. The centralized party enforces single-use, but rate limiting by identity must happen where identity is visible.

**Read tokens** contain a 256-bit random nonce and the citizen's location, with no expiration. They are not entered in the replay tracking map. A read token can be presented multiple times. This is intentional: read access is persistent, and expiring read tokens would require re-enrollment each time a citizen exhausts them.

## Network architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ Authentication  │    │     Citizens     │    │  Centralized    │
│     Server      │◄──►│                  │◄──►│     Party       │
│  192.168.0.20   │    │    192.168.0.X   │    │ 192.168.0.10    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                        │
                       ┌─────────────────┐              │
                       │    Database     │              │
                       │ 192.168.2.100   │              │
                       └─────────────────┘              │
                                △                       │
                                │                       │
                       ┌─────────────────────┐          │
                       │ Municipalities      │◄─────────┘
                       │ Lisbon: 192.168.1.1 │
                       │ Porto:  192.168.1.2 │
                       └─────────────────────┘
```

**192.168.0.x (public subnet)** contains citizens, the authentication server, and the centralized party's citizen-facing interface. A network adversary here can observe encrypted traffic and attempt connection floods. They cannot read session contents, TLS 1.3 ephemeral key exchange provides forward secrecy, so captured sessions cannot be retrospectively decrypted. They cannot impersonate the centralized party without a certificate signed by the authentication server CA. The centralized party caps concurrent connections at 10 per source IP and TLS handshakes at 3/second with a burst of 10 (`hashlimit` rules in `initScripts/centralizedParty.sh`).

**192.168.1.x (internal subnet)** contains the municipality servers and the centralized party's outbound interface (192.168.1.254). Citizens have no route to this subnet. Traffic here is still mTLS: the centralized party presents its certificate to the municipality. The payload forwarded here was encrypted at the citizen; the centralized party cannot decrypt it. Full access to this network segment yields encrypted blobs and the AAD metadata the proxy already read for routing.

**192.168.2.x (database subnet)** contains only the PostgreSQL instance (192.168.2.100). Municipalities have a second interface in this subnet (192.168.2.1 and 192.168.2.2) through which they reach the database. The database's iptables policy is `INPUT DROP` by default; it accepts port 5432 only from 192.168.2.1 and 192.168.2.2, and explicitly drops traffic from 192.168.1.254. The database requires TLS 1.3 with `ECDHE+AESGCM`. The municipality's database service uses `PreparedStatement` throughout, preventing SQL injection via report field content.

## Defences tested

**`dos_attack.py` — DoS defence**

The script runs against the authentication server at `192.168.0.20:8090` in three phases: a baseline of 6 connections at 0.5-second intervals, a burst of 15 simultaneous connections, and a sustained attack of 100 connections at 10/second. Under the iptables rules in the init scripts, the burst and sustained phases produce refused connections once the per-IP cap (10 concurrent) and handshake throttle (3/second, burst 10) engage. The count of successful connections in the 100-connection phase collapses under those rules.

**`mitm_attack.sh` / `mitm.sh` — MITM defence**

The MITM test positions an attacker VM at 192.168.0.50 between citizen1 (192.168.0.100) and the centralized party (192.168.0.10). `mitm.sh` assigns the attacker's IP and enables IP forwarding. `mitm_attack.sh` runs ARP poisoning via `ettercap` and injects 10% packet corruption via `tc netem`. TLS 1.3 connections fail: the attacker cannot produce a valid certificate for the centralized party (the citizen's truststore contains only the authentication server CA), and the packet corruption causes TLS record failures that the client-side implementation detects and rejects. `mitm_monitor.sh` captures the attempt in Wireshark for inspection.

---

## Getting started

**Prerequisites**

- Java 21+
- Maven 3.6+
- PostgreSQL 18+
- OpenSSL
- All VMs run Kali Linux 64-bit

**Network setup**

Configure three host-only networks in VMware:

```
VNET_2_HOSTONLY_SUBNET 192.168.0.0  (public)
VNET_3_HOSTONLY_SUBNET 192.168.1.0  (internal)
VNET_4_HOSTONLY_SUBNET 192.168.2.0  (database)
```

Adapter assignment:

| Machine               | Adapters                      |
|-----------------------|-------------------------------|
| Citizen               | vmnet2, shared                |
| Authentication Server | vmnet2, shared                |
| Centralized Party     | vmnet2, shared, vmnet3        |
| Municipality          | vmnet3, shared, vmnet4        |
| Database              | vmnet4, shared                |

**1. Generate certificates**

```bash
./generate-fixed-certificates.sh
```

Generates: authentication server CA (4096-bit RSA, self-signed, 10-year), component certificates (2048-bit RSA, signed by CA), JKS truststores, and PKCS12 keystores. Output goes into `certificates/`.

**2. Start components (in order)**

```bash
# Database (192.168.2.100)
cd initScripts/ && chmod +x database_setup.sh && sudo ./database_setup.sh

# Authentication Server (192.168.0.20)
chmod +x initScripts/authServer.sh && sudo ./initScripts/authServer.sh

# Centralized Party (192.168.0.10)
chmod +x initScripts/centralizedParty.sh && sudo ./initScripts/centralizedParty.sh

# Municipalities
chmod +x initScripts/municipalityLisbon.sh && sudo ./initScripts/municipalityLisbon.sh
chmod +x initScripts/municipalityPorto.sh && sudo ./initScripts/municipalityPorto.sh

# Citizen
chmod +x initScripts/citizen1.sh && sudo ./initScripts/citizen1.sh
```

**3. Run the demo**

```bash
chmod +x demo.sh && ./demo.sh
```

The demo enrolls citizen1 with the authentication server, submits 10 reports to Lisbon, attempts to request a second token batch before the cooldown expires (rejected), then reads the submitted reports back.

## Usage

```bash
cd citizenApp/ && mvn compile exec:java -Dexec.args="citizen1"
# [1] Login  [2] Create report  [3] Read reports  [4] Request tokens
```

**Audit trail**

All components write to a shared `audit.log` using a `synchronized(LOCK)` writer, recording each transition from plaintext to ciphertext and back:

```
--------------------------------------------------
Time   : 19-12-2025 15:55:42
Actor  : CLIENT
Action : UNPROTECTED REPORT JSON
Data   : {"report_id":"echo_08269","timestamp":"2025-12-19T15:55:23Z",...}
--------------------------------------------------
Time   : 19-12-2025 15:55:42
Actor  : CLIENT
Action : PROTECTED REPORT JSON
Data   : "eyJjaXBoZXJlZCI6IjdH..."
--------------------------------------------------
Time   : 19-12-2025 15:55:42
Actor  : MUNICIPALITY
Action : UNPROTECTED JSON
Data   : {"report_id":"echo_08269",...}
```

**Crypto library CLI**

```bash
cd utils/
./target/appassembler/bin/protect input.json output.json symmetric.key municipality.pubkey token
./target/appassembler/bin/check output.json decrypted.json symmetric.key
./target/appassembler/bin/unprotect output.json decrypted.json municipality.privatekey
```

## License

MIT — see [LICENSE.txt](LICENSE.txt).
