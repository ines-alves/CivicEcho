#!/bin/bash

sudo wireshark -i eth0 -k -f "tcp port 8090" &
WIRESHARK_PID=$!

read -p "Wireshark monitoring active. Press Enter to stop..."

sudo kill $WIRESHARK_PID 2>/dev/null