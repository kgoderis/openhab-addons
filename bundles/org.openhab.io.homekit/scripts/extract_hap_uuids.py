#!/usr/bin/env python3
import re
import json
import sys
from urllib.request import urlopen

def extract_characteristic_uuids(content):
    """Extract characteristic UUIDs from TypeScript definition file."""
    pattern = r'export class (\w+) extends Characteristic {\s+public static readonly UUID: string = "([^"]+)"'
    matches = re.finditer(pattern, content, re.MULTILINE)
    return {match.group(1): match.group(2) for match in matches}

def extract_service_uuids(content):
    """Extract service UUIDs from TypeScript definition file."""
    pattern = r'export class (\w+) extends Service {\s+public static readonly UUID: string = "([^"]+)"'
    matches = re.finditer(pattern, content, re.MULTILINE)
    return {match.group(1): match.group(2) for match in matches}

def download_file(url):
    """Download content from URL."""
    with urlopen(url) as response:
        return response.read().decode('utf-8')

def main():
    # URLs for the definition files
    char_url = "https://raw.githubusercontent.com/homebridge/HAP-NodeJS/60502a0afac9ab8d7e9c804e034dfdf0ce06ed57/src/lib/definitions/CharacteristicDefinitions.ts"
    svc_url = "https://raw.githubusercontent.com/homebridge/HAP-NodeJS/60502a0afac9ab8d7e9c804e034dfdf0ce06ed57/src/lib/definitions/ServiceDefinitions.ts"
    
    # Download and process characteristic definitions
    char_content = download_file(char_url)
    char_uuids = extract_characteristic_uuids(char_content)
    
    # Download and process service definitions
    svc_content = download_file(svc_url)
    svc_uuids = extract_service_uuids(svc_content)
    
    # Create a mapping of our class names to HAP-NodeJS class names
    class_mapping = {
        # Characteristics
        'HomekitAirQualityCharacteristic': 'AirQuality',
        'HomekitCurrentAirQualityCharacteristic': 'AirQuality',
        'HomekitChargingStateCharacteristic': 'ChargingState',
        'HomekitSecuritySystemAlarmLevelCharacteristic': 'SecuritySystemAlarmLevel',
        'HomekitCurrentHumidityCharacteristic': 'CurrentRelativeHumidity',
        'HomekitCurrentRelativeHumidityCharacteristic': 'CurrentRelativeHumidity',
        'HomekitPowerModeSelectionCharacteristic': 'PowerModeSelection',
        'HomekitAirPlayEnableCharacteristic': 'AirPlayEnable',
        'HomekitBinaryDataCharacteristic': 'Data',
        'HomekitInUseCharacteristic': 'InUse',
        
        # Services
        'HomekitHAPProtocolInformationService': 'HAPProtocolInformation',
        'HomekitProtocolInformationService': 'ProtocolInformation'
    }
    
    # Print the correct UUIDs for our duplicate classes
    print("=== Correct UUIDs from HAP-NodeJS ===\n")
    
    print("Characteristics:")
    for our_class, hap_class in class_mapping.items():
        if hap_class in char_uuids:
            print(f"{our_class}: {char_uuids[hap_class]}")
    
    print("\nServices:")
    for our_class, hap_class in class_mapping.items():
        if hap_class in svc_uuids:
            print(f"{our_class}: {svc_uuids[hap_class]}")

if __name__ == "__main__":
    main() 