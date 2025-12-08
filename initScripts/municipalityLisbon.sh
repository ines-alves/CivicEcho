#!/bin/bash

echo "Configuring Lisbon Municipality..."

if [[ $EUID -ne 0 ]]; then
   echo "Run as root: sudo $0"
   exit 1
fi

echo "Installing Java 21, Maven, and Telnet..."
apt update
apt install -y openjdk-21-jdk maven telnet

ifconfig eth0 192.168.1.1/24 up

ifconfig eth1 192.168.2.1/24 up

ip route add default via 192.168.1.254

systemctl restart NetworkManager
systemctl enable NetworkManager
systemctl start NetworkManager

cat > /etc/network/interfaces << EOF
source /etc/network/interfaces.d/*

auto lo eth0 eth1
iface lo inet loopback

# Interface to VM2/Citizens
iface eth0 inet static
        address 192.168.1.1
        netmask 255.255.255.0
        gateway 192.168.1.254

# Interface to Database
iface eth1 inet static
        address 192.168.2.1
        netmask 255.255.255.0
EOF

echo "Lisbon Municipality Configuration:"
echo "eth0: 192.168.1.1 (to Centralized Party)"
echo "eth1: 192.168.2.1 (to Database)"
echo "Gateway: 192.168.1.254 "
echo "Test: ping 192.168.1.254 and ping 192.168.2.100"