# Homekit Integration: Operating Modes

This document describes the different operating modes of the OpenHAB Homekit integration, including configuration approaches and advanced scenarios.

---

## Table of Contents

1. [Overview](#overview)
2. [Operating Modes](#operating-modes)
    - [1. Homekit Controller Mode (Remote Accessories)](#1-homekit-controller-mode-remote-accessories)
        - [A. Thing-based Configuration](#a-thing-based-configuration)
    - [2. Homekit Client Mode (Expose openHAB to Homekit)](#2-homekit-client-mode-expose-openhab-to-homekit)
        - [A. YAML Configuration](#a-yaml-configuration)
        - [B. Thing-based Configuration](#b-thing-based-configuration-1)
    - [3. Re-Exposing Remote Accessories](#3-re-exposing-remote-accessories)
3. [Configuration Examples](#configuration-examples)
4. [FAQ](#faq)
5. [References](#references)

---

## Overview

The OpenHAB Homekit integration supports multiple operating modes, allowing OpenHAB to act as a Homekit **controller** (for remote accessories), a Homekit **client** (exposing openHAB items/things to Homekit), or a **bridge** (re-exposing remote accessories locally). This flexibility enables a wide range of smart home scenarios.

---

## Operating Modes

### 1. Homekit Controller Mode (Remote Accessories)

In this mode, OpenHAB acts as a **Homekit controller**, discovering and controlling remote Homekit accessories. The integration uses two main Thing types:

#### Thing Types

1. **openhab:accessory**
   - Represents a complete Homekit accessory
   - Uses channel groups to define services (e.g., `lightbulb.1`, `switch.2`, `thermostat.1`)
   - Uses channels within groups to define characteristics
   - Example:
   ```ini
   Thing homekit:accessory:my_accessory "Living Room Light" [ pairingId="1234", accessoryId="1" ] {
       Channels:
           Type switch.1 : main "Main Light" {
               Type switch : power "Power"
           }
   }
   ```

   The channel UUID structure for accessories follows the pattern: `homekit:accessory:[unique id]:[service tag].[service id]#[channel tag]`
   - `[unique id]`: The unique identifier of the accessory (e.g., "my_accessory")
   - `[service tag].[service id]`: The service type (from `@HomekitServiceType` annotation) and instance ID (e.g., "switch.1")
   - `[channel tag]`: The characteristic type (from `@HomekitCharacteristicType` annotation) (e.g., "on", "brightness")
   
   For example, in the configuration above:
   - The accessory has unique ID "my_accessory"
   - It has a switch service with instance ID 1 (using the "switch" tag from the service class annotation)
   - The power characteristic has tag "on" (from the characteristic class annotation)
   - Therefore, the full channel UUID would be: `homekit:accessory:my_accessory:switch.1#on`
   
   Configuration parameters are pairingId, accessoryId

2. **openhab:service**
   - Represents a single Homekit service
   - Uses channels to define characteristics
   - The channel types match the characteristic tags from the `@HomekitCharacteristicType` annotations
   - Example:
   ```ini
   Thing homekit:service:my_switch "Living Room Light Service" [ pairingId="1234", accessoryId="1", serviceId="2" ] {
       Channels:
           Type switch : power "Power"
   }
   ```

   The channel UUID structure for services follows the pattern: `homekit:[service tag]:[unique id]:[channel tag]`
   - `[service tag]`: The service type from the `@HomekitServiceType` annotation (e.g., "switch", "lightbulb")
   - `[unique id]`: The unique identifier of the service (e.g., "my_switch")
   - `[channel tag]`: The characteristic type from the `@HomekitCharacteristicType` annotation (e.g., "on", "brightness")
   
   For example, in the configuration above:
   - The service has type "switch" (from the service class annotation)
   - The service has unique ID "my_switch"
   - The power characteristic has tag "on" (from the characteristic class annotation)
   - Therefore, the full channel UUID would be: `homekit:switch:my_switch:on`
   
   Configuration parameters are pairingId, accessoryId, serviceId

#### Service Tags
- Service tags are defined by the `@HomekitServiceType` annotation in the service classes
- Common service tags include:
  - `lightbulb` - for light services
  - `switch` - for switch services
  - `thermostat` - for thermostat services
  - `fan` - for fan services
  - `garage` - for garage door services
- These tags are used in channel group IDs for accessories and in service thing IDs

#### Type Mapping
- Service types in channel groups correspond to the `@HomekitServiceType` annotation tags in the service classes
- Characteristic types in channels correspond to the `@HomekitCharacteristicType` annotation tags in the characteristic classes
- This ensures type safety and proper mapping between openHAB and Homekit protocols

#### Configuration Structure

1. **Accessory Configuration**
   - Each accessory is represented as a Thing of type `openhab:accessory`
   - Services are defined as channel groups using service tags with instance IDs (e.g., `lightbulb.1`, `switch.2`)
   - Characteristics are defined as channels within those groups
   - Configuration example:
   ```ini
   Thing homekit:accessory:my_accessory "Living Room Light" [ pairingId="1234", accessoryId="1" ] {
       Channels:
           Type lightbulb.1 : main "Main Light" {
               Type switch : power "Power"
               Type dimmer : brightness "Brightness"
           }
           Type switch.2 : outlet "Power Outlet" {
               Type switch : power "Power"
           }
   }
   ```

2. **Service Configuration**
   - Each service is represented as a Thing of type `openhab:service`
   - Characteristics are defined directly as channels
   - Configuration example:
   ```ini
   Thing homekit:service:my_switch "Living Room Light Service" [ pairingId="1234", accessoryId="1", serviceId="2" ] {
       Channels:
           Type switch : power "Power"
   }
   ```

#### Channel Configuration Parameters
- `aid`: Accessory ID - identifies the accessory this characteristic belongs to
- `iid`: Instance ID - unique identifier for the characteristic within the accessory
- These parameters are used to map channels to the correct Homekit characteristics

#### Thing Configuration Parameters
- `pairingId`: Unique identifier for the Homekit pairing
- `accessoryId`: Identifier for the accessory
- `serviceId`: (For service type only) Identifier for the service

#### Key Features
- Automatic discovery of remote Homekit accessories
- Support for all Homekit service types
- Flexible configuration through Things and Channels
- Real-time state synchronization
- Event-based updates

#### Implementation Details
- The `HomekitAccessoryThingHandler` manages accessory Things
- The `HomekitServiceThingHandler` manages service Things
- Channel types are provided by `HomekitChannelTypeProvider`
- Thing types are provided by `HomekitThingTypeProvider`
- Services and characteristics are created using respective factories

---

### 2. Homekit Client Mode (Expose openHAB to Homekit)

In this mode, OpenHAB acts as a **Homekit client** (accessory server), exposing its Items or Things to Homekit controllers (e.g., iPhone Home app).

#### Key Features
- openHAB Items/Things appear as Homekit accessories to Apple devices.
- Supports configuration via YAML files or Thing definitions.

#### A. YAML Configuration

- Accessories and services are defined in YAML files (e.g., `homekit.yaml`).
- Flexible, supports advanced mapping and metadata.
- Example:
  ```yaml
  accessories:
    - name: Living Room Light
      type: lightbulb
      item: LivingRoomLight
      services:
        - type: Lightbulb
          characteristics:
            - type: On
              item: LivingRoomLight
  ```

#### B. Thing-based Configuration

- openHAB Things are annotated or configured to be exposed to Homekit.
- Example:
  ```ini
  Thing lightbulb livingroom [ homekit_expose="true" ]
  ```

---

### 3. Re-Exposing Remote Accessories

This advanced scenario allows OpenHAB to **re-expose remote Homekit accessories** (discovered and controlled in controller mode) as local Homekit accessories via its own accessory server.

#### Key Features
- Acts as a Homekit-to-Homekit bridge.
- Useful for integrating remote accessories into Apple Home app via openHAB.
- Configuration can be managed via YAML or Thing-based approaches, referencing remote accessories.

#### Example Workflow
1. Discover and pair with remote accessory (controller mode).
2. Map remote accessory to a local Item or Thing.
3. Configure the local accessory server to expose this Item/Thing back to Homekit.

---

## Configuration Examples

### Controller Mode: Thing-based

```ini
Thing homekit:accessory:livingroom "Living Room Light" [ pairingId="1234", accessoryId="1" ] {
    Channels:
        Type lightbulb.1 : main "Main Light" {
            Type switch : power "Power"
            Type dimmer : brightness "Brightness"
        }
        Type switch.2 : outlet "Power Outlet" {
            Type switch : power "Power"
        }
}
```

### Multiple Switch Services Example

```ini
Thing homekit:accessory:powerstrip "Power Strip" [ pairingId="1234", accessoryId="1" ] {
    Channels:
        Type switch.1 : outlet1 "Outlet 1" {
            Type switch : power "Power"
        }
        Type switch.2 : outlet2 "Outlet 2" {
            Type switch : power "Power"
        }
        Type switch.3 : outlet3 "Outlet 3" {
            Type switch : power "Power"
        }
        Type switch.4 : outlet4 "Outlet 4" {
            Type switch : power "Power"
        }
}
```

### Client Mode: YAML

```yaml
accessories:
  - name: Kitchen Light
    type: lightbulb
    item: KitchenLight
```

### Client Mode: Thing-based

```ini
Thing lightbulb kitchen [ homekit_expose="true" ]
```

### Re-Exposing Remote Accessories

```yaml
accessories:
  - name: Remote Kitchen Light
    type: lightbulb
    item: RemoteKitchenLight
    source: remote
```

---

## FAQ

**Q: Can I use both controller and client modes at the same time?**  
A: Yes, you can configure openHAB to act as both a Homekit controller and a Homekit accessory server.

**Q: How do I migrate from item-based to thing-based configuration?**  
A: Update your configuration files to define Things and link their Channels to Items, then remove the `{ homekit=... }` tags from Items.

**Q: Can I re-expose non-Homekit devices to Homekit?**  
A: Yes, any openHAB Item or Thing can be exposed as a Homekit accessory in client mode.

---

## References

- [openHAB Homekit Add-on Documentation](https://www.openhab.org/addons/integrations/homekit/)
- [Apple Homekit HomekitAccessory Protocol Specification](https://developer.apple.com/homekit/)
- [openHAB Thing and Item Concepts](https://www.openhab.org/docs/concepts/things.html) 