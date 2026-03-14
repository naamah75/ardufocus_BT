#include "bluetooth_softserial.h"

#ifdef ENABLE_BLUETOOTH_SPP

#include <avr/interrupt.h>
#include <util/atomic.h>
#include <util/delay.h>
#include "hal.h"
#include "macro.h"
#include "ringbuf.h"

#if !defined(__AVR_ATmega328__) && !defined(__AVR_ATmega328P__) && !defined(__AVR_ATmega328PB__) && !defined(__AVR_ATmega168__) && !defined(__AVR_ATmega168P__)
  #error Bluetooth soft serial currently supports the ATmega328/168 family only.
#endif

#if BLUETOOTH_SPP_RX_PIN != 2
  #error Bluetooth soft serial RX must use pin D2 (INT0) in this implementation.
#endif

#ifndef BLUETOOTH_SPP_SOFTSERIAL_RXBUF_SZ
  #define BLUETOOTH_SPP_SOFTSERIAL_RXBUF_SZ 32u
#endif

#ifndef BLUETOOTH_SPP_SOFTSERIAL_TXBUF_SZ
  #define BLUETOOTH_SPP_SOFTSERIAL_TXBUF_SZ 64u
#endif

#define BLUETOOTH_SPP_TIMER_PRESCALER 8UL
#define BLUETOOTH_SPP_TIMER_CS_BITS bit(CS11)
#define BLUETOOTH_SPP_BIT_TICKS ((F_CPU / BLUETOOTH_SPP_TIMER_PRESCALER) / BLUETOOTH_SPP_BAUDRATE)
#define BLUETOOTH_SPP_BIT_DELAY_US (1000000.0 / BLUETOOTH_SPP_BAUDRATE)
#define BLUETOOTH_SPP_RX_START_DELAY_US (BLUETOOTH_SPP_BIT_DELAY_US * 1.5)

static Ringbuf<char, BLUETOOTH_SPP_SOFTSERIAL_RXBUF_SZ> bt_rx_buffer;
static Ringbuf<char, BLUETOOTH_SPP_SOFTSERIAL_TXBUF_SZ> bt_tx_buffer;

static volatile uint8_t* bt_tx_port = 0;
static volatile uint8_t* bt_rx_pin = 0;
static uint8_t bt_tx_mask = 0;
static uint8_t bt_rx_mask = 0;

static volatile uint16_t bt_tx_frame = 0;
static volatile uint8_t bt_tx_bits = 0;
static volatile bool bt_tx_active = false;

bluetooth_softserial bt_serial;

static inline void bt_tx_high()
{
  *bt_tx_port |= bt_tx_mask;
}

static inline void bt_tx_low()
{
  *bt_tx_port &= ~bt_tx_mask;
}

static inline uint8_t bt_rx_level()
{
  return ((*bt_rx_pin & bt_rx_mask) != 0) ? 1 : 0;
}

void bluetooth_softserial::setup()
{
  volatile uint8_t* const tx_dir = (volatile uint8_t *)(hal_tbl_lookup(BLUETOOTH_SPP_TX_PIN, IO_DIR));
  volatile uint8_t* const tx_port = (volatile uint8_t *)(hal_tbl_lookup(BLUETOOTH_SPP_TX_PIN, IO_DATA));
  volatile uint8_t* const rx_dir = (volatile uint8_t *)(hal_tbl_lookup(BLUETOOTH_SPP_RX_PIN, IO_DIR));
  volatile uint8_t* const rx_port = (volatile uint8_t *)(hal_tbl_lookup(BLUETOOTH_SPP_RX_PIN, IO_DATA));

  bt_tx_mask = hal_tbl_lookup(BLUETOOTH_SPP_TX_PIN, IO_BIT);
  bt_rx_mask = hal_tbl_lookup(BLUETOOTH_SPP_RX_PIN, IO_BIT);
  bt_tx_port = tx_port;
  bt_rx_pin = (volatile uint8_t *)(hal_tbl_lookup(BLUETOOTH_SPP_RX_PIN, IO_IN));

  ATOMIC_BLOCK(ATOMIC_RESTORESTATE) {
    *tx_dir |= bt_tx_mask;
    *tx_port |= bt_tx_mask;

    *rx_dir &= ~bt_rx_mask;
    *rx_port |= bt_rx_mask;

    EIMSK &= ~bit(INT0);
    EICRA &= ~(bit(ISC00) | bit(ISC01));
    EICRA |= bit(ISC01);
    EIFR |= bit(INTF0);
    EIMSK |= bit(INT0);

    TCCR1A = 0;
    TCCR1B = 0;
    TIMSK1 &= ~bit(OCIE1A);
    OCR1A = (uint16_t)(BLUETOOTH_SPP_BIT_TICKS - 1);
    TCNT1 = 0;
  }
}

uint8_t bluetooth_softserial::available()
{
  return bt_rx_buffer.empty() ? 0 : 1;
}

char bluetooth_softserial::read()
{
  return bt_rx_buffer.dequeue();
}

void bluetooth_softserial::write(const char& c)
{
  while (!bt_tx_buffer.enqueue(c)) { ; }
  start_tx();
}

void bluetooth_softserial::write(const char* str)
{
  while (*str) {
    write(*str++);
  }
}

void bluetooth_softserial::start_tx()
{
  ATOMIC_BLOCK(ATOMIC_RESTORESTATE) {
    if (bt_tx_active || bt_tx_buffer.empty()) {
      return;
    }

    const uint8_t data = (uint8_t)bt_tx_buffer.dequeue();
    bt_tx_frame = ((uint16_t)1 << 9) | ((uint16_t)data << 1);
    bt_tx_bits = 10;
    bt_tx_active = true;

    if (bt_tx_frame & 0x01u) { bt_tx_high(); }
    else { bt_tx_low(); }

    bt_tx_frame >>= 1;
    --bt_tx_bits;

    TCNT1 = 0;
    TCCR1B = bit(WGM12) | BLUETOOTH_SPP_TIMER_CS_BITS;
    TIMSK1 |= bit(OCIE1A);
  }
}

void bluetooth_softserial::handle_rx_start()
{
  uint8_t data = 0;

  EIMSK &= ~bit(INT0);

  _delay_us(BLUETOOTH_SPP_RX_START_DELAY_US);

  for (uint8_t i = 0; i < 8; ++i) {
    if (bt_rx_level()) {
      data |= bit(i);
    }

    _delay_us(BLUETOOTH_SPP_BIT_DELAY_US);
  }

  const uint8_t stop_bit = bt_rx_level();
  _delay_us(BLUETOOTH_SPP_BIT_DELAY_US);

  if (stop_bit) {
    bt_rx_buffer.enqueue((char)data);
  }

  EIFR |= bit(INTF0);
  EIMSK |= bit(INT0);
}

void bluetooth_softserial::handle_tx_tick()
{
  if (!bt_tx_active) {
    TIMSK1 &= ~bit(OCIE1A);
    TCCR1B = 0;
    bt_tx_high();
    return;
  }

  if (bt_tx_bits == 0) {
    if (bt_tx_buffer.empty()) {
      bt_tx_active = false;
      TIMSK1 &= ~bit(OCIE1A);
      TCCR1B = 0;
      bt_tx_high();
      return;
    }

    const uint8_t data = (uint8_t)bt_tx_buffer.dequeue();
    bt_tx_frame = ((uint16_t)1 << 9) | ((uint16_t)data << 1);
    bt_tx_bits = 10;
  }

  if (bt_tx_frame & 0x01u) { bt_tx_high(); }
  else { bt_tx_low(); }

  bt_tx_frame >>= 1;
  --bt_tx_bits;
}

ISR(INT0_vect)
{
  bluetooth_softserial::handle_rx_start();
}

ISR(TIMER1_COMPA_vect)
{
  bluetooth_softserial::handle_tx_tick();
}

#endif
