#!/bin/bash

set -e

CERTS_DIR="certificates"

# --- Safety cleanup ---
if [ -d "$CERTS_DIR" ]; then
    echo "Certificates detected at '$CERTS_DIR'"
    echo "Removing all existing certificates and keys..."
    rm -rf "$CERTS_DIR"
fi

echo "=== CivicEcho A03 - Authentication Server as Root CA ==="

mkdir -p certificates/{authentication-server,db-server,centralized-party,municipalities/lisbon,municipalities/porto}
cd certificates

echo ""
echo "=== 1. Create Authentication Server as Root CA ==="
cd authentication-server

# Create the Root CA key and certificate 
openssl genrsa -out ca.key 4096
openssl req -new -x509 -days 3650 -key ca.key -out ca.crt \
  -subj "/CN=CivicEcho Authentication CA/O=CivicEcho Authentication Server/C=PT"

# Create initial serial file
echo "01" > ca.srl

# Convert CA certificate to PEM for trust stores
openssl x509 -in ca.crt -out ca.pem

echo "Created Authentication Server CA: ca.key, ca.crt, ca.pem"

echo ""
echo "=== 1b. Create Authentication Server's own HTTPS certificate ==="
# Generate server key
openssl genrsa -out server.key 2048

# Convert private key to DER PKCS#8 (Java compatible)
openssl pkcs8 -topk8 -nocrypt -in server.key -outform DER -out server.privatekey

# Create CSR for the authentication server itself
openssl req -new -key server.key -out server.csr \
  -subj "/CN=auth.civicecho.gov/O=CivicEcho Authentication Server/C=PT"

# Sign the authentication server's own certificate with its CA
openssl x509 -req -days 365 -in server.csr -CA ca.crt -CAkey ca.key \
  -CAserial ca.srl -CAcreateserial -out server.crt

# Generate public key in DER X.509 format (Java compatible)
openssl rsa -in server.key -pubout -outform DER -out server.pubkey

# Create chain file for the authentication server
cat server.crt ca.crt > server-chain.pem

# Create PKCS#12 file (password: changeme)
openssl pkcs12 -export -in server.crt -inkey server.key \
  -out server.p12 -password pass:changeme

# Convert server certificate to PEM
openssl x509 -in server.crt -out server.pem

# Create trust store for authentication server (for trusting other entities if needed)
keytool -import -trustcacerts -file ca.pem \
  -alias "CivicEcho Authentication CA" \
  -keystore authentication-truststore.jks \
  -storepass changeme -noprompt

echo "Created Authentication Server certificate: server.key, server.privatekey, server.pubkey, server.crt, server-chain.pem, server.p12, server.pem, authentication-truststore.jks"
cd ..

echo ""
echo "=== 2. Create Database Server Certificate (signed by Auth Server CA) ==="
cd db-server

openssl genrsa -out db.key 2048

# Convert private key to DER PKCS#8 (Java compatible)
openssl pkcs8 -topk8 -nocrypt -in db.key -outform DER -out db.privatekey

openssl req -new -key db.key -out db.csr \
  -subj "/CN=db.civicecho.internal/O=CivicEcho Database Server/C=PT"

openssl x509 -req -days 365 -in db.csr \
  -CA ../authentication-server/ca.crt \
  -CAkey ../authentication-server/ca.key \
  -CAserial ../authentication-server/ca.srl \
  -CAcreateserial \
  -out db.crt

# Generate public key in DER X.509 format (Java compatible)
openssl rsa -in db.key -pubout -outform DER -out db.pubkey

# Create chain file (server cert + CA cert)
cat db.crt ../authentication-server/ca.crt > db-chain.pem

# Create PKCS#12 file (password: changeme)
openssl pkcs12 -export -in db.crt -inkey db.key \
  -out db.p12 -password pass:changeme

# Convert certificate to PEM
openssl x509 -in db.crt -out db.pem

# Create trust store for DB server (trusts the Authentication Server CA)
keytool -import -trustcacerts -file ../authentication-server/ca.pem \
  -alias "CivicEcho Authentication CA" \
  -keystore db-truststore.jks \
  -storepass changeme -noprompt

echo "Created: db.key, db.privatekey, db.pubkey, db.crt, db-chain.pem, db.p12, db.pem, db-truststore.jks"
cd ..

echo ""
echo "=== 3. Create Centralized Party Server Certificate ==="
cd centralized-party

openssl genrsa -out centralized.key 2048

# Convert private key to DER PKCS#8 (Java compatible)
openssl pkcs8 -topk8 -nocrypt -in centralized.key -outform DER -out centralized.privatekey

openssl req -new -key centralized.key -out centralized.csr \
  -subj "/CN=centralized.civicecho.gov/O=CivicEcho Centralized Party/C=PT"

openssl x509 -req -days 365 -in centralized.csr \
  -CA ../authentication-server/ca.crt \
  -CAkey ../authentication-server/ca.key \
  -CAserial ../authentication-server/ca.srl \
  -CAcreateserial \
  -out centralized.crt

# Generate public key in DER X.509 format (Java compatible)
openssl rsa -in centralized.key -pubout -outform DER -out centralized.pubkey

cat centralized.crt ../authentication-server/ca.crt > centralized-chain.pem

# Create PKCS#12 file (password: changeme)
openssl pkcs12 -export -in centralized.crt -inkey centralized.key \
  -out centralized.p12 -password pass:changeme

# Convert certificate to PEM
openssl x509 -in centralized.crt -out centralized.pem

# Create trust store for Centralized Party (trusts the Authentication Server CA)
keytool -import -trustcacerts -file ../authentication-server/ca.pem \
  -alias "CivicEcho Authentication CA" \
  -keystore centralized-truststore.jks \
  -storepass changeme -noprompt

echo "Created: centralized.key, centralized.privatekey, centralized.pubkey, centralized.crt, centralized-chain.pem, centralized.p12, centralized.pem, centralized-truststore.jks"
cd ..

echo ""
echo "=== 4. Create Lisbon Municipality Certificate ==="
cd municipalities/lisbon

# Generate RSA key (matches your specified format)
openssl genrsa -out lisbon.key 2048

# Convert private key to DER PKCS#8 (Java compatible) - matches your specified format
openssl pkcs8 -topk8 -nocrypt -in lisbon.key -outform DER -out lisbon.privatekey

openssl req -new -key lisbon.key -out lisbon.csr \
  -subj "/CN=municipality-lisbon.civicecho.gov/O=Lisbon Municipality/C=PT"

openssl x509 -req -days 365 -in lisbon.csr \
  -CA ../../authentication-server/ca.crt \
  -CAkey ../../authentication-server/ca.key \
  -CAserial ../../authentication-server/ca.srl \
  -CAcreateserial \
  -out lisbon.crt

# Generate public key in DER X.509 format (Java compatible) - matches your specified format
openssl rsa -in lisbon.key -pubout -outform DER -out lisbon.pubkey

cat lisbon.crt ../../authentication-server/ca.crt > lisbon-chain.pem

# Create PKCS#12 file (password: changeme)
openssl pkcs12 -export -in lisbon.crt -inkey lisbon.key \
  -out lisbon.p12 -password pass:changeme

# Convert certificate to PEM
openssl x509 -in lisbon.crt -out lisbon.pem

# Create trust store for Lisbon (trusts the Authentication Server CA)
keytool -import -trustcacerts -file ../../authentication-server/ca.pem \
  -alias "CivicEcho Authentication CA" \
  -keystore lisbon-truststore.jks \
  -storepass changeme -noprompt

echo "Created: lisbon.key, lisbon.privatekey, lisbon.pubkey, lisbon.crt, lisbon-chain.pem, lisbon.p12, lisbon.pem, lisbon-truststore.jks"
cd ../..

echo ""
echo "=== 5. Create Porto Municipality Certificate ==="
cd municipalities/porto

openssl genrsa -out porto.key 2048

# Convert private key to DER PKCS#8 (Java compatible)
openssl pkcs8 -topk8 -nocrypt -in porto.key -outform DER -out porto.privatekey

openssl req -new -key porto.key -out porto.csr \
  -subj "/CN=municipality-porto.civicecho.gov/O=Porto Municipality/C=PT"

openssl x509 -req -days 365 -in porto.csr \
  -CA ../../authentication-server/ca.crt \
  -CAkey ../../authentication-server/ca.key \
  -CAserial ../../authentication-server/ca.srl \
  -CAcreateserial \
  -out porto.crt

# Generate public key in DER X.509 format (Java compatible)
openssl rsa -in porto.key -pubout -outform DER -out porto.pubkey

cat porto.crt ../../authentication-server/ca.crt > porto-chain.pem

# Create PKCS#12 file (password: changeme)
openssl pkcs12 -export -in porto.crt -inkey porto.key \
  -out porto.p12 -password pass:changeme

# Convert certificate to PEM
openssl x509 -in porto.crt -out porto.pem

# Create trust store for Porto (trusts the Authentication Server CA)
keytool -import -trustcacerts -file ../../authentication-server/ca.pem \
  -alias "CivicEcho Authentication CA" \
  -keystore porto-truststore.jks \
  -storepass changeme -noprompt

echo "Created: porto.key, porto.privatekey, porto.pubkey, porto.crt, porto-chain.pem, porto.p12, porto.pem, porto-truststore.jks"
cd ../..

echo ""
echo "=== All certificates, keys, trust stores, and PKCS#12 files generated successfully! ==="
echo ""
