#include "bluetooth_softserial.h"

#ifdef ENABLE_BLUETOOTH_SPP

#include <Arduino.h>
#include <NeoSWSerial.h>

static NeoSWSerial bt_port(BLUETOOTH_SPP_RX_PIN, BLUETOOTH_SPP_TX_PIN);

bluetooth_softserial bt_serial;

void bluetooth_softserial::setup()
{
  bt_port.begin(BLUETOOTH_SPP_BAUDRATE);
}

uint8_t bluetooth_softserial::available()
{
  return (uint8_t)bt_port.available();
}

char bluetooth_softserial::read()
{
  return (char)bt_port.read();
}

void bluetooth_softserial::write(const char& c)
{
  bt_port.write(c);
}

void bluetooth_softserial::write(const char* str)
{
  bt_port.print(str);
}

#endif
