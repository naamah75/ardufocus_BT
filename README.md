[![Donate Patreon](https://img.shields.io/badge/Donate-Patreon-blue.svg?style=for-the-badge)](https://www.patreon.com/join/jbrazio?)
[![Donate PayPal](https://img.shields.io/badge/Donate-Paypal-blue.svg?style=for-the-badge)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=D5XZ7QFLP8LXE)
[![Download beta](https://img.shields.io/github/release-pre/naamah75/ardufocus_BT.svg?style=for-the-badge)](https://github.com/naamah75/ardufocus_BT/releases)
[![GitHub stars](https://img.shields.io/github/stars/naamah75/ardufocus_BT.svg?style=for-the-badge)](https://github.com/naamah75/ardufocus_BT/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/naamah75/ardufocus_BT.svg?style=for-the-badge)](https://github.com/naamah75/ardufocus_BT/network)

**Ardufocus - The most accurate Open Source focus controller.**

Ardufocus is the only OSS/OSH controller supporting **two independent** focusing motors, **high resolution**
mode allowing **sub μm** movements by step, multiple acceleration profiles and it will never forget your
focuser position between restarts.

Ardufocus is a multi repository project:
- [Ardufocus firmware](https://github.com/jbrazio/ardufocus)
- [Ardufocus schematics](https://github.com/jbrazio/ardufocus-schematics)
- [Ardufocus ASCOM IFocuserV3 Driver](https://github.com/jbrazio/ardufocus-ascom)

Ardufocus is licensed under [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html). <br />
Ardufocus PCB schematics are licensed under [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/).

----

### This fork targets Arduino UNO class boards based on the ATmega328P/168 family.

Default hardware profile in this repository:

- `Arduino UNO`
- `28BYJ-48` unipolar stepper motor
- `ULN2003` driver board
- build system based on `PlatformIO` with `framework = arduino`
- ASCOM / Moonlite serial control on the hardware UART
- JDY-34-SPP / HC-05 / HC-06 Bluetooth control via `SoftwareSerial`

Default pinout in this fork:

- `D0/D1`: hardware serial for ASCOM / Moonlite
- `D2`: Bluetooth RX from JDY-34-SPP TX
- `D3`: Bluetooth TX to JDY-34-SPP RX
- `D8/D9/D10/D11`: ULN2003 `IN1/IN2/IN3/IN4`

Quick wiring reference:

- `UNO 5V` -> `ULN2003 VCC`
- `UNO GND` -> `ULN2003 GND`
- `UNO D8` -> `ULN2003 IN1`
- `UNO D9` -> `ULN2003 IN2`
- `UNO D10` -> `ULN2003 IN3`
- `UNO D11` -> `ULN2003 IN4`
- `28BYJ-48` -> `ULN2003` motor connector
- `UNO 5V` -> `JDY-34 VCC` only if your module board is 5V tolerant; otherwise power it at the module's required voltage
- `UNO GND` -> `JDY-34 GND`
- `UNO D2` <- `JDY-34 TXD`
- `UNO D3` -> `JDY-34 RXD`

Bluetooth wiring note:

- many JDY-34 boards use 3.3 V logic on `RXD`; if your board is not explicitly 5 V tolerant, add a level shifter or a resistor divider between `UNO D3` and `JDY-34 RXD`

3D printable enclosure:

[https://www.thingiverse.com/thing:5140868](https://www.thingiverse.com/thing:5140868)

Default Bluetooth commands:

- `FWD`: continuous forward move
- `BWD`: continuous backward move
- `FWD <n>`: move forward by `n` steps
- `BWD <n>`: move backward by `n` steps
- `STOP`: stop movement
- `POS`: report current position
- `INFO`: report Bluetooth backend info and whether any traffic has been seen since boot

Notes:

- this fork now builds with the Arduino framework and uses `SoftwareSerial` for the Bluetooth UART on `D2/D3`
- firmware version in this fork is `0.3-BT`
- the ULN2003 default uses half-step mode on a fresh EEPROM for smoother 28BYJ-48 motion
- if EEPROM already contains older settings, clear it once if you want the new defaults to take effect
- on boot the firmware prints `BT READY` and `BT LINK UNKNOWN`; the backend can detect traffic seen on the Bluetooth UART, but it cannot universally identify or interrogate every module unless that module supports a compatible AT mode
- if you enable `ENABLE_UART_DEBUG_CONSOLE` in `ardufocus/config.h`, the USB serial port stops speaking ASCOM/Moonlite and becomes a plain text debug console for `FWD`, `BWD`, `STOP`, `POS`, `INFO` and `HELP`


### For documentation, tutorials and howto visit [ardufocus.com](https://ardufocus.com).

----
