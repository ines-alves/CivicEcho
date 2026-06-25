# CivicEcho Security Demonstration

## Setup
```bash
# Start VMs: database, authServer, centralizedParty, citizen1, MITM
./generate-fixed-certificates.sh
```

## MITM Attack Demo
```bash
# MITM VM (192.168.0.50):
./mitm.sh                     # Setup network
./mitm_monitor.sh             # Start Wireshark
./mitm_attack.sh              # ARP poison + corrupt 10% packets

# CentralizedParty VM (192.168.0.10):
./centralized_monitor.sh      # Monitor incoming traffic

# Citizen1 VM (192.168.0.100):
./security_demo.sh            # Generate target traffic
```

## Results
- **✓ MITM position achieved**: All traffic intercepted
- **✓ Packet corruption active**: 10% packets corrupted
- **✓ TLS protection works**: Data arrives intact
- **✓ TCP retransmissions**: Corrupted packets retransmitted
- **✓ Zero plaintext visible**: Encryption prevents data access

## Security Proof
Even with perfect network control and active packet corruption, TLS+GCM protects data integrity and confidentiality.