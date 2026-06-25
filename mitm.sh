#!/bin/bash

# MITM VM Setup - VMnet2 (192.168.0.0/24)
# Position between citizen1 (192.168.0.100) and centralized party (192.168.0.10)

# Configure MITM IP 
sudo ip addr add 192.168.0.50/24 dev eth0
sudo ip link set eth0 up

# Enable IP forwarding for packet manipulation
echo 1 | sudo tee /proc/sys/net/ipv4/ip_forward >/dev/null

# Install required tools if not present
if ! command -v ettercap &> /dev/null; then
    sudo apt update -qq
    sudo apt install -y ettercap-text-only dsniff wireshark-common
fi