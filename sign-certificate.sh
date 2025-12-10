#!/bin/bash

set -e  


if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <csr_file> <output_file>"
    exit 1
fi

CSR_FILE="$1"
OUTPUT_FILE="$2"

CA_CERT="certificates/autheticationServer/ca.crt"
CA_KEY="certificates/autheticationServer/ca.key"
CA_SRL="certificates/autheticationServer/ca.srl"
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

echo "Signing certificate..."
echo "CSR: $CSR_FILE"
echo "Output: $OUTPUT_FILE"

openssl x509 -req -days $VALID_DAYS -in "$CSR_FILE" \
    -CA "$CA_CERT" \
    -CAkey "$CA_KEY" \
    -CAserial "$CA_SRL" \
    -out "$OUTPUT_FILE"

echo "Certificate signed successfully: $OUTPUT_FILE"
