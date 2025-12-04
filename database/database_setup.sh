#!/bin/bash

# Simple Database Setup Script
# Install PostgreSQL, create DB, create schema, start server

echo "=== Database Setup ==="

# Install PostgreSQL if not installed
if ! command -v psql &> /dev/null; then
    echo "Installing PostgreSQL..."
    sudo apt update
    sudo apt install -y postgresql postgresql-contrib
fi

# Start PostgreSQL
echo "Starting PostgreSQL..."
sudo systemctl enable postgresql
sudo systemctl start postgresql
sleep 2

# Create database and user
echo "Creating database..."
sudo -u postgres psql << EOF > /dev/null 2>&1
CREATE DATABASE IF NOT EXISTS municipality;
CREATE USER IF NOT EXISTS municipality_user WITH PASSWORD 'municipality_pass';
GRANT ALL PRIVILEGES ON DATABASE municipality TO municipality_user;
EOF

# Create schema
echo "Creating schema..."
sudo -u postgres psql -d municipality << EOF > /dev/null 2>&1
CREATE TABLE IF NOT EXISTS reports (
    report_id VARCHAR(255) PRIMARY KEY,
    location VARCHAR(255),
    report_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
GRANT ALL ON reports TO municipality_user;
GRANT ALL ON SCHEMA public TO municipality_user;
EOF

# Start Database Server
echo "Starting Database Server..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
mvn clean compile exec:java -Dexec.mainClass="DatabaseServer"