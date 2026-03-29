![Image](https://ardufocus.com/assets/images/layout/logo-github.png)

[![Donate Patreon](https://img.shields.io/badge/Donate-Patreon-blue.svg?style=for-the-badge)](https://www.patreon.com/join/jbrazio?)
[![Donate PayPal](https://img.shields.io/badge/Donate-Paypal-blue.svg?style=for-the-badge)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=D5XZ7QFLP8LXE)
[![Travis (.com)](https://img.shields.io/travis/com/jbrazio/ardufocus.svg?style=for-the-badge)](https://travis-ci.com/jbrazio/ardufocus)
[![Download beta](https://img.shields.io/github/release-pre/jbrazio/ardufocus.svg?style=for-the-badge)](https://github.com/jbrazio/ardufocus/archive/master.zip)
[![GitHub stars](https://img.shields.io/github/stars/jbrazio/ardufocus.svg?style=for-the-badge)](https://github.com/jbrazio/ardufocus/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/jbrazio/ardufocus.svg?style=for-the-badge)](https://github.com/jbrazio/ardufocus/network)

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

Default Bluetooth commands:

- `FWD` or `AVANTI`: continuous forward move
- `BWD` or `INDIETRO`: continuous backward move
- `FWD <n>`: move forward by `n` steps
- `BWD <n>`: move backward by `n` steps
- `STOP`: stop movement
- `POS`: report current position
- `INFO`: report Bluetooth backend info and whether any traffic has been seen since boot

Notes:

- this fork now builds with the Arduino framework and uses `SoftwareSerial` for the Bluetooth UART on `D2/D3`
- the ULN2003 default uses half-step mode on a fresh EEPROM for smoother 28BYJ-48 motion
- if EEPROM already contains older settings, clear it once if you want the new defaults to take effect
- on boot the firmware prints `BT READY` and `BT LINK UNKNOWN`; the backend can detect traffic seen on the Bluetooth UART, but it cannot universally identify or interrogate every module unless that module supports a compatible AT mode
- if you enable `ENABLE_UART_DEBUG_CONSOLE` in `ardufocus/config.h`, the USB serial port stops speaking ASCOM/Moonlite and becomes a plain text debug console for `FWD`, `BWD`, `STOP`, `POS`, `INFO` and `HELP`

Android app suggestion:

- `Serial Bluetooth Terminal` is the easiest ready-made Android app for this firmware
- it supports Bluetooth Classic SPP modules such as `JDY-34-SPP`, `HC-05` and `HC-06`
- you can create custom buttons/macros like `FWD`, `BWD`, `STOP`, `FWD 50`, `BWD 50`, `POS` and `INFO`

Included Android app:

- this repository now also contains a small native Android app in `android-app/`
- it provides paired-device selection, connect/disconnect, continuous `Avanti` / `Indietro`, step presets, `STOP`, `POS`, and a live text log
- if a paired module named `JDY-31-SPP` is present, the app selects it by default; otherwise it falls back to `JDY-34-SPP`, `HC-05`, `HC-06`, then the first paired device
- the two large direction buttons send continuous `FWD` and `BWD`
- preset buttons send relative moves for `1`, `5`, `10`, `50`, `100`, and `500` steps in either direction
- the app shows the current focuser position, requests `POS` on connect and every 60 seconds, and retries `POS` / `INFO` up to 5 times if the firmware answers `ERR UNKNOWN`
- after relative moves, the app updates the displayed position optimistically and later re-syncs it from the controller
- open `android-app/` with Android Studio to build the APK
- on Android 12 and newer, grant both Bluetooth connect and Bluetooth scan permissions when the app asks for them
- only APK files under `android-app/app/release/` are meant to be versioned; generated APKs elsewhere remain ignored

Build:

```ini
pio run -e uno
```

### For documentation, tutorials and howto visit [ardufocus.com](https://ardufocus.com).

----
