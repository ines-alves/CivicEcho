#!/bin/bash
cd utils
mvn clean install
cd ../citizenApp
mvn clean install

inputs=(
    # login
    "1" 1
    "citizen1" 2

    # -------- Report 1 --------
    "1" 0.5
    "Noise" 0.5
    "Lisbon" 0.5
    "387223" 0.5
    "91393" 0.5
    "Loud music at night" 0.5
    "s" 1

    # -------- Report 2 --------
    "1" 0.5
    "Garbage" 0.5
    "Lisbon" 0.5
    "411579" 0.5
    "86291" 0.5
    "Overflowing trash bins" 0.5
    "s" 1

    # -------- Report 3 --------
    "1" 0.5
    "Parking" 0.5
    "Lisbon" 0.5
    "298144" 0.5
    "77421" 0.5
    "Cars blocking sidewalk" 0.5
    "s" 1

    # -------- Report 4 --------
    "1" 0.5
    "Lighting" 0.5
    "Lisbon" 0.5
    "556732" 0.5
    "99124" 0.5
    "Street lights not working" 0.5
    "s" 1

    # -------- Report 5 --------
    "1" 0.5
    "Road Damage" 0.5
    "Lisbon" 0.5
    "774190" 0.5
    "66532" 0.5
    "Large potholes on road" 0.5
    "s" 1

    # -------- Report 6 --------
    "1" 0.5
    "Water Leak" 0.5
    "Lisbon" 0.5
    "889321" 0.5
    "44567" 0.5
    "Water leaking from pavement" 0.5
    "s" 1

    # -------- Report 7 --------
    "1" 0.5
    "Graffiti" 0.5
    "Lisbon" 0.5
    "663290" 0.5
    "33218" 0.5
    "Graffiti on public walls" 0.5
    "s" 1

    # -------- Report 8 --------
    "1" 0.5
    "Abandoned Vehicle" 0.5
    "Lisbon" 0.5
    "901244" 0.5
    "71882" 0.5
    "Car abandoned for weeks" 0.5
    "s" 1

    # -------- Report 9 --------
    "1" 0.5
    "Public Safety" 0.5
    "Lisbon" 0.5
    "482911" 0.5
    "50933" 0.5
    "Broken railing near stairs" 0.5
    "s" 1

    # -------- Report 10 --------
    "1" 0.5
    "Animals" 0.5
    "Lisbon" 0.5
    "775401" 0.5
    "88012" 0.5
    "Stray dogs near school" 0.5
    "s" 1

    # -------- Token request --------
    "1" 0.5
	"1" 1
    # -------- Keep exactly --------
    "2" 0.5
    "2" 2
    "0" 0
)


# Feed input with sleep between each
{
    for ((i=0; i<${#inputs[@]}; i+=2)); do
        echo "${inputs[i]}"
        sleep "${inputs[i+1]}"
    done
} | timeout 60s mvn exec:java -Dexec.args="citizen1"
