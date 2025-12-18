#!/bin/bash

echo "Configuring Authentication Server..."

if [[ $EUID -ne 0 ]]; then
   echo "Run as root: sudo $0"
   exit 1
fi

echo "Installing Java 21, Maven, and Telnet..."
apt update
apt install -y openjdk-21-jdk maven telnet

ifconfig eth0 192.168.0.20/24 up

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

# ========== AUTH SERVER APPLICATION RULES ==========
/sbin/iptables -A INPUT -i eth0 -p tcp --dport 8090 -s 192.168.0.0/24 -j ACCEPT # allow incoming traffic on eth0 only from citizen subnet

# ========== INTERNET ACCESS ==========
/sbin/iptables -A OUTPUT -p tcp --dport 80 -j ACCEPT      # HTTP
/sbin/iptables -A OUTPUT -p tcp --dport 443 -j ACCEPT     # HTTPS
/sbin/iptables -A OUTPUT -p tcp --dport 53 -j ACCEPT      # DNS TCP
/sbin/iptables -A OUTPUT -p udp --dport 53 -j ACCEPT      # DNS UDP
/sbin/iptables -A OUTPUT -p udp --dport 123 -j ACCEPT     # NTP

systemctl restart NetworkManager
systemctl enable NetworkManager
systemctl start NetworkManager

cat > /etc/network/interfaces << EOF
source /etc/network/interfaces.d/*

auto lo eth0 eth1
iface lo inet loopback

iface eth0 inet static
        address 192.168.0.20
        netmask 255.255.255.0

EOF


echo "Configuration:"
echo "eth0: 192.168.0.20 "
echo "eth2: Internet"