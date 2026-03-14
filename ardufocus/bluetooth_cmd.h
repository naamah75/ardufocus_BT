#ifndef __BLUETOOTH_CMD_H__
#define __BLUETOOTH_CMD_H__

#include "config.h"

#ifdef ENABLE_BLUETOOTH_SPP

#include <stdint.h>

class bluetooth_cmd {
  public:
    bluetooth_cmd() {;}
    ~bluetooth_cmd() {;}

    void parse(char* line);

  private:
    enum command_t {
      CMD_INVALID,
      CMD_FORWARD,
      CMD_BACKWARD,
      CMD_STOP,
      CMD_POSITION,
      CMD_HELP
    };

    command_t parse_command(char* token);
    bool parse_steps(char* token, uint32_t& steps);
    void execute_relative_move(const command_t& command, const uint32_t& steps);
    void execute_continuous_move(const command_t& command);
    void reply_ok();
    void reply_error(const char* message);
    void reply_position();
    void reply_help();
};

extern bluetooth_cmd bt_cmd;

#endif

#endif
