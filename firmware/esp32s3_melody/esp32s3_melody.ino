// "Für Elise" (L. van Beethoven) on a passive speaker.
// Board: ESP32-S3 Super Mini. Speaker: GPIO4 -> speaker -> GND.
// The melody loops forever with a 1 s pause between repeats.

#include <Arduino.h>

#define SPEAKER_PIN 4

#define REST 0
#define NOTE_C4  262
#define NOTE_E4  330
#define NOTE_F4  349
#define NOTE_G4  392
#define NOTE_GS4 415
#define NOTE_A4  440
#define NOTE_B4  494
#define NOTE_C5  523
#define NOTE_D5  587
#define NOTE_DS5 622
#define NOTE_E5  659
#define NOTE_F5  698
#define NOTE_G5  784
#define NOTE_A5  880
#define NOTE_E6  1319

// Duration unit: 16th notes (tempo ~ 3/8 at Poco moto).
const int SIXTEENTH_MS = 150;

struct Note { int freq; int len; };  // len in 16ths

const Note melody[] = {
  // Theme A
  {NOTE_E5,1},{NOTE_DS5,1},
  {NOTE_E5,1},{NOTE_DS5,1},{NOTE_E5,1},{NOTE_B4,1},{NOTE_D5,1},{NOTE_C5,1},
  {NOTE_A4,2},{REST,1},{NOTE_C4,1},{NOTE_E4,1},{NOTE_A4,1},
  {NOTE_B4,2},{REST,1},{NOTE_E4,1},{NOTE_GS4,1},{NOTE_B4,1},
  {NOTE_C5,2},{REST,1},{NOTE_E4,1},{NOTE_E5,1},{NOTE_DS5,1},
  {NOTE_E5,1},{NOTE_DS5,1},{NOTE_E5,1},{NOTE_B4,1},{NOTE_D5,1},{NOTE_C5,1},
  {NOTE_A4,2},{REST,1},{NOTE_C4,1},{NOTE_E4,1},{NOTE_A4,1},
  {NOTE_B4,2},{REST,1},{NOTE_E4,1},{NOTE_C5,1},{NOTE_B4,1},
  {NOTE_A4,4},{REST,1},{NOTE_B4,1},{NOTE_C5,1},{NOTE_D5,1},
  // Theme B
  {NOTE_E5,3},{NOTE_G4,1},{NOTE_F5,1},{NOTE_E5,1},
  {NOTE_D5,3},{NOTE_F4,1},{NOTE_E5,1},{NOTE_D5,1},
  {NOTE_C5,3},{NOTE_E4,1},{NOTE_D5,1},{NOTE_C5,1},
  {NOTE_B4,2},{REST,1},{NOTE_E4,1},{NOTE_E5,1},{REST,1},
  {REST,1},{NOTE_E5,1},{NOTE_E6,1},{REST,1},{REST,1},{NOTE_DS5,1},
  {NOTE_E5,1},{REST,1},{REST,1},{NOTE_DS5,1},{NOTE_E5,1},{NOTE_DS5,1},
  // Theme A again
  {NOTE_E5,1},{NOTE_DS5,1},{NOTE_E5,1},{NOTE_B4,1},{NOTE_D5,1},{NOTE_C5,1},
  {NOTE_A4,2},{REST,1},{NOTE_C4,1},{NOTE_E4,1},{NOTE_A4,1},
  {NOTE_B4,2},{REST,1},{NOTE_E4,1},{NOTE_GS4,1},{NOTE_B4,1},
  {NOTE_C5,2},{REST,1},{NOTE_E4,1},{NOTE_E5,1},{NOTE_DS5,1},
  {NOTE_E5,1},{NOTE_DS5,1},{NOTE_E5,1},{NOTE_B4,1},{NOTE_D5,1},{NOTE_C5,1},
  {NOTE_A4,2},{REST,1},{NOTE_C4,1},{NOTE_E4,1},{NOTE_A4,1},
  {NOTE_B4,2},{REST,1},{NOTE_E4,1},{NOTE_C5,1},{NOTE_B4,1},
  {NOTE_A4,6},
};

const size_t MELODY_LEN = sizeof(melody) / sizeof(melody[0]);

void setup() {
  pinMode(SPEAKER_PIN, OUTPUT);
  digitalWrite(SPEAKER_PIN, LOW);
}

void loop() {
  for (size_t i = 0; i < MELODY_LEN; i++) {
    int durationMs = melody[i].len * SIXTEENTH_MS;
    if (melody[i].freq == REST) {
      noTone(SPEAKER_PIN);
    } else {
      // Play 90% of the slot so repeated notes are separated.
      tone(SPEAKER_PIN, melody[i].freq, durationMs * 9 / 10);
    }
    delay(durationMs);
  }
  noTone(SPEAKER_PIN);
  digitalWrite(SPEAKER_PIN, LOW);
  delay(1000);
}
