#!/bin/bash

echo "Configuring Porto Municipality..."

if [[ $EUID -ne 0 ]]; then
   echo "Run as root: sudo $0"
   exit 1
fi

echo "Installing Java 21, Maven, and Telnet..."
apt update
apt install -y openjdk-21-jdk maven telnet

ifconfig eth0 192.168.1.2/24 up

ifconfig eth1 192.168.2.2/24 up

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

# ========== MUNICIPALITY APPLICATION RULES ==========
/sbin/iptables -A INPUT -i eth0 -p tcp --dport 8091 -s 192.168.1.254 -j ACCEPT  # allow incoming traffic on eth0 only from centralized party
/sbin/iptables -A OUTPUT -o eth1 -p tcp --dport 5432 -d 192.168.2.100 -j ACCEPT # allow outgoing traffic on eth1 only to database

# ========== INTERNET ACCESS ==========
/sbin/iptables -A OUTPUT -p tcp --dport 80 -j ACCEPT      # HTTP
/sbin/iptables -A OUTPUT -p tcp --dport 443 -j ACCEPT     # HTTPS
/sbin/iptables -A OUTPUT -p tcp --dport 53 -j ACCEPT      # DNS TCP
/sbin/iptables -A OUTPUT -p udp --dport 53 -j ACCEPT      # DNS UDP
/sbin/iptables -A OUTPUT -p udp --dport 123 -j ACCEPT     # NTP

iptables-save > /etc/iptables/rules.v4

echo "Saved firewall rules"

systemctl restart NetworkManager
systemctl enable NetworkManager
systemctl start NetworkManager

cat > /etc/network/interfaces << EOF
source /etc/network/interfaces.d/*

auto lo eth0 eth1
iface lo inet loopback

# Interface to VM2/Citizens
iface eth0 inet static
        address 192.168.1.2
        netmask 255.255.255.0
        gateway 192.168.1.254

# Interface to Database
iface eth1 inet static
        address 192.168.2.2
        netmask 255.255.255.0
EOF


echo "Porto Municipality Configuration:"
echo "eth0: 192.168.1.2 (to Centralized Party)"
echo "eth1: 192.168.2.2 (to Database)"

cd ../utils/
mvn clean install

cd ../municipalityApp/
mvn clean install
mvn compile

ifconfig eth2 down

mvn exec:java -Dexec.args="porto"