#!/bin/bash

echo "Configuring Citizen1..."

if [[ $EUID -ne 0 ]]; then
   echo "Run as root: sudo $0"
   exit 1
fi

echo "Installing Java 21, Maven, and Telnet..."
apt update
apt install -y openjdk-21-jdk maven telnet

ifconfig eth0 192.168.0.100/24 up

ip route add default via 192.168.0.10

systemctl restart NetworkManager
systemctl enable NetworkManager
systemctl start NetworkManager

cat > /etc/network/interfaces << EOF
source /etc/network/interfaces.d/*

auto lo eth0
iface lo inet loopback

iface eth0 inet static
        address 192.168.0.100
        netmask 255.255.255.0
        gateway 192.168.0.10
EOF

echo "Citizen1 Configuration:"
echo "IP: 192.168.0.100"
echo "Gateway: 192.168.0.10"
echo "Test with: ping 192.168.0.10"