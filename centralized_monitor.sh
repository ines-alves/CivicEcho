#!/bin/bash

sudo wireshark -i eth0 -k -f "host 192.168.0.100" &
WIRESHARK_PID=$!

read -p "Monitoring traffic from citizen1. Press Enter to stop..."

sudo kill $WIRESHARK_PID 2>/dev/null