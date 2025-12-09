#!/bin/bash

# TLS Certificate Setup with Centralized Party as CA for clients
set -e

echo "=== Creating TLS Certificates ==="
echo "Note: Centralized Party acts as CA for clients"

# Create directory structure
mkdir -p certificates/{centralized-party,municipalities/{lisbon,porto},clients/citizen1,database}
cd certificates

echo ""
echo "--- Step 1: Create Centralized Party (acts as CA for clients) ---"
cd centralized-party

# Generate CP key pair
openssl genrsa -out cp.key 2048
echo "Created: cp.key"

# Create self-signed CP certificate (acts as CA)
openssl req -new -x509 -days 3650 -key cp.key -out cp.crt \
  -subj "/CN=centralized-party-ca"
echo "Created: cp.crt (self-signed, acts as CA)"

# Create .srl file for signing
echo "01" > cp.srl
echo "Created: cp.srl (serial file)"

# Create PKCS12 for CP
openssl pkcs12 -export -in cp.crt -inkey cp.key -out cp.p12 -password pass:changeme
echo "Created: cp.p12"

# Create empty truststore (will add clients later)
keytool -import -trustcacerts -file cp.crt -alias cp-ca \
  -keystore servertruststore.jks -storepass changeme -noprompt
echo "Created: servertruststore.jks (with CP as CA)"
cd ..

echo ""
echo "--- Step 2: Create Municipalities (signed by CP as CA) ---"

# Lisbon
echo "Creating Lisbon municipality..."
cd municipalities/lisbon

# Generate RSA private key (PEM temporarily)
openssl genrsa -out lisbon.key 2048

# Convert private key to DER PKCS#8 (Java compatible)
openssl pkcs8 -topk8 -nocrypt -in lisbon.key -outform DER -out lisbon.privatekey

# Generate public key in DER X.509 format (Java compatible)
openssl rsa -in lisbon.key -pubout -outform DER -out lisbon.pubkey

# Generate CSR
openssl req -new -key lisbon.key -out lisbon.csr -subj "/CN=municipality-lisbon"

# Sign certificate
openssl x509 -req -days 365 -in lisbon.csr -CA ../../centralized-party/cp.crt \
  -CAkey ../../centralized-party/cp.key -CAserial ../../centralized-party/cp.srl -out lisbon.crt

echo "Created: lisbon.key (PEM), lisbon.privatekey (DER), lisbon.pubkey (DER), lisbon.csr, lisbon.crt"

# Create PKCS#12 bundle (for browsers / tools)
openssl pkcs12 -export -in lisbon.crt -inkey lisbon.key -out lisbon.p12 -password pass:changeme

echo "Created: lisbon.p12"


# Lisbon truststore (trusts CP as CA)
keytool -import -trustcacerts -file ../../centralized-party/cp.crt -alias cp-ca \
  -keystore lisbon-truststore.jks -storepass changeme -noprompt
echo "Created: lisbon-truststore.jks (trusts CP as CA)"
cd ../..

# Porto
echo "Creating Porto municipality..."
cd municipalities/porto
openssl genrsa -out porto.key 2048
openssl req -new -key porto.key -out porto.csr -subj "/CN=municipality-porto"
openssl x509 -req -days 365 -in porto.csr -CA ../../centralized-party/cp.crt \
  -CAkey ../../centralized-party/cp.key -CAserial ../../centralized-party/cp.srl -out porto.crt
echo "Created: porto.key, porto.csr, porto.crt"

openssl pkcs12 -export -in porto.crt -inkey porto.key -out porto.p12 -password pass:changeme
echo "Created: porto.p12"

# Porto truststore (trusts CP as CA)
keytool -import -trustcacerts -file ../../centralized-party/cp.crt -alias cp-ca \
  -keystore porto-truststore.jks -storepass changeme -noprompt
echo "Created: porto-truststore.jks (trusts CP as CA)"
cd ../..

echo ""
echo "--- Step 3: Create Database (signed by CP as CA) ---"
cd database

# Generate DB key
openssl genrsa -out db.key 2048
echo "Created: db.key"

# Create CSR
openssl req -new -key db.key -out db.csr -subj "/CN=database"
echo "Created: db.csr"

# Sign with CP as CA
openssl x509 -req -days 365 -in db.csr -CA ../centralized-party/cp.crt \
  -CAkey ../centralized-party/cp.key -CAserial ../centralized-party/cp.srl -out db.crt
echo "Created: db.crt (signed by CP)"

# Create PKCS12
openssl pkcs12 -export -in db.crt -inkey db.key -out db.p12 -password pass:changeme
echo "Created: db.p12"

# DB truststore (trusts DB as CA)
keytool -import -trustcacerts -file ../municipalities/lisbon/lisbon.crt -alias lisbon \
  -keystore ../../database/db-truststore.jks -storepass changeme -noprompt

echo "Created: db-truststore.jks (trusts CP as CA)"
cd ..

echo ""
echo "--- Step 4: Create Client (signed by CP as CA) ---"
cd clients/citizen1

# Generate client key
openssl genrsa -out citizen1.key 2048
echo "Created: citizen1.key"

# Create CSR
openssl req -new -key citizen1.key -out citizen1.csr -subj "/CN=citizen1"
echo "Created: citizen1.csr"

# Sign with CP as CA
openssl x509 -req -days 365 -in citizen1.csr -CA ../../centralized-party/cp.crt \
  -CAkey ../../centralized-party/cp.key -CAserial ../../centralized-party/cp.srl -out citizen1.crt
echo "Created: citizen1.crt (signed by CP)"

# Convert to PEM
openssl x509 -in citizen1.crt -out citizen1.pem
echo "Created: citizen1.pem"

# Create PKCS12
openssl pkcs12 -export -in citizen1.crt -inkey citizen1.key -out citizen1.p12 -password pass:changeme
echo "Created: citizen1.p12"

# Client truststore (trusts CP as CA)
keytool -import -trustcacerts -file ../../centralized-party/cp.crt -alias cp-ca \
  -keystore usertruststore.jks -storepass changeme -noprompt
echo "Created: usertruststore.jks (trusts CP as CA)"

# Add client certificate to CP's truststore
echo "Adding client to CP's truststore..."
keytool -import -trustcacerts -file citizen1.crt -alias citizen1 \
  -keystore ../../centralized-party/servertruststore.jks -storepass changeme -noprompt
echo "✓ Added citizen1.crt to CP truststore"

# Also add municipality certificates to CP's truststore
echo "Adding municipalities to CP's truststore..."
keytool -import -trustcacerts -file ../../municipalities/lisbon/lisbon.crt -alias lisbon \
  -keystore ../../centralized-party/servertruststore.jks -storepass changeme -noprompt
keytool -import -trustcacerts -file ../../municipalities/porto/porto.crt -alias porto \
  -keystore ../../centralized-party/servertruststore.jks -storepass changeme -noprompt
echo "✓ Added municipality certificates to CP truststore"

cd ../..

echo ""
echo "=== Setup Complete ==="
echo ""
echo "Certificate Hierarchy:"
echo "  CP (self-signed) → Acts as CA"
echo "    ├── citizen1.crt (signed by CP)"
echo "    ├── lisbon.crt (signed by CP)"
echo "    ├── porto.crt (signed by CP)"
echo "    └── db.crt (signed by CP)"
echo ""
echo "Files created:"
echo "Centralized Party (acts as CA):"
echo "  certificates/centralized-party/cp.key"
echo "  certificates/centralized-party/cp.crt (self-signed CA)"
echo "  certificates/centralized-party/cp.p12"
echo "  certificates/centralized-party/servertruststore.jks (trusts: all clients + municipalities)"
echo ""
echo "Municipality Lisbon:"
echo "  certificates/municipalities/lisbon/lisbon.key"
echo "  certificates/municipalities/lisbon/lisbon.crt"
echo "  certificates/municipalities/lisbon/lisbon.p12"
echo "  certificates/municipalities/lisbon/lisbon-truststore.jks (trusts: cp.crt)"
echo ""
echo "Municipality Porto:"
echo "  certificates/municipalities/porto/porto.key"
echo "  certificates/municipalities/porto/porto.crt"
echo "  certificates/municipalities/porto/porto.p12"
echo "  certificates/municipalities/porto/porto-truststore.jks (trusts: cp.crt)"
echo ""
echo "Database:"
echo "  certificates/database/db.key"
echo "  certificates/database/db.crt"
echo "  certificates/database/db.p12"
echo "  certificates/database/db-truststore.jks (trusts: cp.crt)"
echo ""
echo "Client Citizen1:"
echo "  certificates/clients/citizen1/citizen1.key"
echo "  certificates/clients/citizen1/citizen1.crt"
echo "  certificates/clients/citizen1/citizen1.p12"
echo "  certificates/clients/citizen1/usertruststore.jks (trusts: cp.crt)"
echo ""
echo "Java Properties for Centralized Party:"
echo "  -Djavax.net.ssl.keyStore=certificates/centralized-party/cp.p12"
echo "  -Djavax.net.ssl.keyStorePassword=changeme"
echo "  -Djavax.net.ssl.trustStore=certificates/centralized-party/servertruststore.jks"
echo "  -Djavax.net.ssl.trustStorePassword=changeme"
