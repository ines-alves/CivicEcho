#!/bin/bash
cd utils
mvn clean install -q
cd ../citizenApp
mvn clean install -q

inputs=(
    # Quick login
    "1" 0.5
    "citizen1" 1

    # Create report with target data for tampering
    "1" 0.5
    "Noise" 0.5
    "Lisbon" 1
    "387223" 0.5
    "91393" 0.5
    "Loud music demonstration" 0.5
    "s" 1

    # Read reports to generate more traffic
    "2" 0.5
    "2" 0.5    # Select "All" reports

    # Exit after operations
    "0" 0
)

# Feed input with minimal delays for quick demo
{
    for ((i=0; i<${#inputs[@]}; i+=2)); do
        echo "${inputs[i]}"
        sleep "${inputs[i+1]}"
    done
} | timeout 30s mvn exec:java -Dexec.args="citizen1"