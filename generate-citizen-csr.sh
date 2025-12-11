#!/bin/bash

set -e

CITIZEN_ID="$1"
CA_CERT_PATH="$2"
OUTPUT_DIR="citizens/$CITIZEN_ID"
PUBKEYS_DIR="../../../pubkeys"  

echo "=== Generating Citizen Certificate Materials: $CITIZEN_ID ==="

if [ ! -f "$CA_CERT_PATH" ]; then
    echo "Error: CA certificate not found at: $CA_CERT_PATH"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"
cd "$OUTPUT_DIR"

echo ""
echo "--- Step 1: Generate Private Key ---"
# Generate RSA private key
openssl genrsa -out "$CITIZEN_ID.key" 2048
echo "Created: $CITIZEN_ID.key (2048-bit RSA private key)"

echo ""
echo "--- Step 2: Convert Private Key to DER PKCS#8 (Java compatible) ---"
# Convert private key to DER PKCS#8 format
openssl pkcs8 -topk8 -nocrypt -in "$CITIZEN_ID.key" -outform DER -out "$CITIZEN_ID.privatekey"
echo "Created: $CITIZEN_ID.privatekey (DER PKCS#8 format)"

echo ""
echo "--- Step 3: Generate Certificate Signing Request (CSR) ---"
openssl req -new -key "$CITIZEN_ID.key" -out "$CITIZEN_ID.csr" \
  -subj "/CN=$CITIZEN_ID/O=CivicEcho Citizen/OU=Client"
echo "Created: $CITIZEN_ID.csr (Certificate Signing Request)"

echo ""
echo "--- Step 4: Generate Public Key in DER X.509 format (Java compatible) ---"
# Generate public key in DER format
openssl rsa -in "$CITIZEN_ID.key" -pubout -outform DER -out "$CITIZEN_ID.pubkey"
echo "Created: $CITIZEN_ID.pubkey (Public key in DER X.509 format)"

echo ""
echo "--- Step 5: Generate Public Key in PEM format ---"
# Also generate public key in PEM format for compatibility
openssl rsa -in "$CITIZEN_ID.key" -pubout -out "$CITIZEN_ID.pub"
echo "Created: $CITIZEN_ID.pub (Public key in PEM format)"

echo ""
echo "--- Step 6: Copy Public Key to pubkeys directory ---"
if [ -d "$PUBKEYS_DIR" ]; then
    # Copy both DER and PEM versions to pubkeys directory
    cp "$CITIZEN_ID.pubkey" "$PUBKEYS_DIR/$CITIZEN_ID.pubkey"
    cp "$CITIZEN_ID.pub" "$PUBKEYS_DIR/$CITIZEN_ID.pub"
    echo "Copied: $CITIZEN_ID.pubkey and $CITIZEN_ID.pub to $PUBKEYS_DIR/"
else
    echo "Warning: pubkeys directory not found at $PUBKEYS_DIR"
    echo "Creating pubkeys directory..."
    mkdir -p "$PUBKEYS_DIR"
    cp "$CITIZEN_ID.pubkey" "$PUBKEYS_DIR/$CITIZEN_ID.pubkey"
    cp "$CITIZEN_ID.pub" "$PUBKEYS_DIR/$CITIZEN_ID.pub"
    echo "Created pubkeys directory and copied $CITIZEN_ID.pubkey and $CITIZEN_ID.pub"
fi

echo ""
echo "--- Step 7: Create Truststore with CA Certificate ---"
if [ -f "truststore.jks" ]; then
    echo "Removing existing truststore.jks"
    rm -f truststore.jks
fi

# Create a keystore with the CA certificate
keytool -import -trustcacerts -file "../../$CA_CERT_PATH" -alias auth-ca \
  -keystore truststore.jks -storepass changeme -noprompt
echo "Created: truststore.jks (contains CA certificate)"
echo ""
echo "--- Step 8: Create PEM files for easy use ---"
# Private key in PEM format
cp "$CITIZEN_ID.key" "$CITIZEN_ID-private.pem"
echo "Created: $CITIZEN_ID-private.pem (Private key in PEM format)"

# Public key in PEM format
cp "$CITIZEN_ID.pub" "$CITIZEN_ID-public.pem"
echo "Created: $CITIZEN_ID-public.pem (Public key in PEM format)"