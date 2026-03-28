#ifndef __BLUETOOTH_SPP_H__
#define __BLUETOOTH_SPP_H__

#include "config.h"

#ifdef ENABLE_BLUETOOTH_SPP

#include <stdint.h>

class bluetooth_spp {
  public:
    bluetooth_spp() {;}
    ~bluetooth_spp() {;}

    void setup();
    void update();
    void write(const char* str);
    void write_line(const char* str);
    void write_uint32(const uint32_t& value);
    uint8_t has_seen_activity();
    uint32_t get_rx_count();

  private:
    void process(char c);
};

extern bluetooth_spp bt_spp;

#endif

#endif
