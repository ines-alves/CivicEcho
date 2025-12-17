# CivicEcho - Secure Citizen Reporting System

## Team

| Number  | Name              | User                                   | E-mail                                                 |
| --------|-------------------|----------------------------------------| -------------------------------------------------------|
| 1104147 | Diogo Rodrigues   | <https://github.com/diogotrodrigues>   | <mailto:diogo.teixeira.rodrigues@tecnico.ulisboa.pt>   |
| 1106378 | Luca Dallalana    | <https://github.com/luca-dallalana>     | <mailto:luca.dallalana@tecnico.ulisboa.pt>            |
| 1107157 | Inês Alves        | <https://github.com/ines-alves>        | <mailto:ines.b.alves@tecnico.ulisboa.pt>               |


![Alice](img/alice.png) ![Bob](img/bob.png) ![Luca](img/luca.png)

*(add face photos with 150px height; faces should have similar size and framing)*

## Contents

This repository contains documentation and source code for the *Network and Computer Security (SIRS)* project.

The [REPORT](REPORT.md) document provides a detailed overview of the key technical decisions and various components of the implemented project.
It offers insights into the rationale behind these choices, the project's architecture, and the impact of these decisions on the overall functionality and performance of the system.

This document presents installation and demonstration instructions.

## Project Overview

**CivicEcho** is a secure citizen reporting system designed to enable anonymous civic participation while maintaining report authenticity and integrity. The system allows citizens to submit legitimate reports about civic issues to their municipalities while preserving their anonymity and preventing malicious actors from submitting false reports.

### Key Features

- **Anonymous Reporting**: Citizens can submit reports without revealing their identity
- **Authenticated Access**: Token-based authentication ensures only legitimate citizens can create reports
- **Document Security**: Hybrid encryption (AES-GCM + RSA) protects data confidentiality and integrity
- **Multi-Municipality Support**: System supports multiple municipalities (Lisbon, Porto)
- **Replay Protection**: Prevents token reuse and replay attacks
- **Secure Communication**: TLS 1.3 with mutual authentication between all components

## System Architecture

The system operates in a distributed network environment with five main components:

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ Authentication  │    │     Citizens     │    │  Centralized    │
│     Server      │◄──►│                  │◄──►│     Party       │
│  192.168.0.20   │    │  192.168.0.100+  │    │ 192.168.0.10    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                        │
                       ┌─────────────────┐              │
                       │    Database     │              │
                       │ 192.168.2.100   │              │
                       └─────────────────┘              │
                                △                       │ 
                                │                       │
                       ┌─────────────────┐              │
                       │ Municipalities  │◄─────────────┘
                       │ Lisbon: 192.168.1.1
                       │ Porto:  192.168.1.2
                       └─────────────────┘
```

### Components

1. **CitizenApp**: Client application for report creation and management
2. **AuthenticationServer**: Issues tokens and manages citizen enrollment
3. **CentralizedParty**: Trusted intermediary for anonymization and routing
4. **MunicipalityApp**: Processes reports for specific municipalities
5. **Database**: PostgreSQL database for report storage

## Installation

To see the project in action, it is necessary to setup a virtual environment with 5 networks and 7 virtual machines.

### Prerequisites

All virtual machines should be based on **Kali Linux 64-bit**.

**Required Software:**
- Java 21 or higher
- Maven 3.6+
- PostgreSQL 18+
- OpenSSL
- Git

### Network Configuration

The system uses the following network topology:
- **COONNECTED TO INTERNET** CentralizedParty
- **VMNET2**: 192.168.0.0/24 (Citizens + CentralizedParty + AuthServer)
- **VMNET3**: 192.168.1.0/24 (CentralizedParty ↔ Municipalities)
- **VMNET4**: 192.168.2.0/24 (Municipalities ↔ Database)

### Certificate Generation 

**Before setting up any VMs, you must generate the certificates for all components:**

```bash
./generate-fixed-certificates.sh
```

This script creates certificates for:
- Authentication Server (CA)
- Centralized Party
- Municipalities (Lisbon, Porto)
- Database Server
- Citizens (generated dynamically during enrollment)

### Machine Configurations

**Only after certificate generation is complete**, proceed with VM setup using the initialization scripts in the `initScripts/` directory.

#### 1. Authentication Server (192.168.0.20)

This machine runs the authentication server that handles citizen enrollment and token issuance.

**Setup:**
```bash
cd initScripts/
sudo ./authServer.sh
```

**Start Service:**
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
sudo ./centralizedParty.sh
```

**Start Service:**
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
sudo ./municipalityLisbon.sh
```

**Start Service:**
```bash
cd municipalityApp/
mvn compile exec:java -Dexec.args="lisbon"
```

##### Porto Municipality (192.168.1.2)

**Setup:**
```bash
cd initScripts/
sudo ./municipalityPorto.sh
```

**Start Service:**
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

#### 5. Citizen Machines (192.168.0.100+)

**Setup:**
```bash
cd initScripts/
sudo ./citizen1.sh  
```

## Demonstration

Now that all networks and machines are up and running, let's demonstrate the key features of CivicEcho.

### 1. Citizen Enrollment and Login

Start the citizen application and perform enrollment:

```bash
cd citizenApp/
mvn compile exec:java -Dexec.args="citizen1"
```

**Expected Flow:**
1. Certificate generation for citizen1
2. Authentication server enrollment
3. Successful login
4. **Automatic token grant**: First-time login automatically grants 10 tokens

**Security Evidence:** The system generates unique certificates per citizen and establishes TLS 1.3 connections. New citizens receive an initial allocation of 10 tokens upon first successful authentication.

### 2. Token Management

Citizens receive 10 tokens automatically upon first login. When tokens are exhausted, additional tokens can be requested:

```
┌─────────────────────────────────┐
│       CvicEcho CITIZEN APP      │
│             REPORT              │
│       AVAILABLE TOKENS: 0       │
├─────────────────────────────────┤
│ 3 ▸ Request Tokens              │
└─────────────────────────────────┘
```

**Security Evidence:** Tokens are cryptographically signed by the authentication server and contain location-based authorization.

### 3. Report Creation

Create a civic report with encrypted protection:

```
┌──────────────────────────────────────────────┐
│                 FINAL PREVIEW                │
├──────────────────────────────────────────────┤
│ Report ID   ▸ echo_71420                     │
│ Timestamp   ▸ 2025-12-16T23:33:59.060245290Z │
│ Category    ▸ Infrastructure                 │
│ Location    ▸ lisbon                         │
│ Latitude    ▸ 38.7223                        │
│ Longitude   ▸ -9.1393                        │
│ Description ▸ Pothole on Main Street         │
└──────────────────────────────────────────────┘
```

**Security Evidence:** Reports are encrypted using hybrid cryptography (AES-GCM + RSA) before transmission.

### 4. End-to-End Verification

The system demonstrates the following security mechanisms:

**Token Verification:**
```bash
# Centralized Party validates tokens
INVALID TOKEN: [token] # If token is invalid/expired
USED TOKEN: [token]    # If token was already used (replay protection)
```

**Database Storage:**
```sql
-- Verify report storage
SELECT report_id, location, created_at FROM reports WHERE location = 'lisbon';
```

**Network Security:**
All communication uses TLS 1.3 with mutual certificate authentication:
- Citizens ↔ Authentication Server
- Citizens ↔ Centralized Party
- Centralized Party ↔ Municipalities
- Municipalities ↔ Database

### 5. Read Reports

Citizens can query reports from their municipality:

```
┌─────────────────────────────────┐
│  How many reports do you want?  │
├─────────────────────────────────┤
│ 1 ▸ Custom                      │
│ 2 ▸ All                         │
│ 0 ▸ Go Back                     │
└─────────────────────────────────┘
```

**Security Evidence:** Responses are symmetrically encrypted using extracted keys, ensuring confidentiality.

This concludes the demonstration of CivicEcho's secure civic reporting system.

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

*(switch to another license, or no license, as you see fit)*

----
END OF README
