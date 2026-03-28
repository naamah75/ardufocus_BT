#include "uart_debug.h"

#ifdef ENABLE_UART_DEBUG_CONSOLE

#include <string.h>
#include "bluetooth_cmd.h"

#ifndef CMD_START_CHAR
  #define CMD_START_CHAR ':'
#endif

#ifndef CMD_END_CHAR
  #define CMD_END_CHAR '#'
#endif

#include "serial.h"

#ifndef UART_DEBUG_RX_BUFFER
  #define UART_DEBUG_RX_BUFFER 32u
#endif

class uart_debug_port: protected serial {
  public:
    void setup_port() {
      setup();
    }

    size_t write_text(const char* str) {
      return write(str);
    }

    size_t write_char(const char c) {
      return write(c);
    }

    uint8_t read_char(char& c) {
      ATOMIC_BLOCK(ATOMIC_RESTORESTATE) {
        if (usart::buffer.rx.empty()) {
          return 0;
        }

        c = usart::buffer.rx.dequeue();
        return 1;
      }

      return 0;
    }
};

static uart_debug_port debug_port;
static char debug_line[UART_DEBUG_RX_BUFFER] = {0};
static uint8_t debug_line_pos = 0;

uart_debug debug_uart;

void uart_debug::setup()
{
  debug_port.setup_port();
  write_line("UART DEBUG MODE");
  write_line("ASCOM DISABLED");
  write_line("Use FWD/BWD/STOP/POS/INFO/HELP");
}

void uart_debug::update()
{
  char c = 0;

  while (debug_port.read_char(c)) {
    process(c);
  }
}

void uart_debug::write(const char* str)
{
  debug_port.write_text(str);
}

void uart_debug::write_line(const char* str)
{
  debug_port.write_text(str);
  debug_port.write_char('\r');
  debug_port.write_char('\n');
}

void uart_debug::process(char c)
{
  if ((c == '\r') || (c == '\n') || (c == '#')) {
    if (debug_line_pos == 0) {
      return;
    }

    debug_line[debug_line_pos] = 0;
    bt_cmd.parse(debug_line);
    memset(debug_line, 0, sizeof(debug_line));
    debug_line_pos = 0;
    return;
  }

  if (debug_line_pos >= (sizeof(debug_line) - 1)) {
    memset(debug_line, 0, sizeof(debug_line));
    debug_line_pos = 0;
    write_line("ERR TOO_LONG");
    return;
  }

  debug_line[debug_line_pos++] = c;
}

#endif
