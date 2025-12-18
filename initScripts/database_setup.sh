#!/bin/bash

if [[ $EUID -ne 0 ]]; then
   echo "Run as root: sudo $0"
   exit 1
fi

apt update
apt install -y telnet

ifconfig eth0 192.168.2.100/24 up

iptables -F

/sbin/iptables -P INPUT DROP
/sbin/iptables -P FORWARD DROP
/sbin/iptables -P OUTPUT DROP

/sbin/iptables -A INPUT -i lo -j ACCEPT
/sbin/iptables -A OUTPUT -o lo -j ACCEPT

/sbin/iptables -A INPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT # allow incoming traffic for established connections
/sbin/iptables -A OUTPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT # allow outgoing traffic for established connections

# ========== BLOCKING RULES ==========
/sbin/iptables -A INPUT -p tcp --dport 22 -j DROP      # SSH
/sbin/iptables -A OUTPUT -p tcp --dport 22 -j DROP     # SSH
/sbin/iptables -A INPUT -p icmp -j DROP                # ICMP
/sbin/iptables -A OUTPUT -p icmp -j DROP               # ICMP
/sbin/iptables -A INPUT -p tcp --dport 23 -j DROP      # Telnet

# ========== DATABASE APPLICATION RULES ==========
/sbin/iptables -A INPUT -i eth0 -p tcp --dport 5432 -s 192.168.2.1 -j ACCEPT # allow incoming traffic on eth0 only from Lisbon and Porto municipalities
/sbin/iptables -A INPUT -i eth0 -p tcp --dport 5432 -s 192.168.2.2 -j ACCEPT
/sbin/iptables -A INPUT -s 192.168.1.254 -j DROP # block incoming traffic from centralized party

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

apt install -y postgresql postgresql-contrib

sudo cp "../certificates/db-server/db.crt" "/var/lib/postgresql/18/main/server.crt"
sudo cp "../certificates/db-server/db.key" "/var/lib/postgresql/18/main/server.key"
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

echo "Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
    if sudo -u postgres pg_isready -p 5432 > /dev/null 2>&1; then
        echo "PostgreSQL is ready!"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

echo "Creating database and user..."
sudo -u postgres psql -p 5432 << 'EOF'
SELECT 'CREATE DATABASE municipality'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'municipality')\gexec

DO
$do$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = 'municipality_user') THEN
      CREATE USER municipality_user WITH PASSWORD 'municipality_pass';
   END IF;
END
$do$;

GRANT CONNECT ON DATABASE municipality TO municipality_user;
REVOKE ALL ON SCHEMA public FROM municipality_user;
GRANT USAGE ON SCHEMA public TO municipality_user;
EOF

echo "Creating reports table..."
sudo -u postgres psql -p 5432 -d municipality << 'EOF'
DROP TABLE IF EXISTS reports CASCADE;

CREATE TABLE reports (
    report_id VARCHAR(255) PRIMARY KEY,
    location VARCHAR(255),
    report_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

REVOKE ALL PRIVILEGES ON TABLE reports FROM municipality_user;
GRANT SELECT, INSERT ON TABLE reports TO municipality_user;
EOF

echo "Verifying database setup..."
sudo -u postgres psql -p 5432 -c "\du" | grep municipality_user
sudo -u postgres psql -p 5432 -d municipality -c "\dt" | grep reports

sudo systemctl enable postgresql