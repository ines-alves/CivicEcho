#!/bin/bash

if [[ $EUID -ne 0 ]]; then
   echo "Run as root: sudo $0"
   exit 1
fi

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

apt update
apt install -y postgresql postgresql-contrib

sudo cp "/mnt/hgfs/SIRS/A03-CivicEcho/certificates/database/db.crt" "/var/lib/postgresql/18/main/server.crt"
sudo cp "/mnt/hgfs/SIRS/A03-CivicEcho/certificates/database/db.key" "/var/lib/postgresql/18/main/server.key"
sudo chown postgres:postgres "/var/lib/postgresql/18/main/server.crt" "/var/lib/postgresql/18/main/server.key"
sudo chmod 600 "/var/lib/postgresql/18/main/server.key"
sudo chmod 644 "/var/lib/postgresql/18/main/server.crt"

sudo sed -i "s/^#\?ssl = .*/ssl = on/" "/etc/postgresql/18/main/postgresql.conf"
sudo sed -i "s|^#\?ssl_cert_file = .*|ssl_cert_file = 'server.crt'|" "/etc/postgresql/18/main/postgresql.conf"
sudo sed -i "s|^#\?ssl_key_file = .*|ssl_key_file = 'server.key'|" "/etc/postgresql/18/main/postgresql.conf"
sudo sed -i "s/^#\?listen_addresses = .*/listen_addresses = '*'/" "/etc/postgresql/18/main/postgresql.conf"
sudo sed -i "s/^#\?port = .*/port = 5432/" "/etc/postgresql/18/main/postgresql.conf"
sudo sed -i "s/^#\?ssl_min_protocol_version = .*/ssl_min_protocol_version = 'TLSv1.3'/" "/etc/postgresql/18/main/postgresql.conf"
sudo sed -i "s/^#\?ssl_max_protocol_version = .*/ssl_max_protocol_version = 'TLSv1.3'/" "/etc/postgresql/18/main/postgresql.conf"
sudo sed -i "s|^#\?ssl_ciphers = .*|ssl_ciphers = 'ECDHE+AESGCM'|" "/etc/postgresql/18/main/postgresql.conf"

grep -q "^hostssl municipality municipality_user 192.168.2.0/24 scram-sha-256" "/etc/postgresql/18/main/pg_hba.conf" || \
    echo "hostssl municipality municipality_user 192.168.2.0/24 scram-sha-256" | sudo tee -a "/etc/postgresql/18/main/pg_hba.conf" > /dev/null

sudo systemctl restart postgresql

for i in {1..30}; do
    if sudo -u postgres pg_isready -p 5432 > /dev/null 2>&1; then
        break
    fi
    sleep 1
done

sudo -u postgres psql -p 5432 << 'EOF' > /dev/null 2>&1
SELECT 'CREATE DATABASE municipality'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'municipality')\gexec

DO
$do$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = 'municipality_user') THEN
      CREATE USER municipality_user WITH PASSWORD 'municipality_pass' PASSWORD_ENCRYPTION 'scram-sha-256';
   END IF;
END
$do$;

GRANT ALL PRIVILEGES ON DATABASE municipality TO municipality_user;
EOF

sudo -u postgres psql -p 5432 -d municipality << 'EOF' > /dev/null 2>&1
CREATE TABLE IF NOT EXISTS reports (
    report_id VARCHAR(255) PRIMARY KEY,
    location VARCHAR(255),
    report_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
GRANT ALL ON reports TO municipality_user;
GRANT ALL ON SCHEMA public TO municipality_user;
EOF

sudo systemctl enable postgresql