#include "bluetooth_cmd.h"

#ifdef ENABLE_BLUETOOTH_SPP

#include <limits.h>
#include <stdlib.h>
#include <string.h>
#include "api.h"
#include "bluetooth_spp.h"

static void str_to_upper(char* text)
{
  while (*text) {
    if ((*text >= 'a') && (*text <= 'z')) {
      *text = *text - ('a' - 'A');
    }

    ++text;
  }
}

bluetooth_cmd bt_cmd;

void bluetooth_cmd::parse(char* line)
{
  char* command_token = strtok(line, " \t");

  if (command_token == 0) {
    return;
  }

  str_to_upper(command_token);

  const command_t command = parse_command(command_token);

  if (command == CMD_INVALID) {
    reply_error("UNKNOWN");
    return;
  }

  if (command == CMD_STOP) {
    api::motor_stop(BLUETOOTH_SPP_MOTOR);
    reply_ok();
    return;
  }

  if (command == CMD_POSITION) {
    reply_position();
    return;
  }

  if (command == CMD_INFO) {
    reply_info();
    return;
  }

  if (command == CMD_HELP) {
    reply_help();
    return;
  }

  char* steps_token = strtok(0, " \t");

  if (steps_token == 0) {
    execute_continuous_move(command);
    return;
  }

  uint32_t steps = 0;

  if (!parse_steps(steps_token, steps)) {
    reply_error("BAD_STEPS");
    return;
  }

  if (strtok(0, " \t") != 0) {
    reply_error("BAD_ARGS");
    return;
  }

  execute_relative_move(command, steps);
}

bluetooth_cmd::command_t bluetooth_cmd::parse_command(char* token)
{
  if ((strcmp(token, "FWD") == 0) || (strcmp(token, "FORWARD") == 0) || (strcmp(token, "AVANTI") == 0)) {
    return CMD_FORWARD;
  }

  if ((strcmp(token, "BWD") == 0) || (strcmp(token, "BACKWARD") == 0) || (strcmp(token, "INDIETRO") == 0)) {
    return CMD_BACKWARD;
  }

  if ((strcmp(token, "STOP") == 0) || (strcmp(token, "HALT") == 0) || (strcmp(token, "FERMA") == 0)) {
    return CMD_STOP;
  }

  if ((strcmp(token, "POS") == 0) || (strcmp(token, "POSITION") == 0)) {
    return CMD_POSITION;
  }

  if ((strcmp(token, "INFO") == 0) || (strcmp(token, "BTINFO") == 0) || (strcmp(token, "STATUS") == 0)) {
    return CMD_INFO;
  }

  if ((strcmp(token, "HELP") == 0) || (strcmp(token, "?") == 0)) {
    return CMD_HELP;
  }

  return CMD_INVALID;
}

bool bluetooth_cmd::parse_steps(char* token, uint32_t& steps)
{
  char* endptr = 0;
  const unsigned long value = strtoul(token, &endptr, 10);

  if ((*token == 0) || (*endptr != 0)) {
    return false;
  }

  steps = (uint32_t)value;
  return true;
}

void bluetooth_cmd::execute_relative_move(const command_t& command, const uint32_t& steps)
{
  const uint32_t current = api::motor_get_position(BLUETOOTH_SPP_MOTOR);
  uint32_t target = current;

  if (command == CMD_FORWARD) {
    target = (UINT32_MAX - current < steps) ? UINT32_MAX : current + steps;
  }
  else if (command == CMD_BACKWARD) {
    target = (steps > current) ? 0 : current - steps;
  }

  api::motor_set_target(BLUETOOTH_SPP_MOTOR, target);
  api::motor_start(BLUETOOTH_SPP_MOTOR);
  reply_ok();
}

void bluetooth_cmd::execute_continuous_move(const command_t& command)
{
  if (command == CMD_FORWARD) {
    api::motor_set_target(BLUETOOTH_SPP_MOTOR, UINT32_MAX);
    api::motor_start(BLUETOOTH_SPP_MOTOR);
    reply_ok();
    return;
  }

  if (command == CMD_BACKWARD) {
    api::motor_set_target(BLUETOOTH_SPP_MOTOR, 0);
    api::motor_start(BLUETOOTH_SPP_MOTOR);
    reply_ok();
    return;
  }

  reply_error("BAD_CMD");
}

void bluetooth_cmd::reply_ok()
{
  bt_spp.write_line("OK");
}

void bluetooth_cmd::reply_error(const char* message)
{
  bt_spp.write("ERR ");
  bt_spp.write_line(message);
}

void bluetooth_cmd::reply_position()
{
  bt_spp.write("POS ");
  bt_spp.write_uint32(api::motor_get_position(BLUETOOTH_SPP_MOTOR));
  bt_spp.write_line("");
}

void bluetooth_cmd::reply_info()
{
  bt_spp.write_line("INFO BT BACKEND AVR_SOFTSERIAL");
  bt_spp.write("INFO BT BAUD ");
  bt_spp.write_uint32(BLUETOOTH_SPP_BAUDRATE);
  bt_spp.write_line("");
  bt_spp.write("INFO BT RX_PIN ");
  bt_spp.write_uint32(BLUETOOTH_SPP_RX_PIN);
  bt_spp.write_line("");
  bt_spp.write("INFO BT TX_PIN ");
  bt_spp.write_uint32(BLUETOOTH_SPP_TX_PIN);
  bt_spp.write_line("");
  bt_spp.write("INFO BT ACTIVE ");
  bt_spp.write_line(bt_spp.has_seen_activity() ? "YES" : "NO");
  bt_spp.write("INFO BT RX_COUNT ");
  bt_spp.write_uint32(bt_spp.get_rx_count());
  bt_spp.write_line("");
}

void bluetooth_cmd::reply_help()
{
  bt_spp.write_line("FWD [n]");
  bt_spp.write_line("BWD [n]");
  bt_spp.write_line("STOP");
  bt_spp.write_line("POS");
  bt_spp.write_line("INFO");
}

#endif
