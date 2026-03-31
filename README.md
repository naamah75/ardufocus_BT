# 🔭 Ardufocus BT – Wireless Telescope Focuser

> Turn your Ardufocus into a **fully wireless focuser system**

Bluetooth-enabled fork of Ardufocus designed for **portable astrophotography setups**.

---

## 🚀 Why this fork?

Classic Ardufocus requires a USB cable.

This version adds:

- 📡 Wireless control via Bluetooth
- 📱 Smartphone interface
- 🧳 Cleaner field setup (no cables hanging from the telescope)

---

## ✨ Features

- Bluetooth serial communication (JDY-31 / HC-05 / HC-06)
- Compatible with ASCOM / Moonlite (via hardware UART)
- Real-time telemetry
- Step-based precision control
- Mobile-friendly interface

---

## 📸 Demo

### 📱 Mobile App – Focus Control
![Focus Control](https://github.com/naamah75/ardufocus_BT/blob/master/doc/app_focus.jpg)

### 📡 Telemetry & Debug
![Telemetry](https://github.com/naamah75/ardufocus_BT/blob/master/doc/app_telemetry.jpg)

### 🔗 Bluetooth Connection
![Connection](https://github.com/naamah75/ardufocus_BT/blob/master/doc/app_connection.jpg)

---

## 🔧 Hardware Setup

### 🧩 Default Configuration

- Arduino UNO  
- 28BYJ-48 stepper motor  
- ULN2003 driver  
- JDY-31 / JDY-34 / HC-05 / HC-06 Bluetooth module  
- PlatformIO (Arduino framework)

---

### 🔌 Pinout

| Arduino Pin | Function |
|------------|--------|
| D0 / D1 | Hardware Serial (ASCOM / Moonlite) |
| D2 | Bluetooth RX (from module TX) |
| D3 | Bluetooth TX (to module RX) |
| D8 | ULN2003 IN1 |
| D9 | ULN2003 IN2 |
| D10 | ULN2003 IN3 |
| D11 | ULN2003 IN4 |

---

### ⚡ Wiring

#### Stepper + Driver

`UNO 5V`  -> `ULN2003 VCC` 
`UNO GND` -> `ULN2003 GND`  

`UNO D8`  -> `ULN2003 IN1` 
`UNO D9`  -> `ULN2003 IN2`  
`UNO D10` -> `ULN2003 IN3`  
`UNO D11` -> `ULN2003 IN4`  

`28BYJ-48`-> `ULN2003 connector` 

#### Bluetooth Module

`UNO GND` -> `BT GND`  
`UNO D2` <- `BT TXD`  
`UNO D3` -> `BT RXD`  

⚠️ Some modules are NOT 5V tolerant → use 3.3V or level shifting if needed.

---

## 📡 Bluetooth Configuration

- Backend: SoftwareSerial  
- Baud rate: 9600  
- RX pin: D2  
- TX pin: D3  

---

## 🧱 Case

3D printable enclosure:

https://www.thingiverse.com/thing:5140868

---

## 🤝 Credits

Based on original Ardufocus project.

---

## 📢 Feedback

Suggestions, issues and improvements are welcome!
