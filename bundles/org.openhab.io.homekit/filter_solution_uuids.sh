#!/bin/bash

# Input and output files
INPUT_FILE="ts_uuids.txt"
OUTPUT_FILE="output/non_solution_uuids.txt"
CLASSNAMES="classnames.txt"

# Create output directory if it doesn't exist
mkdir -p output

# List of UUIDs that are part of the core HomeKit solution
# These are the standard HomeKit service UUIDs
CORE_UUIDS=(
    "0000003E-0000-1000-8000-0026BB765291" # AccessoryInformation
    "00000040-0000-1000-8000-0026BB765291" # Fan
    "00000041-0000-1000-8000-0026BB765291" # GarageDoorOpener
    "00000043-0000-1000-8000-0026BB765291" # Lightbulb
    "00000044-0000-1000-8000-0026BB765291" # LockManagement
    "00000045-0000-1000-8000-0026BB765291" # LockMechanism
    "00000047-0000-1000-8000-0026BB765291" # Outlet
    "00000049-0000-1000-8000-0026BB765291" # Switch
    "0000004A-0000-1000-8000-0026BB765291" # Thermostat
    "00000055-0000-1000-8000-0026BB765291" # Pairing
    "0000007E-0000-1000-8000-0026BB765291" # SecuritySystem
    "0000007F-0000-1000-8000-0026BB765291" # CarbonMonoxideSensor
    "00000080-0000-1000-8000-0026BB765291" # ContactSensor
    "00000081-0000-1000-8000-0026BB765291" # Door
    "00000082-0000-1000-8000-0026BB765291" # HumiditySensor
    "00000083-0000-1000-8000-0026BB765291" # LeakSensor
    "00000084-0000-1000-8000-0026BB765291" # LightSensor
    "00000085-0000-1000-8000-0026BB765291" # MotionSensor
    "00000086-0000-1000-8000-0026BB765291" # OccupancySensor
    "00000087-0000-1000-8000-0026BB765291" # SmokeSensor
    "00000088-0000-1000-8000-0026BB765291" # StatefulProgrammableSwitch
    "00000089-0000-1000-8000-0026BB765291" # StatelessProgrammableSwitch
    "0000008A-0000-1000-8000-0026BB765291" # TemperatureSensor
    "0000008B-0000-1000-8000-0026BB765291" # Window
    "0000008C-0000-1000-8000-0026BB765291" # WindowCovering
    "0000008D-0000-1000-8000-0026BB765291" # AirQualitySensor
    "00000096-0000-1000-8000-0026BB765291" # Battery
    "00000097-0000-1000-8000-0026BB765291" # CarbonDioxideSensor
    "000000A2-0000-1000-8000-0026BB765291" # ProtocolInformation
    "000000B7-0000-1000-8000-0026BB765291" # Fanv2
    "000000B9-0000-1000-8000-0026BB765291" # Slat
    "000000BA-0000-1000-8000-0026BB765291" # FilterMaintenance
    "000000BB-0000-1000-8000-0026BB765291" # AirPurifier
    "000000BC-0000-1000-8000-0026BB765291" # HeaterCooler
    "000000BD-0000-1000-8000-0026BB765291" # HumidifierDehumidifier
    "000000CC-0000-1000-8000-0026BB765291" # ServiceLabel
    "000000CF-0000-1000-8000-0026BB765291" # IrrigationSystem
    "000000D0-0000-1000-8000-0026BB765291" # Valve
    "000000D7-0000-1000-8000-0026BB765291" # Faucet
    "000000D8-0000-1000-8000-0026BB765291" # Television
    "000000D9-0000-1000-8000-0026BB765291" # InputSource
    "000000DA-0000-1000-8000-0026BB765291" # AccessControl
    "00000110-0000-1000-8000-0026BB765291" # CameraRTPStreamManagement
    "00000111-0000-1000-8000-0026BB765291" # CameraControl
    "00000112-0000-1000-8000-0026BB765291" # Microphone
    "00000113-0000-1000-8000-0026BB765291" # Speaker
    "00000121-0000-1000-8000-0026BB765291" # Doorbell
    "00000125-0000-1000-8000-0026BB765291" # TargetControl
    "00000127-0000-1000-8000-0026BB765291" # AudioStreamManagement
    "00000129-0000-1000-8000-0026BB765291" # DataStreamTransportManagement
    "00000133-0000-1000-8000-0026BB765291" # Siri
)

# Function to check if a UUID is in the core list
is_core_uuid() {
    local uuid=$1
    for core_uuid in "${CORE_UUIDS[@]}"; do
        if [ "$uuid" = "$core_uuid" ]; then
            return 0
        fi
    done
    return 1
}

# Process the input file
echo "Finding UUIDs not part of the core solution..."
while IFS= read -r line; do
    uuid=$(echo "$line" | awk '{print $2}')
    if ! is_core_uuid "$uuid"; then
        echo "$line" >> "$OUTPUT_FILE"
    fi
done < "$INPUT_FILE"

echo "Non-core UUIDs have been saved to $OUTPUT_FILE" 