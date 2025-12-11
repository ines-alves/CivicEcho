#!/bin/bash

set -e  


CSR_FILE="$1"
OUTPUT_CERT_FILE="$2"

CA_CERT="../certificates/authentication-server/ca.crt"
CA_KEY="../certificates/authentication-server/ca.key"
CA_SRL="../certificates/authentication-server/ca.srl"
VALID_DAYS=365  

if [ ! -f "$CA_CERT" ]; then
    echo "ERROR: CA certificate not found: $CA_CERT"
    exit 1
fi

if [ ! -f "$CA_KEY" ]; then
    echo "ERROR: CA private key not found: $CA_KEY"
    exit 1
fi

if [ ! -f "$CSR_FILE" ]; then
    echo "ERROR: CSR file not found: $CSR_FILE"
    exit 1
fi



echo "=== Signing Certificate ==="
echo "CSR: $CSR_FILE"
echo "Output Certificate: $OUTPUT_CERT_FILE"

# Sign the certificate
echo "Signing certificate..."
openssl x509 -req -days $VALID_DAYS -in "$CSR_FILE" \
    -CA "$CA_CERT" \
    -CAkey "$CA_KEY" \
    -CAserial "$CA_SRL" \
    -out "$OUTPUT_CERT_FILE"

echo "Certificate signed successfully: $OUTPUT_CERT_FILE"

# Create PEM file (for trust stores)
# Extract base name for PEM file
PEM_FILE="${OUTPUT_CERT_FILE%.*}.pem"
echo "Creating PEM file..."
openssl x509 -in "$OUTPUT_CERT_FILE" -out "$PEM_FILE"
echo "PEM file created: $PEM_FILE"

# Generate base name for trust store
BASE_NAME=$(basename "$OUTPUT_CERT_FILE" .crt)
echo ""
echo "To create a trust store with this certificate:"
echo "  keytool -import -trustcacerts -file $PEM_FILE \\"
echo "    -alias \"$BASE_NAME\" \\"
echo "    -keystore ${BASE_NAME}-truststore.jks \\"
echo "    -storepass changeme -noprompt"