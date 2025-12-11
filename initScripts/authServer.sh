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


cat > /etc/network/interfaces << EOF
source /etc/network/interfaces.d/*

auto lo eth0 eth1
iface lo inet loopback

iface eth0 inet static
        address 192.168.0.20
        netmask 255.255.255.0

EOF

systemctl restart NetworkManager
systemctl enable NetworkManager
systemctl start NetworkManager

echo "Configuration:"
echo "eth0: 192.168.0.20 "
echo "eth2: Internet"