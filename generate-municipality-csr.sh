#!/bin/bash

set -e

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <municipality_id> <ca_certificate_path>"
    echo "Example: $0 lisbon ../certificates/auth-server/auth-server.crt"
    echo "Example: $0 porto ../certificates/auth-server/auth-server.crt"
    exit 1
fi

MUNICIPALITY_ID="$1"
CA_CERT_PATH="$2"
OUTPUT_DIR="municipalities/$MUNICIPALITY_ID"
PUBKEYS_DIR="../pubkeys" 

echo "=== Generating Municipality Certificate Materials: $MUNICIPALITY_ID ==="

if [ ! -f "$CA_CERT_PATH" ]; then
    echo "Error: CA certificate not found at: $CA_CERT_PATH"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"
cd "$OUTPUT_DIR"

echo ""
echo "--- Step 1: Generate Private Key ---"
openssl genrsa -out "$MUNICIPALITY_ID.key" 2048
echo "✓ Created: $MUNICIPALITY_ID.key (2048-bit RSA private key)"

echo ""
echo "--- Step 2: Generate Certificate Signing Request (CSR) ---"
openssl req -new -key "$MUNICIPALITY_ID.key" -out "$MUNICIPALITY_ID.csr" \
  -subj "/CN=municipality-$MUNICIPALITY_ID/O=CivicEcho Municipality/OU=Government"
echo "✓ Created: $MUNICIPALITY_ID.csr (Certificate Signing Request)"

echo ""
echo "--- Step 3: Generate Public Key from Private Key ---"
openssl rsa -in "$MUNICIPALITY_ID.key" -pubout -out "$MUNICIPALITY_ID.pub"
echo "✓ Created: $MUNICIPALITY_ID.pub (Public key)"

echo ""
echo "--- Step 4: Copy Public Key to pubkeys directory ---"
if [ -d "$PUBKEYS_DIR" ]; then
    cp "$MUNICIPALITY_ID.pub" "$PUBKEYS_DIR/$MUNICIPALITY_ID.pub"
    echo "✓ Copied: $MUNICIPALITY_ID.pub to $PUBKEYS_DIR/"
else
    echo "⚠ Warning: pubkeys directory not found at $PUBKEYS_DIR"
    echo "  Creating pubkeys directory..."
    mkdir -p "$PUBKEYS_DIR"
    cp "$MUNICIPALITY_ID.pub" "$PUBKEYS_DIR/$MUNICIPALITY_ID.pub"
    echo "✓ Created pubkeys directory and copied $MUNICIPALITY_ID.pub"
fi

echo ""
echo "--- Step 5: Create Truststore with CA Certificate ---"
# Create a keystore with the CA certificate
keytool -import -trustcacerts -file "$CA_CERT_PATH" -alias auth-ca \
  -keystore truststore.jks -storepass changeme -noprompt
echo "✓ Created: truststore.jks (contains CA certificate)"

echo ""
echo "--- Step 6: Create PEM files for easy use ---"
# Private key in PEM format
cp "$MUNICIPALITY_ID.key" "$MUNICIPALITY_ID-private.pem"
echo "✓ Created: $MUNICIPALITY_ID-private.pem (Private key in PEM format)"

# Public key in PEM format
cp "$MUNICIPALITY_ID.pub" "$MUNICIPALITY_ID-public.pem"
echo "✓ Created: $MUNICIPALITY_ID-public.pem (Public key in PEM format)"
