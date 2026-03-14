#include "bluetooth_spp.h"

#ifdef ENABLE_BLUETOOTH_SPP

#include <stdio.h>
#include <string.h>
#include "bluetooth_cmd.h"
#include "bluetooth_softserial.h"

#ifndef BLUETOOTH_SPP_RX_BUFFER
  #define BLUETOOTH_SPP_RX_BUFFER 32u
#endif

static char bt_line[BLUETOOTH_SPP_RX_BUFFER] = {0};
static uint8_t bt_line_pos = 0;

bluetooth_spp bt_spp;

void bluetooth_spp::setup()
{
  bt_serial.setup();
  write_line("BT READY");
}

void bluetooth_spp::update()
{
  while (bt_serial.available() > 0) {
    process(bt_serial.read());
  }
}

void bluetooth_spp::write(const char* str)
{
  bt_serial.write(str);
}

void bluetooth_spp::write_line(const char* str)
{
  bt_serial.write(str);
  bt_serial.write('\r');
  bt_serial.write('\n');
}

void bluetooth_spp::write_uint32(const uint32_t& value)
{
  char buffer[11] = {0};
  snprintf(buffer, sizeof(buffer), "%lu", (unsigned long)value);
  bt_serial.write(buffer);
}

void bluetooth_spp::process(char c)
{
  if ((c == '\r') || (c == '\n') || (c == '#')) {
    if (bt_line_pos == 0) {
      return;
    }

    bt_line[bt_line_pos] = 0;
    bt_cmd.parse(bt_line);
    memset(bt_line, 0, sizeof(bt_line));
    bt_line_pos = 0;
    return;
  }

  if (bt_line_pos >= (sizeof(bt_line) - 1)) {
    memset(bt_line, 0, sizeof(bt_line));
    bt_line_pos = 0;
    write_line("ERR TOO_LONG");
    return;
  }

  bt_line[bt_line_pos++] = c;
}

#endif
