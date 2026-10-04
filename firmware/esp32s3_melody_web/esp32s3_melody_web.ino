// ESP32-S3 Super Mini: web-controlled melody player on a passive speaker.
// Speaker: GPIO4 -> speaker -> GND.
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
#include "page.h"

#define SPEAKER_PIN 4

const char *AP_SSID = "ESP32-Melody";
const char *AP_PASS = "melody123";  // at least 8 characters
const IPAddress AP_IP(192, 168, 4, 1);

const size_t MAX_NOTES = 1500;

struct Note {
  uint16_t freq;  // Hz, 0 = rest
  uint16_t ms;    // full slot length
};

Note song[MAX_NOTES];
size_t songLen = 0;
String songName;
bool loopSong = true;
uint16_t pauseMs = 1000;
uint8_t volume = 50;  // 1..100 %

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

void speakerOff() { ledcWrite(SPEAKER_PIN, 0); }

void speakerOn(uint16_t freq) {
  ledcWriteTone(SPEAKER_PIN, freq);
  // ledcWriteTone sets 50 % duty (0x1FF of 10 bits); scale it for volume.
  ledcWrite(SPEAKER_PIN, (uint32_t)0x1FF * volume / 100);
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

void saveSettings() {
  prefs.putBool("loop", loopSong);
  prefs.putUShort("pause", pauseMs);
  prefs.putUChar("vol", volume);
}

void loadFromFlash() {
  loopSong = prefs.getBool("loop", true);
  pauseMs = prefs.getUShort("pause", 1000);
  volume = constrain(prefs.getUChar("vol", 50), 1, 100);
  size_t bytes = prefs.getBytesLength("song");
  if (bytes >= sizeof(Note) && bytes <= sizeof(song) && bytes % sizeof(Note) == 0) {
    prefs.getBytes("song", song, bytes);
    songLen = bytes / sizeof(Note);
    songName = prefs.getString("name", "Моя мелодия");
  } else {
    loadDefaultSong();
  }
}

// ---------------------------------------------------------------------------
// HTTP API

void applySettingsFromArgs() {
  if (server.hasArg("loop")) loopSong = server.arg("loop") == "1";
  if (server.hasArg("pause")) pauseMs = constrain(server.arg("pause").toInt(), 0, 60000);
  if (server.hasArg("vol")) volume = constrain(server.arg("vol").toInt(), 1, 100);
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
  j += "}";
  server.send(200, "application/json", j);
}

// Body: "freq,ms;freq,ms;..."  Query: name, loop, pause, vol, save=1
void handlePlay() {
  const String &body = server.arg("plain");
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
        song[count].freq = (f >= 20 && f <= 20000) ? f : 0;
        song[count].ms = constrain(d, 1, 65535);
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
  songName = server.hasArg("name") && server.arg("name").length() ? server.arg("name") : "Моя мелодия";
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
  saveSettings();
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
  ledcAttach(SPEAKER_PIN, 1000, 10);
  speakerOff();

  prefs.begin("melody", false);
  loadFromFlash();

  WiFi.mode(WIFI_AP);
  WiFi.softAPConfig(AP_IP, AP_IP, IPAddress(255, 255, 255, 0));
  WiFi.softAP(AP_SSID, AP_PASS);
  dns.start(53, "*", AP_IP);
  setupServer();
  Serial.printf("Wi-Fi \"%s\" / \"%s\", open http://%s\n", AP_SSID, AP_PASS, AP_IP.toString().c_str());

  playFromStart();
}

void loop() {
  dns.processNextRequest();
  server.handleClient();
  playerTick();
}
