#!/bin/bash

echo "Configuring Database Server..."

if [[ $EUID -ne 0 ]]; then
   echo "Run as root: sudo $0"
   exit 1
fi

echo "Installing Java 21, Maven, and Telnet..."
apt update
apt install -y openjdk-21-jdk maven telnet

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