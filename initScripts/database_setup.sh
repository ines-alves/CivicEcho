#!/bin/bash

echo "Configuring Database Server..."

if [[ $EUID -ne 0 ]]; then
   echo "Run as root: sudo $0"
   exit 1
fi

echo "Installing Telnet..."
apt update
apt install -y telnet

ifconfig eth0 192.168.2.100/24 up

systemctl restart NetworkManager
systemctl enable NetworkManager
systemctl start NetworkManager

cat > /etc/network/interfaces << EOF
source /etc/network/interfaces.d/*

auto lo eth0
iface lo inet loopback

iface eth0 inet static
        address 192.168.2.100
        netmask 255.255.255.0
        # NO GATEWAY - only local subnet communication
EOF

echo "Database Server Configuration:"
echo "IP: 192.168.2.100"
echo "Connected to: Municipality on subnet 192.168.2.0/24"

# Simple Database Setup Script
# Install PostgreSQL, create DB, create schema, configure SSL, and start server

echo "=== Database Setup ==="

# Install PostgreSQL if not installed
if ! command -v psql &> /dev/null; then
    echo "Installing PostgreSQL..."
    apt update
    apt install -y postgresql postgresql-contrib
fi

# Start PostgreSQL
echo "Starting PostgreSQL..."
systemctl enable postgresql
systemctl start postgresql
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

# Configure PostgreSQL for SSL
echo "Configuring PostgreSQL for SSL..."
PG_CONF=$(sudo -u postgres psql -t -P format=unaligned -c 'show config_file;')
PG_HBA=$(sudo -u postgres psql -t -P format=unaligned -c 'show hba_file;')
CERT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/../certificates" && pwd)

sed -i "s/#ssl = off/ssl = on/g" $PG_CONF
sed -i "s|#ssl_cert_file = 'server.crt'|ssl_cert_file = '$CERT_DIR/database/db.crt'|g" $PG_CONF
sed -i "s|#ssl_key_file = 'server.key'|ssl_key_file = '$CERT_DIR/database/db.key'|g" $PG_CONF
sed -i "s|#ssl_ca_file = ''|ssl_ca_file = '$CERT_DIR/centralized-party/cp.crt'|g" $PG_CONF

echo "hostssl all all 0.0.0.0/0 md5" | tee -a $PG_HBA

# Restart PostgreSQL
echo "Restarting PostgreSQL..."
systemctl restart postgresql