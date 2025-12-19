# CivicEcho - Secure Citizen Reporting System

## Team

| Number  | Name              | User                                   | E-mail                                                 |
| --------|-------------------|----------------------------------------| -------------------------------------------------------|
| 1104147 | Diogo Rodrigues   | <https://github.com/diogotrodrigues>   | <mailto:diogo.teixeira.rodrigues@tecnico.ulisboa.pt>   |
| 1106378 | Luca Dallalana    | <https://github.com/luca-dallalana>     | <mailto:luca.dallalana@tecnico.ulisboa.pt>            |
| 1107157 | Inês Alves        | <https://github.com/ines-alves>        | <mailto:ines.b.alves@tecnico.ulisboa.pt>               |


<div>
  <img src="img/ines.JPG" alt="Inês" style="width:150px; height:150px; object-fit:contain;">
  <img src="img/diogo.jpeg" alt="Diogo" style="width:150px; height:150px; object-fit:contain;">
  <img src="img/luca.png" alt="Luca" style="width:150px; height:150px; object-fit:contain;">
</div>

## Contents

This repository contains documentation and source code for the *Network and Computer Security (SIRS)* project.

The [REPORT](REPORT.md) document provides a detailed overview of the key technical decisions and various components of the implemented project.
It offers insights into the rationale behind these choices, the project's architecture, and the impact of these decisions on the overall functionality and performance of the system.

This document presents installation and demonstration instructions.

## Project Overview

**CivicEcho** is a secure citizen reporting system designed to enable anonymous civic participation while maintaining report authenticity and integrity. The system allows citizens to submit legitimate reports about civic issues to their municipalities while preserving their anonymity and preventing malicious actors from submitting false reports.

### Key Features

- **Anonymous Reporting**: Reports can't be traced back to their author
- **Authenticated Access**: Token and certificate based authentication ensures only legitimate citizens can create reports
- **Document Security**: Hybrid encryption (AES-GCM + RSA) protects data confidentiality and integrity
- **Multi-Municipality Support**: System supports multiple municipalities and it's flexible (currently Lisbon and Porto)
- **Replay Protection**: Prevents token reuse and replay attacks
- **Secure Communication**: TLS 1.3 with mutual authentication between all components, except in authentication server
- **Audit File**: Logging mechanism that allows to trace back the complete information flow through the system

## System Architecture

The system operates in a distributed network environment with five main components:

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ Authentication  │    │     Citizens     │    │  Centralized    │
│     Server      │◄──►│                  │◄──►│     Party       │
│  192.168.0.20   │    │    192.168.0.X   |    │ 192.168.0.10    │
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
                       │ Lisbon: 192.168.1.1 |
                       │ Porto:  192.168.1.2 |
                       └─────────────────────┘
```

### Components

1. **CitizenApp**: Client application for report creation/reading
2. **AuthenticationServer**: Issues tokens and manages citizen enrollment
3. **CentralizedParty**: Trusted intermediary for anonymization and routing
4. **MunicipalityApp**: Processes reports and verifies integrity
5. **Database**: PostgreSQL database for report storage

## Installation

To see the project in action, it is necessary to setup 3 virtual networks and 7 virtual machines.

### Prerequisites

All virtual machines should be based on **Kali Linux 64-bit**.

**Required Software:**
- Java 21 or higher
- Maven 3.6+
- PostgreSQL 18+
- OpenSSL
- Git

### Network Configuration

The 3 following virtual networks should be configured on /Library/Preferences/VMware\ Fusion/networking:
- **VMNET2**: 192.168.0.0/24 (Citizens + CentralizedParty + AuthServer)
- **VMNET3**: 192.168.1.0/24 (CentralizedParty ↔ Municipalities)
- **VMNET4**: 192.168.2.0/24 (Municipalities ↔ Database)
```
answer VNET_2_HOSTONLY_NETMASK 255.255.255.0
answer VNET_2_HOSTONLY_SUBNET 192.168.0.0
answer VNET_3_HOSTONLY_NETMASK 255.255.255.0
answer VNET_3_HOSTONLY_SUBNET 192.168.1.0
answer VNET_4_HOSTONLY_NETMASK 255.255.255.0
answer VNET_4_HOSTONLY_SUBNET 192.168.2.0

```

**Citizen Machine**

1. Add a network adapter connected to `vmnet2` and generate MAC address
2. Add another network adapter connected to `share with my MAC` and generate MAC address
***

**Authentication Server Machine**

1. Add a network adapter connected to `vmnet2` and generate MAC address
2. Add another network adapter connected to `share with my MAC` and generate MAC address
***

**Centralized Party Machine**

1. Add a network adapter connected to `vmnet2` and generate MAC address
2. Add another network adapter connected to `share with my MAC` and generate MAC address
3. Add another network adapter connected to `vmnet3` and generate MAC address
***

**Municipality Machine**

1. Add a network adapter connected to `vmnet3` and generate MAC address
2. Add another network adapter connected to `share with my MAC` and generate MAC address
3. Add another network adapter connected to `vmnet4` and generate MAC address
***

**Database Machine**

1. Add a network adapter connected to `vmnet4` and generate MAC address
2. Add another network adapter connected to `share with my MAC` and generate MAC address

### Certificate Generation 

**Before running the application, you must generate the certificates for these components:**

```bash
./generate-fixed-certificates.sh
```

This script creates certificates for:
- Authentication Server (CA)
- Centralized Party
- Municipalities (Lisbon, Porto)
- Database Server

### Machine Initialization

**Only after certificate generation is complete**, proceed with VM setup using the initialization scripts in the `initScripts/` directory.

#### 1. Authentication Server (192.168.0.20)

This machine runs the authentication server that handles citizen enrollment and token issuance.

**Setup:**
```bash
cd initScripts/
chmod +x authServer.sh
sudo ./authServer.sh
```

**Start Service:**

The script already initiates the app but if you wish to run manually use:
```bash
cd authenticationServerApp/
mvn compile exec:java
```

**Expected Output:**
```
Authentication Server listening on port 8090...
```

#### 2. Centralized Party (192.168.0.10)

This machine acts as a gateway and anonymization service between citizens and municipalities.

**Setup:**
```bash
cd initScripts/
chmod +x centralizedParty.sh
sudo ./centralizedParty.sh
```

**Start Service:**

The script already initiates the app but if you wish to run manually use:
```bash
cd centralizedPartyApp/
mvn compile exec:java
```

**Expected Output:**
```
Centralized Party listening on port 8090...
```

#### 3. Database Server (192.168.2.100)

This machine runs PostgreSQL database for storing municipal reports.

**Setup:**
```bash
cd initScripts/
chmod +x database_setup.sh
sudo ./database_setup.sh
```

**Verify Installation:**
```bash
sudo -u postgres psql -d municipality -c "SELECT * FROM reports;"
```

**Expected Output:**
```
 report_id | location | report_data | created_at
-----------+----------+-------------+------------
(0 rows)
```

#### 4. Municipality Servers

##### Lisbon Municipality (192.168.1.1)

**Setup:**
```bash
cd initScripts/
chmod +x municipalityLisbon.sh
sudo ./municipalityLisbon.sh
```

**Start Service:**

The script already initiates the app but if you wish to run manually use:
```bash
cd municipalityApp/
mvn compile exec:java -Dexec.args="lisbon"
```

##### Porto Municipality (192.168.1.2)

**Setup:**
```bash
cd initScripts/
chmod +x municipalityPorto.sh
sudo ./municipalityPorto.sh
```

**Start Service:**

The script already initiates the app but if you wish to run manually use:
```bash
cd municipalityApp/
mvn compile exec:java -Dexec.args="porto"
```

**Expected Output (both):**
```
Municipality App Started - [municipality]
Starting [municipality] municipality server on port 8091...
Municipality server listening on port 8091...
```

#### 5. Citizen Machines (192.168.0.X)

**Setup:**
```bash
cd initScripts/
chmod +x citizen1.sh 
sudo ./citizen1.sh  
```

```bash
cd initScripts/
chmod +x citizen2.sh 
sudo ./citizen2.sh  
```

The script already initiates the app but if you wish to run manually use:

```bash
cd citizenApp/
mvn compile exec:java -Dexec.args="citizen1"
```
## Demonstration

To show the flow of the application we created a `demo` script that performs the login, creates 10 reports, tries to request more tokens and then reads all reports from its municipality. The token request will fail due to the spam prevention mechanisms.

The message payloads, evidence of the security mechanisms in action and relevant messages can be found in the Audit File: `audit.log`.

```bash
chmod +x demo.sh
./demo.sh
```

**Audit File example:**
```
--------------------------------------------------
Time   : 19-12-2025 15:55:42
Actor  : CLIENT
Action : UNPROTECTED REPORT JSON
Data   : {
  "report_id": "echo_08269",
  "timestamp": "2025-12-19T15:55:23.391107916Z",
  "category": "Demo report",
  "location": "lisbon",
  "coordinates": {
    "latitude": 948759.0,
    "longitude": 938479.0
  },
  "description": "this is a demo"
}
--------------------------------------------------
Time   : 19-12-2025 15:55:42
Actor  : CLIENT
Action : PROTECTED REPORT JSON
Data   : "eyJjaXBoZXJlZCI6IjdHQVJXeXJHVCtjcDNUVittV3BXWmpnMGVoNGNUSWg0NS9SbG51RXJuYi81dlFEd3FJZHNLeXFOMUJGSmtlOHA1LzJ3eHhJbTlXZE5KZjFLZHNORFowR2doTU04Sk1RT1ZKY0w2VklvRERWSnNVVnp1cGVoS21LVksxQkNRdGZTMmJOM2lsVlBYaE9uUFpHRjdCT0h4b0ZlRlk2VThXSlRtdnNMT3VqUHRGSHNLUGM1YmVvYVFVS1JXRGNod0ZrREZVYXdON2wvc004anBmK0pQbmx4NHVyM0VBWjlpNktyQ3Y3OEhKMG5NN0F0T0w5Z2RHakFJK0FWNU00OW81RTkxR1gwUG9YQSt6cFJhNkpWTzFnNEsxTXdoUmgyejlLcnExWUMxUVx1MDAzZFx1MDAzZCIsIml2IjoiNGRiMCtzWHIrdExEVGFYaCIsImFhZCI6eyJ0aW1lc3RhbXAiOiIyMDI1LTEyLTE5VDE1OjU1OjIzLjM5MTEwNzkxNloiLCJsb2NhdGlvbiI6Imxpc2JvbiIsIm9wZXJhdGlvbiI6IkNSRUFURSIsInRva2VuIjoiSzI1Uk9VZDFSRVYzZEVzNFdERkZSa0ppWjNneU1HRndRV0pHWXl0cU0wbzVUbkZsSzIxMVFVczRXVDA2TVRjMk5qRTJNRE14T1RVME1qcE1hWE5pYjI0XHUwMDNkLVNtQ0gyQlNiTitnNWdrdVh4dEEvM2JsT2lDQ0dkVWZpSExaTzF2elVJc1UyU2tzeGllUmVBQ3FyelRvVkczcGFnd21VblJNU1pQb29XNkJ1ZDMwc0JHL0YwWG1Gb1lsVlArRUdlRWduTEhXK1Z6QVg0U0JOZ1d5VjdFRjFJTjcrSklTcXNCZFlOaWpEMmplZldsMm5wWnJlV0FrYlJDYnpnaDZKenkxNTcyMDhDeWdsdC9FT0c2QStTZ2VoSXU0bGJDeUFzK2pocXpUeFFnMWtLbVBLRjZIQm1iRUpWc21NTE42OC9ZVDFLVnNQU0RQV3MzYmpVNk5vTlZ1UkpkSFRQVnFZN3NWSEg5bXNNaTJpeEI2SEl2WFJJc2I3WGNWb2VxcmNkNjZyTUNVMXRUdndFZkZaMWI4WUJLc0Y2UFk1b3pHZUw0aER0Nm9VWVVORmVuVzhPd1x1MDAzZFx1MDAzZCJ9LCJrZXkiOiJtSkdMeHZPYjRWWUI1dE5LcytnMWhsOFBvNVVkVHRaOEk0UGVnOW5vaTl4UElIenlnOXN6eDNscy9CYnBTeUJxNVhoaVBwbXJZRVlqYXVqVWlEdmtDR3dCZ0c1ODBQN2V3SVpqVUdjUnBaTlRvU0t3b3BYTkcwd1l2cSt1TjRpODRRK3ZtWmFhYW5DV1RjUDZXYkRwSDR4clY4MnJBWUYzT09Zd0d5dHk5MjJTczBrY0VvUndXMHlNS3lqYUE5ZDl4WVFKNWx2WDdyWTBlbmM5NVdUUy85RWtzakp5dmdSWVRjTWlSclVRV2U3TzZzWDAxa3BlckRLTGFNUU80b2pOdlpwMXpSOG0rbzcvRDkvRVpqQnFiWE9JcFllblVoTDN6R0dFRUJPSndaZHBlTW5mTW45YmkraFBiTDNIaG9tVTgrdU5xRktpUklSL2dQSTRPR0xxdVFcdTAwM2RcdTAwM2QifQ\u003d\u003d"
--------------------------------------------------
Time   : 19-12-2025 15:55:42
Actor  : MUNICIPALITY
Action : RECEIVED PROTECTED JSON
Data   : {
  "ciphered": "7GARWyrGT+cp3TV+mWpWZjg0eh4cTIh45/RlnuErnb/5vQDwqIdsKyqN1BFJke8p5/2wxxIm9WdNJf1KdsNDZ0GghMM8JMQOVJcL6VIoDDVJsUVzupehKmKVK1BCQtfS2bN3ilVPXhOnPZGF7BOHxoFeFY6U8WJTmvsLOujPtFHsKPc5beoaQUKRWDchwFkDFUawN7l/sM8jpf+JPnlx4ur3EAZ9i6KrCv78HJ0nM7AtOL9gdGjAI+AV5M49o5E91GX0PoXA+zpRa6JVO1g4K1MwhRh2z9Krq1YC1Q\u003d\u003d",
  "iv": "4db0+sXr+tLDTaXh",
  "aad": {
    "timestamp": "2025-12-19T15:55:23.391107916Z",
    "location": "lisbon",
    "operation": "CREATE",
    "token": "K25ROUd1REV3dEs4WDFFRkJiZ3gyMGFwQWJGYytqM0o5TnFlK211QUs4WT06MTc2NjE2MDMxOTU0MjpMaXNib24\u003d-SmCH2BSbN+g5gkuXxtA/3blOiCCGdUfiHLZO1vzUIsU2SksxieReACqrzToVG3pagwmUnRMSZPooW6Bud30sBG/F0XmFoYlVP+EGeEgnLHW+VzAX4SBNgWyV7EF1IN7+JISqsBdYNijD2jefWl2npZreWAkbRCbzgh6Jzy157208Cyglt/EOG6A+SgehIu4lbCyAs+jhqzTxQg1kKmPKF6HBmbEJVsmMLN68/YT1KVsPSDPWs3bjU6NoNVuRJdHTPVqY7sVHH9msMi2ixB6HIvXRIsb7XcVoeqrcd66rMCU1tTvwEfFZ1b8YBKsF6PY5ozGeL4hDt6oUYUNFenW8Ow\u003d\u003d"
  },
  "key": "mJGLxvOb4VYB5tNKs+g1hl8Po5UdTtZ8I4Peg9noi9xPIHzyg9szx3ls/BbpSyBq5XhiPpmrYEYjaujUiDvkCGwBgG580P7ewIZjUGcRpZNToSKwopXNG0wYvq+uN4i84Q+vmZaaanCWTcP6WbDpH4xrV82rAYF3OOYwGyty922Ss0kcEoRwW0yMKyjaA9d9xYQJ5lvX7rY0enc95WTS/9EksjJyvgRYTcMiRrUQWe7O6sX01kperDKLaMQO4ojNvZp1zR8m+o7/D9/EZjBqbXOIpYenUhL3zGGEEBOJwZdpeMnfMn9bi+hPbL3HhomU8+uNqFKiRIR/gPI4OGLquQ\u003d\u003d"
}
--------------------------------------------------
...
```

## Additional Information

### Links to Used Tools and Libraries

- [Java 21](https://openjdk.java.net/) - Primary development platform
- [Maven 3.9+](https://maven.apache.org/) - Build and dependency management
- [PostgreSQL 18](https://www.postgresql.org/) - Database system
- [Gson 2.10.1](https://github.com/google/gson) - JSON serialization
- [OpenSSL](https://www.openssl.org/) - Certificate generation and cryptography
- [Kali Linux](https://www.kali.org/) - Operating system for VMs

### Versioning

We use [SemVer](http://semver.org/) for versioning.  

### License

This project is licensed under the MIT License - see the [LICENSE.txt](LICENSE.txt) for details.

----
END OF README
