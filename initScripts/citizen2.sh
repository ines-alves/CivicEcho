#!/bin/bash

echo "Configuring Citizen2..."

if [[ $EUID -ne 0 ]]; then
   echo "Run as root: sudo $0"
   exit 1
fi

echo "Installing Java 21, Maven, and Telnet..."
apt update
apt install -y openjdk-21-jdk maven telnet

ifconfig eth0 192.168.0.101/24 up

ip route add default via 192.168.0.10

systemctl restart NetworkManager
systemctl enable NetworkManager
systemctl start NetworkManager

cat > /etc/network/interfaces << EOF
source /etc/network/interfaces.d/*

auto lo eth0
iface lo inet loopback

iface eth0 inet static
        address 192.168.0.101
        netmask 255.255.255.0
EOF

echo "Citizen2 Configuration:"
echo "IP: 192.168.0.101"

cd ../utils/
mvn clean install

cd ../citizenApp/
mvn clean install
mvn compile exec:java -Dexec.args="citizen2"
