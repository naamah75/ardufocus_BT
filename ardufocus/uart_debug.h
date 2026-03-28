#ifndef __UART_DEBUG_H__
#define __UART_DEBUG_H__

#include "config.h"

#ifdef ENABLE_UART_DEBUG_CONSOLE

class uart_debug {
  public:
    uart_debug() {;}
    ~uart_debug() {;}

    void setup();
    void update();
    void write(const char* str);
    void write_line(const char* str);

  private:
    void process(char c);
};

extern uart_debug debug_uart;

#endif

#endif
