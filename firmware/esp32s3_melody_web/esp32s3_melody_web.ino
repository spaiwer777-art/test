// ESP32-S3 Super Mini: web-controlled melody player on a passive speaker.
// Speaker: pin D4 (GPIO5) -> speaker -> GND. The pin can be changed on the
// web page (boards labelled D0..D10 use the XIAO ESP32S3 pin layout).
//
// The board starts a Wi-Fi access point "ESP32-Melody" (password "melody123").
// Connect to it and open http://192.168.4.1 (most phones open the page by
// themselves). On the page you can paste an RTTTL ringtone, upload a .rtttl /
// .txt / .mid file or pick a preset. The browser converts it to a plain list of
// "frequency,duration" pairs and sends it here; the board plays it on GPIO4.
// A saved melody is kept in flash and plays on every power-up.

#include <Arduino.h>
#include <WiFi.h>
#include <WebServer.h>
#include <DNSServer.h>
#include <Preferences.h>
#include <driver/gpio.h>
#include "page.h"

// Pins offered for the speaker: board label -> GPIO (XIAO ESP32S3 layout).
// D6/D7 (GPIO43/44) are left out: they are the serial port.
struct PinOption { const char *label; uint8_t gpio; };
const PinOption SPEAKER_PINS[] = {
  {"D0", 1}, {"D1", 2}, {"D2", 3}, {"D3", 4}, {"D4", 5}, {"D5", 6},
  {"D8", 7}, {"D9", 8}, {"D10", 9},
};
const uint8_t DEFAULT_SPEAKER_GPIO = 5;  // D4
uint8_t speakerPin = DEFAULT_SPEAKER_GPIO;

bool isSpeakerPin(long gpio) {
  for (const PinOption &p : SPEAKER_PINS)
    if (p.gpio == gpio) return true;
  return false;
}

const char *AP_SSID = "ESP32-Melody";
const char *AP_PASS = "melody123";  // at least 8 characters
const IPAddress AP_IP(192, 168, 4, 1);

// ---------------------------------------------------------------------------
// Safety limits. The speaker (16 ohm) is wired straight to the pin with no
// resistor, so the pin itself must limit the current.

// Pad drive strength of GPIO4: CAP_0 ~5 mA, CAP_1 ~10 mA, CAP_2 ~20 mA
// (ESP32 default), CAP_3 ~40 mA (absolute maximum, never use it here).
// CAP_2 is the chip's own default for every pin and is within spec; CAP_1 is
// quieter with more margin. Do not use CAP_3 without a series resistor or a
// transistor.
const gpio_drive_cap_t SPEAKER_DRIVE = GPIO_DRIVE_CAP_2;

// Volume 100 % = 50 % PWM duty (a square wave); never more, so the pin is
// high at most half of the time.
const uint8_t MAX_VOLUME = 100;
const uint8_t DEFAULT_VOLUME = 50;

// Notes outside this range are moved by octaves into it: a tiny speaker
// cannot reproduce them anyway, and very low tones mean long high pulses.
const uint16_t MIN_FREQ = 100;
const uint16_t MAX_FREQ = 8000;

const uint16_t MIN_NOTE_MS = 10;
const uint16_t MAX_NOTE_MS = 10000;
const uint16_t MAX_PAUSE_MS = 10000;
const size_t MAX_NOTES = 1500;
const size_t MAX_NAME_LEN = 40;
const size_t MAX_BODY_LEN = MAX_NOTES * 12;  // "8000,10000;" per note + slack

// Settings are written to flash only after they stop changing for this long,
// so dragging the volume slider does not wear the flash.
const uint32_t SETTINGS_SAVE_DELAY_MS = 3000;

// Lower Wi-Fi TX power: the access point is used at arm's length, and the
// Super Mini (small LDO, chip antenna) runs cooler and avoids brown-outs.
const wifi_power_t WIFI_TX_POWER = WIFI_POWER_8_5dBm;

struct Note {
  uint16_t freq;  // Hz, 0 = rest
  uint16_t ms;    // full slot length
};

Note song[MAX_NOTES];
size_t songLen = 0;
String songName;
bool loopSong = true;
uint16_t pauseMs = 1000;
uint8_t volume = DEFAULT_VOLUME;  // 1..MAX_VOLUME %

WebServer server(80);
DNSServer dns;
Preferences prefs;

// ---------------------------------------------------------------------------
// Default melody: "Für Elise" (16th note = 150 ms)

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
#define NOTE_E6  1319

const uint16_t FUR_ELISE[][2] = {
  {NOTE_E5,1},{NOTE_DS5,1},
  {NOTE_E5,1},{NOTE_DS5,1},{NOTE_E5,1},{NOTE_B4,1},{NOTE_D5,1},{NOTE_C5,1},
  {NOTE_A4,2},{REST,1},{NOTE_C4,1},{NOTE_E4,1},{NOTE_A4,1},
  {NOTE_B4,2},{REST,1},{NOTE_E4,1},{NOTE_GS4,1},{NOTE_B4,1},
  {NOTE_C5,2},{REST,1},{NOTE_E4,1},{NOTE_E5,1},{NOTE_DS5,1},
  {NOTE_E5,1},{NOTE_DS5,1},{NOTE_E5,1},{NOTE_B4,1},{NOTE_D5,1},{NOTE_C5,1},
  {NOTE_A4,2},{REST,1},{NOTE_C4,1},{NOTE_E4,1},{NOTE_A4,1},
  {NOTE_B4,2},{REST,1},{NOTE_E4,1},{NOTE_C5,1},{NOTE_B4,1},
  {NOTE_A4,4},{REST,1},{NOTE_B4,1},{NOTE_C5,1},{NOTE_D5,1},
  {NOTE_E5,3},{NOTE_G4,1},{NOTE_F5,1},{NOTE_E5,1},
  {NOTE_D5,3},{NOTE_F4,1},{NOTE_E5,1},{NOTE_D5,1},
  {NOTE_C5,3},{NOTE_E4,1},{NOTE_D5,1},{NOTE_C5,1},
  {NOTE_B4,2},{REST,1},{NOTE_E4,1},{NOTE_E5,1},{REST,1},
  {REST,1},{NOTE_E5,1},{NOTE_E6,1},{REST,1},{REST,1},{NOTE_DS5,1},
  {NOTE_E5,1},{REST,1},{REST,1},{NOTE_DS5,1},{NOTE_E5,1},{NOTE_DS5,1},
  {NOTE_E5,1},{NOTE_DS5,1},{NOTE_E5,1},{NOTE_B4,1},{NOTE_D5,1},{NOTE_C5,1},
  {NOTE_A4,2},{REST,1},{NOTE_C4,1},{NOTE_E4,1},{NOTE_A4,1},
  {NOTE_B4,2},{REST,1},{NOTE_E4,1},{NOTE_GS4,1},{NOTE_B4,1},
  {NOTE_C5,2},{REST,1},{NOTE_E4,1},{NOTE_E5,1},{NOTE_DS5,1},
  {NOTE_E5,1},{NOTE_DS5,1},{NOTE_E5,1},{NOTE_B4,1},{NOTE_D5,1},{NOTE_C5,1},
  {NOTE_A4,2},{REST,1},{NOTE_C4,1},{NOTE_E4,1},{NOTE_A4,1},
  {NOTE_B4,2},{REST,1},{NOTE_E4,1},{NOTE_C5,1},{NOTE_B4,1},
  {NOTE_A4,6},
};

void loadDefaultSong() {
  songLen = sizeof(FUR_ELISE) / sizeof(FUR_ELISE[0]);
  for (size_t i = 0; i < songLen; i++) {
    song[i].freq = FUR_ELISE[i][0];
    song[i].ms = FUR_ELISE[i][1] * 150;
  }
  songName = "Für Elise";
}

// ---------------------------------------------------------------------------
// Non-blocking player, driven from loop() so the web server stays responsive.

enum PlayerState { IDLE, NOTE_ON, NOTE_GAP, LOOP_PAUSE };
PlayerState state = IDLE;
size_t noteIdx = 0;
uint32_t soundEnd = 0;  // when the current tone stops
uint32_t slotEnd = 0;   // when the next note starts

static bool reached(uint32_t t) { return (int32_t)(millis() - t) >= 0; }

void speakerOff() { ledcWrite(speakerPin, 0); }

uint16_t safeFreq(long f) {
  if (f <= 0) return 0;
  while (f < MIN_FREQ) f *= 2;
  while (f > MAX_FREQ) f /= 2;
  return f;
}

uint16_t safeMs(long ms) { return constrain(ms, MIN_NOTE_MS, MAX_NOTE_MS); }

uint8_t safeVolume(long v) { return constrain(v, 1, MAX_VOLUME); }

uint16_t safePause(long ms) { return constrain(ms, 0, MAX_PAUSE_MS); }

// Cut to MAX_NAME_LEN bytes without splitting a multi-byte UTF-8 character.
String safeName(const String &s) {
  if (s.length() <= MAX_NAME_LEN) return s;
  size_t n = MAX_NAME_LEN;
  while (n > 0 && ((uint8_t)s[n] & 0xC0) == 0x80) n--;
  return s.substring(0, n);
}

void attachSpeaker(uint8_t gpio) {
  if (gpio == speakerPin) return;
  ledcDetach(speakerPin);
  pinMode(speakerPin, INPUT);  // release the old pin
  speakerPin = gpio;
  ledcAttach(speakerPin, 1000, 10);
  ledcWrite(speakerPin, 0);
  gpio_set_drive_capability((gpio_num_t)speakerPin, SPEAKER_DRIVE);
}

void speakerOn(uint16_t freq) {
  ledcWriteTone(speakerPin, safeFreq(freq));
  // ledcWriteTone sets 50 % duty (0x1FF of 10 bits); scale it for volume.
  ledcWrite(speakerPin, (uint32_t)0x1FF * safeVolume(volume) / 100);
}

void startNote() {
  if (noteIdx >= songLen) {
    speakerOff();
    if (loopSong && songLen > 0) {
      state = LOOP_PAUSE;
      slotEnd = millis() + pauseMs;
    } else {
      state = IDLE;
    }
    return;
  }
  const Note &n = song[noteIdx];
  uint32_t now = millis();
  slotEnd = now + n.ms;
  if (n.freq) {
    // Sound for 90 % of the slot so repeated notes are separated.
    speakerOn(n.freq);
    soundEnd = now + (uint32_t)n.ms * 9 / 10;
    state = NOTE_ON;
  } else {
    speakerOff();
    state = NOTE_GAP;
  }
}

void playFromStart() {
  noteIdx = 0;
  startNote();
}

void stopPlaying() {
  speakerOff();
  state = IDLE;
}

void playerTick() {
  switch (state) {
    case NOTE_ON:
      if (reached(soundEnd)) {
        speakerOff();
        state = NOTE_GAP;
      }
      break;
    case NOTE_GAP:
      if (reached(slotEnd)) {
        noteIdx++;
        startNote();
      }
      break;
    case LOOP_PAUSE:
      if (reached(slotEnd)) playFromStart();
      break;
    case IDLE:
      break;
  }
}

// ---------------------------------------------------------------------------
// Persistence

void saveSong() {
  prefs.putBytes("song", song, songLen * sizeof(Note));
  prefs.putString("name", songName);
}

bool settingsDirty = false;
uint32_t settingsChangedAt = 0;

void saveSettings() {
  settingsDirty = false;
  prefs.putBool("loop", loopSong);
  prefs.putUShort("pause", pauseMs);
  prefs.putUChar("vol", volume);
  prefs.putUChar("pin", speakerPin);
}

void loadFromFlash() {
  loopSong = prefs.getBool("loop", true);
  pauseMs = safePause(prefs.getUShort("pause", 1000));
  volume = safeVolume(prefs.getUChar("vol", DEFAULT_VOLUME));
  size_t bytes = prefs.getBytesLength("song");
  if (bytes >= sizeof(Note) && bytes <= sizeof(song) && bytes % sizeof(Note) == 0) {
    prefs.getBytes("song", song, bytes);
    songLen = bytes / sizeof(Note);
    for (size_t i = 0; i < songLen; i++) {
      song[i].freq = safeFreq(song[i].freq);
      song[i].ms = safeMs(song[i].ms);
    }
    songName = safeName(prefs.getString("name", "Моя мелодия"));
  } else {
    loadDefaultSong();
  }
}

// ---------------------------------------------------------------------------
// HTTP API

void applySettingsFromArgs() {
  if (server.hasArg("loop")) loopSong = server.arg("loop") == "1";
  if (server.hasArg("pause")) pauseMs = safePause(server.arg("pause").toInt());
  if (server.hasArg("vol")) volume = safeVolume(server.arg("vol").toInt());
  if (server.hasArg("pin") && isSpeakerPin(server.arg("pin").toInt())) {
    bool wasPlaying = state != IDLE;
    stopPlaying();
    attachSpeaker(server.arg("pin").toInt());
    if (wasPlaying) playFromStart();
  }
}

String jsonEscape(const String &s) {
  String out;
  for (size_t i = 0; i < s.length(); i++) {
    char c = s[i];
    if (c == '"' || c == '\\') { out += '\\'; out += c; }
    else if ((uint8_t)c < 0x20) out += ' ';
    else out += c;
  }
  return out;
}

void sendStatus() {
  uint32_t total = 0;
  for (size_t i = 0; i < songLen; i++) total += song[i].ms;
  String j = "{";
  j += "\"playing\":" + String(state != IDLE ? "true" : "false");
  j += ",\"name\":\"" + jsonEscape(songName) + "\"";
  j += ",\"notes\":" + String(songLen);
  j += ",\"index\":" + String(noteIdx);
  j += ",\"durationMs\":" + String(total);
  j += ",\"loop\":" + String(loopSong ? "true" : "false");
  j += ",\"pause\":" + String(pauseMs);
  j += ",\"vol\":" + String(volume);
  j += ",\"maxNotes\":" + String(MAX_NOTES);
  j += ",\"maxVol\":" + String(MAX_VOLUME);
  j += ",\"pin\":" + String(speakerPin);
  j += ",\"pins\":[";
  for (size_t i = 0; i < sizeof(SPEAKER_PINS) / sizeof(SPEAKER_PINS[0]); i++) {
    if (i) j += ",";
    j += "[\"" + String(SPEAKER_PINS[i].label) + "\"," + String(SPEAKER_PINS[i].gpio) + "]";
  }
  j += "]";
  j += ",\"minFreq\":" + String(MIN_FREQ);
  j += ",\"maxFreq\":" + String(MAX_FREQ);
  j += ",\"minNoteMs\":" + String(MIN_NOTE_MS);
  j += ",\"maxNoteMs\":" + String(MAX_NOTE_MS);
  j += ",\"maxPause\":" + String(MAX_PAUSE_MS);
  j += "}";
  server.send(200, "application/json", j);
}

// Body: "freq,ms;freq,ms;..."  Query: name, loop, pause, vol, save=1
void handlePlay() {
  const String &body = server.arg("plain");
  if (body.length() > MAX_BODY_LEN) {
    server.send(413, "text/plain", "Слишком длинная мелодия");
    return;
  }
  size_t count = 0;
  int pos = 0;
  int len = body.length();
  while (pos < len && count < MAX_NOTES) {
    int sep = body.indexOf(';', pos);
    if (sep < 0) sep = len;
    int comma = body.indexOf(',', pos);
    if (comma > pos && comma < sep) {
      long f = body.substring(pos, comma).toInt();
      long d = body.substring(comma + 1, sep).toInt();
      if (d > 0) {
        song[count].freq = safeFreq(f);
        song[count].ms = safeMs(d);
        count++;
      }
    }
    pos = sep + 1;
  }
  if (count == 0) {
    server.send(400, "text/plain", "Нет нот в запросе");
    return;
  }
  stopPlaying();
  songLen = count;
  songName = server.hasArg("name") && server.arg("name").length() ? safeName(server.arg("name")) : "Моя мелодия";
  applySettingsFromArgs();
  if (server.arg("save") == "1") {
    saveSong();
    saveSettings();
  }
  playFromStart();
  sendStatus();
}

void handleSettings() {
  applySettingsFromArgs();
  settingsDirty = true;
  settingsChangedAt = millis();
  sendStatus();
}

void setupServer() {
  server.on("/", HTTP_GET, [] { server.send_P(200, "text/html; charset=utf-8", INDEX_HTML); });
  server.on("/api/status", HTTP_GET, sendStatus);
  server.on("/api/play", HTTP_POST, handlePlay);
  server.on("/api/replay", HTTP_POST, [] { playFromStart(); sendStatus(); });
  server.on("/api/stop", HTTP_POST, [] { stopPlaying(); sendStatus(); });
  server.on("/api/settings", HTTP_POST, handleSettings);
  server.on("/api/default", HTTP_POST, [] {
    stopPlaying();
    loadDefaultSong();
    prefs.remove("song");
    prefs.remove("name");
    playFromStart();
    sendStatus();
  });
  // Captive portal: send every unknown URL to the main page.
  server.onNotFound([] {
    server.sendHeader("Location", String("http://") + AP_IP.toString() + "/", true);
    server.send(302, "text/plain", "");
  });
  server.begin();
}

void setup() {
  Serial.begin(115200);
  prefs.begin("melody", false);
  loadFromFlash();
  uint8_t pin = prefs.getUChar("pin", DEFAULT_SPEAKER_GPIO);
  speakerPin = isSpeakerPin(pin) ? pin : DEFAULT_SPEAKER_GPIO;
  ledcAttach(speakerPin, 1000, 10);
  speakerOff();
  gpio_set_drive_capability((gpio_num_t)speakerPin, SPEAKER_DRIVE);

  WiFi.mode(WIFI_AP);
  WiFi.softAPConfig(AP_IP, AP_IP, IPAddress(255, 255, 255, 0));
  WiFi.softAP(AP_SSID, AP_PASS);
  WiFi.setTxPower(WIFI_TX_POWER);
  dns.start(53, "*", AP_IP);
  setupServer();
  Serial.printf("Wi-Fi \"%s\" / \"%s\", open http://%s\n", AP_SSID, AP_PASS, AP_IP.toString().c_str());

  playFromStart();
}

void loop() {
  dns.processNextRequest();
  server.handleClient();
  playerTick();
  if (settingsDirty && (millis() - settingsChangedAt) >= SETTINGS_SAVE_DELAY_MS) saveSettings();
}
