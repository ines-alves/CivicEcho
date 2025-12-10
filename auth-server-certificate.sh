#!/bin/bash

set -e

echo "=== Creating Authentication Server (CA) Certificates ==="

mkdir -p /certificates/auth-server
cd certificates

echo ""
echo "--- Creating Authentication Server (acts as CA) ---"
cd auth-server

echo "Generating CA private key..."
openssl genrsa -out auth-server.key 2048
echo "✓ Created: auth-server.key"

echo "Creating self-signed CA certificate..."
openssl req -new -x509 -days 3650 -key auth-server.key -out auth-server.crt \
  -subj "/CN=auth-server-ca/O=CivicEcho Authentication Server"
echo "✓ Created: auth-server.crt (self-signed, acts as CA)"

echo "01" > auth-server.srl
echo "✓ Created: auth-server.srl (serial file)"

echo "Creating PKCS12 bundle for authentication server..."
openssl pkcs12 -export -in auth-server.crt -inkey auth-server.key \
  -out auth-server.p12 -password pass:changeme
echo "✓ Created: auth-server.p12"

# Create truststore for the authentication server (trusts itself as CA)
echo "Creating authentication server truststore..."
keytool -import -trustcacerts -file auth-server.crt -alias auth-ca \
  -keystore truststore.jks -storepass changeme -noprompt
echo "✓ Created: truststore.jks (trusts auth-server as CA)"

cd ../..
