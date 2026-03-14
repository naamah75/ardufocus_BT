#ifndef __BLUETOOTH_SOFTSERIAL_H__
#define __BLUETOOTH_SOFTSERIAL_H__

#include "config.h"

#ifdef ENABLE_BLUETOOTH_SPP

#include <stdint.h>

class bluetooth_softserial {
  public:
    bluetooth_softserial() {;}
    ~bluetooth_softserial() {;}

    void setup();
    uint8_t available();
    char read();
    void write(const char& c);
    void write(const char* str);

    static void handle_rx_start();
    static void handle_tx_tick();

  private:
    static void start_tx();
};

extern bluetooth_softserial bt_serial;

#endif

#endif
