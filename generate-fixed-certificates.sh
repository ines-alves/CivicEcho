#!/bin/bash

set -e

echo "=== CivicEcho PKI - Authentication Server as Root CA ==="

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

echo "Created Authentication Server CA: ca.key, ca.crt"

echo ""
echo "=== 1b. Create Authentication Server's own HTTPS certificate ==="
# Generate server key
openssl genrsa -out server.key 2048

# Create CSR for the authentication server itself
openssl req -new -key server.key -out server.csr \
  -subj "/CN=auth.civicecho.gov/O=CivicEcho Authentication Server/C=PT"

# Sign the authentication server's own certificate with its CA
openssl x509 -req -days 365 -in server.csr -CA ca.crt -CAkey ca.key \
  -CAserial ca.srl -CAcreateserial -out server.crt

# Create chain file for the authentication server
cat server.crt ca.crt > server-chain.pem

echo "Created Authentication Server certificate: server.key, server.crt, server-chain.pem"
cd ..

echo ""
echo "=== 2. Create Database Server Certificate (signed by Auth Server CA) ==="
cd db-server

openssl genrsa -out db.key 2048
openssl req -new -key db.key -out db.csr \
  -subj "/CN=db.civicecho.internal/O=CivicEcho Database Server/C=PT"

openssl x509 -req -days 365 -in db.csr \
  -CA ../authentication-server/ca.crt \
  -CAkey ../authentication-server/ca.key \
  -CAserial ../authentication-server/ca.srl \
  -CAcreateserial \
  -out db.crt

# Create chain file (server cert + CA cert)
cat db.crt ../authentication-server/ca.crt > db-chain.pem
echo "Created: db.key, db.crt, db-chain.pem"
cd ..

echo ""
echo "=== 3. Create Centralized Party Server Certificate ==="
cd centralized-party

openssl genrsa -out centralized.key 2048
openssl req -new -key centralized.key -out centralized.csr \
  -subj "/CN=centralized.civicecho.gov/O=CivicEcho Centralized Party/C=PT"

openssl x509 -req -days 365 -in centralized.csr \
  -CA ../authentication-server/ca.crt \
  -CAkey ../authentication-server/ca.key \
  -CAserial ../authentication-server/ca.srl \
  -CAcreateserial \
  -out centralized.crt

cat centralized.crt ../authentication-server/ca.crt > centralized-chain.pem
echo "Created: centralized.key, centralized.crt, centralized-chain.pem"
cd ..

echo ""
echo "=== 4. Create Lisbon Municipality Certificate ==="
cd municipalities/lisbon

openssl genrsa -out lisbon.key 2048
openssl req -new -key lisbon.key -out lisbon.csr \
  -subj "/CN=municipality-lisbon.civicecho.gov/O=Lisbon Municipality/C=PT"

openssl x509 -req -days 365 -in lisbon.csr \
  -CA ../../authentication-server/ca.crt \
  -CAkey ../../authentication-server/ca.key \
  -CAserial ../../authentication-server/ca.srl \
  -CAcreateserial \
  -out lisbon.crt

cat lisbon.crt ../../authentication-server/ca.crt > lisbon-chain.pem
echo "Created: lisbon.key, lisbon.crt, lisbon-chain.pem"
cd ../..

echo ""
echo "=== 5. Create Porto Municipality Certificate ==="
cd municipalities/porto

openssl genrsa -out porto.key 2048
openssl req -new -key porto.key -out porto.csr \
  -subj "/CN=municipality-porto.civicecho.gov/O=Porto Municipality/C=PT"

openssl x509 -req -days 365 -in porto.csr \
  -CA ../../authentication-server/ca.crt \
  -CAkey ../../authentication-server/ca.key \
  -CAserial ../../authentication-server/ca.srl \
  -CAcreateserial \
  -out porto.crt

cat porto.crt ../../authentication-server/ca.crt > porto-chain.pem
echo "Created: porto.key, porto.crt, porto-chain.pem"
cd ../..
