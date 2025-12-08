#!/bin/bash

echo "Configuring Centralized Party..."

if [[ $EUID -ne 0 ]]; then
   echo "Run as root: sudo $0"
   exit 1
fi

echo "Installing Java 21, Maven, and Telnet..."
apt update
apt install -y openjdk-21-jdk maven telnet

ifconfig eth0 192.168.0.10/24 up
ifconfig eth1 192.168.1.254/24 up

sysctl net.ipv4.ip_forward=1

iptables -P FORWARD ACCEPT
iptables -F FORWARD

iptables -t nat -F
iptables -t nat -A POSTROUTING -o eth2 -j MASQUERADE

# Talvez retirar forwarding
echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf

cat > /etc/network/interfaces << EOF
source /etc/network/interfaces.d/*

auto lo eth0 eth1
iface lo inet loopback

iface eth0 inet static
        address 192.168.0.10
        netmask 255.255.255.0

iface eth1 inet static
        address 192.168.1.254
        netmask 255.255.255.0
EOF

systemctl restart NetworkManager
systemctl enable NetworkManager
systemctl start NetworkManager

echo "Gateway Configuration:"
echo "eth0: 192.168.0.10 "
echo "eth1: 192.168.1.254 "
echo "eth2: Internet"
echo "IP forwarding enabled"