# HomeKit Integration: Operating Modes

This document describes the different operating modes of the OpenHAB HomeKit integration, including configuration approaches and advanced scenarios.

---

## Table of Contents

1. [Overview](#overview)
2. [Operating Modes](#operating-modes)
    - [1. HomeKit Controller Mode (Remote Accessories)](#1-homekit-controller-mode-remote-accessories)
        - [A. Item-based Configuration](#a-item-based-configuration)
        - [B. Thing-based Configuration](#b-thing-based-configuration)
    - [2. HomeKit Client Mode (Expose openHAB to HomeKit)](#2-homekit-client-mode-expose-openhab-to-homekit)
        - [A. YAML Configuration](#a-yaml-configuration)
        - [B. Thing-based Configuration](#b-thing-based-configuration-1)
    - [3. Re-Exposing Remote Accessories](#3-re-exposing-remote-accessories)
3. [Configuration Examples](#configuration-examples)
4. [FAQ](#faq)
5. [References](#references)

---

## Overview

The OpenHAB HomeKit integration supports multiple operating modes, allowing OpenHAB to act as a HomeKit **controller** (for remote accessories), a HomeKit **client** (exposing openHAB items/things to HomeKit), or a **bridge** (re-exposing remote accessories locally). This flexibility enables a wide range of smart home scenarios.

---

## Operating Modes

### 1. HomeKit Controller Mode (Remote Accessories)

In this mode, OpenHAB acts as a **HomeKit controller** (like an iPhone or iPad), discovering, pairing with, and controlling remote HomeKit accessories (e.g., lights, sensors, thermostats).

#### Key Features
- Discovers and manages remote HomeKit accessories.
- Integrates remote HomeKit devices into OpenHAB as Things and/or Items.
- Supports two configuration approaches:
    - **A. Item-based Configuration**
    - **B. Thing-based Configuration**

#### A. Item-based Configuration

- Accessories are mapped directly to OpenHAB Items.
- Minimal configuration, suitable for simple use cases.
- Example:  
  ```ini
  Switch LivingRoomLight "Living Room Light" { homekit="accessory:lightbulb" }
  ```

#### B. Thing-based Configuration

- Accessories are represented as Things, with Channels mapped to Items.
- Allows for advanced configuration, grouping, and metadata.
- Example:  
  ```ini
  Bridge homekit:controller:mybridge [ ... ] {
    Thing lightbulb livingroom [ accessoryId="..." ]
  }
  ```

---

### 2. HomeKit Client Mode (Expose openHAB to HomeKit)

In this mode, OpenHAB acts as a **HomeKit client** (accessory server), exposing its Items or Things to HomeKit controllers (e.g., iPhone Home app).

#### Key Features
- openHAB Items/Things appear as HomeKit accessories to Apple devices.
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

- openHAB Things are annotated or configured to be exposed to HomeKit.
- Example:
  ```ini
  Thing lightbulb livingroom [ homekit_expose="true" ]
  ```

---

### 3. Re-Exposing Remote Accessories

This advanced scenario allows OpenHAB to **re-expose remote HomeKit accessories** (discovered and controlled in controller mode) as local HomeKit accessories via its own accessory server.

#### Key Features
- Acts as a HomeKit-to-HomeKit bridge.
- Useful for integrating remote accessories into Apple Home app via openHAB.
- Configuration can be managed via YAML or Thing-based approaches, referencing remote accessories.

#### Example Workflow
1. Discover and pair with remote accessory (controller mode).
2. Map remote accessory to a local Item or Thing.
3. Configure the local accessory server to expose this Item/Thing back to HomeKit.

---

## Configuration Examples

### Controller Mode: Item-based

```ini
Switch KitchenLight "Kitchen Light" { homekit="accessory:lightbulb" }
```

### Controller Mode: Thing-based

```ini
Bridge homekit:controller:main [ ... ] {
  Thing lightbulb kitchen [ accessoryId="..." ]
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
A: Yes, you can configure openHAB to act as both a HomeKit controller and a HomeKit accessory server.

**Q: How do I migrate from item-based to thing-based configuration?**  
A: Update your configuration files to define Things and link their Channels to Items, then remove the `{ homekit=... }` tags from Items.

**Q: Can I re-expose non-HomeKit devices to HomeKit?**  
A: Yes, any openHAB Item or Thing can be exposed as a HomeKit accessory in client mode.

---

## References

- [openHAB HomeKit Add-on Documentation](https://www.openhab.org/addons/integrations/homekit/)
- [Apple HomeKit HomekitAccessory Protocol Specification](https://developer.apple.com/homekit/)
- [openHAB Thing and Item Concepts](https://www.openhab.org/docs/concepts/things.html) 