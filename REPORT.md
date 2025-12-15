# A03 CivicEcho Project Report

## 1. Introduction
`CivicEcho` is a secure citizen reporting system designed to enable anonymous civic participation while maintaining report authenticity and integrity. The system addresses the critical challenge of allowing citizens to submit legitimate reports about civic issues to their municipalities while preserving their anonymity and preventing malicious actors from submitting false reports.

The system operates in a distributed network environment with three main entities: Citizens who generate reports, Municipalities that process and act upon these reports, and a Centralized Party that serves as a trusted intermediary for authentication and anonymization. Citizens connect from the 192.168.0.0 subnet, while municipalities operate from the 192.168.1.0 subnet, with the centralized party facilitating secure communication between them.

The core architecture implements a hybrid cryptographic approach combining symmetric encryption (AES-GCM) for efficient data protection, asymmetric encryption (RSA) for secure key exchange, and digital signatures for authentication. The secure document format encapsulates citizen reports with multiple layers of protection: confidentiality through hybrid encryption, integrity and freshness through GCM authentication tags, and authenticity through a two-phase digital signature process that preserves anonymity.

To facilitate the process we have the following assumptions:
1. The public keys from the municipalities, centralized party, authentication server and database are well known.
2. All private keys are private and are only accessed by the owner, although for simplicity they’re stored in the same directory.
3. The certificates from the municipalities, centralized party, authentication server and database are predistributed and signed by the   authentication server.
4. Valid citizens are already registered at the authentication server.
5. Citizens can only query reports from their municipality.



![Business Scenario](img/businessScenario.png)

## 2. Project Development

### 2.1. Secure Document Format

#### 2.1.1. Design

Our cryptographic library secures citizen-generated reports in JSON format through four primary commands: protect, unprotect, check, ProtectSymmetric and UnprotectSymmetric . 
To ensure confidentiality, integrity, and authenticity, before a report enters the network, the citizen must first protect it. 
We employ a hybrid encryption scheme to achieve this efficiently. Since asymmetric encryption is inefficient for large messages, the process begins with the citizen generating a random unique symmetric key for AES in Galois Counter Mode (GCM), which provides `perfect future protection`. This key encrypts the report itself. The symmetric key is then encrypted using the public key of the intended municipality, ensuring that only the municipality with the corresponding private key can access it, thus guaranteeing `[SR1: confidentiality]`.
The GCM mode inherently provides `[SR2: integrity]`. Any tampering with the ciphertext, even a single bit, results in a completely different authentication tag, which the municipality can detect during decryption, causing the message to be rejected. 
When a citizen wants to read other citizen’s reports, they protect this request in the same way as when they create a new report. The centralized party forwards this request to the citizen’s municipality. Upon receiving the request, the municipality unprotects, stores the session key and queries the database for the number of reports requested. To guarantee that only the citizen can see the contents, the municipality encrypts the response with the session key.

The secure document format uses a JSON structure encoded in Base64:
```
  {
    "ciphered": "base64_encrypted_content",
    "iv": "base64_initialization_vector",
    "aad": {
      "timestamp": "report_timestamp",
      "location": "report_location",
      “operation”: “CREATE/READ”,
      “token”: “token + signature”
    },
    "key": "base64_rsa_encrypted_aes_key"
  }
```

#### 2.1.2. Implementation
The ProtectDocumentLibrary class provides these main operations:
  
- protect(): Encrypts and signs citizen reports with a citizen's private key.
- unprotect(): Decrypts reports and verifies centralized party signature.
- check(): Verifies report integrity. 
- ProtectSymmetric(): Encrypts the read result with the session key
- UnprotectSymmetric(): Decrypts the read result with the session key

Supporting utility classes include RSAKeyGenerator for asymmetric key management and AESKeyGenerator for symmetric key operations.

***
**Hybrid Encryption Scheme:** 

The encryptGCM() method implements the hybrid encryption approach. Reports are encrypted using AES-256 in Galois Counter Mode with a randomly generated 128-bit symmetric key. The symmetric key is then encrypted using RSA with PKCS1 padding using the municipality's public key.
***
**Integrity and Freshness:** 

GCM mode provides built-in integrity verification through a 128-bit authentication tag. The implementation uses Additional Authenticated Data (AAD) containing the report's timestamp, location, operation and token metadata, ensuring these critical fields cannot be modified without detection. This can be verified utilizing the check() method, which uses GCM to decrypt the ciphered message and guarantee that it was not tampered with. 
***
**Libraries used:**

The library was implemented using Java with the standard JCA/JCE cryptographic framework which are well-established, widely tested, and industry-recognized cryptographic libraries.
Key classes come from:
- java.security for public/private keys, SecureRandom, and Signature (digital signatures).
- javax.crypto for Cipher and GCMParameterSpec (AES-GCM authenticated encryption).
- SecretKeySpec for wrapping raw key bytes into a usable AES key.
***

### 2.2. Infrastructure

#### 2.2.1. Network and Machine Setup

(_Provide a brief description of the built infrastructure._)

(_Justify the choice of technologies for each server._)

![Infrastructure Diagram](img/network.png)
#### 2.2.2. Server Communication Security

To provide `authenticity`, the flow of communication ( citizen -> centralized party -> municipality -> database ) is implemented with TLS 1.3 secure sockets with client authentication and cipher suite TLS_AES_128_GCM_SHA256.
To provide `authentication`, when the citizens join the application they must contact the authentication server first. They generate a certificate request that is sent to the authentication server alongside their citizen id and a signature. 
The authentication server has a map of registered citizens and verifies if the request is coming from a valid citizen. If proven valid, the server signs the certificate and sends it back to the citizen so that it can then establish a TLS connection with the centralized party that requires client authentication.

### 2.3. Security Challenge

#### 2.3.1. Challenge Overview

We chose to implement the Security Challenge B, which introduces these new requirements:
- Citizens have a limited number of reports they can create in a given time (tokens)
- Citizens can only request more tokens after a specified cooldown time
- Authenticity of the tokens must be provided
- Tokens must have a limited lifetime

These requirements implied modifications in the authentication process and citizen - centralized party communication.


#### 2.3.2. Attacker Model

(_Define who is fully trusted, partially trusted, or untrusted._)

(_Define how powerful the attacker is, with capabilities and limitations, i.e., what can he do and what he cannot do_)

#### 2.3.3. Solution Design and Implementation
Upon citizen authentication, the authentication server generates 10 write tokens and 1 non-expiring read token that have these corresponding formats:
```
(random_256bitNumber : expiration_timestamp : citizen verified location) - token_signature
```
```
(random_256bitNumber : citizen verified location) - token_signature
```
The server stores the timestamp of the token creation, so that it can control the frequency of token requests. These tokens are sent to the citizen alongside the signed certificate.
In the context of the challenge, the citizen needs to send a different token per create request, to provide `freshness and avoid spam`. 

The centralized party, upon receiving a request, checks if this token is already in the list of used tokens.
It then verifies the authenticity and integrity of the token, by checking if the signature was created by the authentication server, as well as if the location in the report matches the location in the token. 
This way we provide `[SR1: confidentiality] and anonymity`, while maintaining the `[SR3: authentication]` requirement that only verified citizens can submit reports.

For read operation the citizens must send the read token and the number of reports they wish to read from their municipality. This operation is validated by the municipality that verifies the authenticity of the token and the location in the token, meaning the permission to read from that municipality.

Citizens can issue a request to the authentication server for more tokens, if they have all been used or expired. To provide citizen authentication, a signature is sent with this request.
Before generating more tokens, the server verifies if the citizen has waited the specified cooldown time. If so, the server generates 10 new write tokens and sends them to the citizen.

![Token diagram](img/tokens.png)
## 3. Conclusion

(_State the main achievements of your work._)

(_Describe which requirements were satisfied, partially satisfied, or not satisfied; with a brief justification for each one._)

(_Identify possible enhancements in the future._)

(_Offer a concluding statement, emphasizing the value of the project experience._)

## 4. Bibliography

(_Present bibliographic references, with clickable links. Always include at least the authors, title, "where published", and year._)

----
END OF REPORT
