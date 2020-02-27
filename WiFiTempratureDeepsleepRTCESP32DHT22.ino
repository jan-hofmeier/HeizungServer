#include "DHT.h"
#include <WiFi.h>
#include "wifisettings.h"

#define LED 12 //LED_BUILTIN
#define DHTTYPE DHT22
#define DHTPin 4 
#define RETRIES 5
#define RESTORE_MODE 3

#define MAX_ROUND 3

const int oneWireBus = 4;
const int serverPort = 50000;

bool ledEnable = false;

static RTC_DATA_ATTR struct {
  byte mac [ 6 ];
  byte mode;
  byte chl;
  uint32_t ip;
  uint32_t gw;
  uint32_t msk;
  uint32_t dns;
  uint16_t localPort;
  uint32_t round;
  uint32_t chk;
} cfgbuf;

bool checkcfg() {
  uint32_t x = 0;
  uint32_t *p = (uint32_t *)cfgbuf.mac;
  for (uint32_t i = 0; i < sizeof(cfgbuf) / 4; i++) x += p[i];
  printf("RTC read: chk=%x x=%x ip=%08x mode=%d %s\n", cfgbuf.chk, x, cfgbuf.ip, cfgbuf.mode,
         x == 10 ? "OK" : "FAIL");
  if (x == 0 && cfgbuf.ip != 0)
    return true;
  p = (uint32_t *)cfgbuf.mac;
  for (uint32_t i = 0; i < sizeof(cfgbuf) / 4; i++)
    printf(" %08x", p[i]);
  printf("\n");
  // bad checksum, init data
  cfgbuf.round = 0;
  for (uint32_t i = 0; i < 6; i++)
    cfgbuf.mac[i] = 0xff;
  cfgbuf.mode = 0; // chk err, reconfig
  cfgbuf.chl = 0;
  cfgbuf.ip = IPAddress(0, 0, 0, 0);
  cfgbuf.gw = IPAddress(0, 0, 0, 0);
  cfgbuf.msk = IPAddress(255, 255, 255, 0);
  cfgbuf.dns = IPAddress(0, 0, 0, 0);
  cfgbuf.localPort = 10000;
  return false;
}

void writecfg(void) {
  // save new info
  uint8_t *bssid = WiFi.BSSID();
  for (uint32_t i = 0; i < sizeof(cfgbuf.mac); i++) cfgbuf.mac[i] = bssid[i];
  cfgbuf.chl = WiFi.channel();
  cfgbuf.ip = WiFi.localIP();
  cfgbuf.gw = WiFi.gatewayIP();
  cfgbuf.msk = WiFi.subnetMask();
  cfgbuf.dns = WiFi.dnsIP();
  //printf("BSSID: %x:%x:%x:%x:%x:%x\n", cfgbuf.mac[0], cfgbuf.mac[1], cfgbuf.mac[2],
  //    cfgbuf.mac[3], cfgbuf.mac[4], cfgbuf.mac[5]);
  // recalculate checksum
  uint32_t x = 0;
  uint32_t *p = (uint32_t *)cfgbuf.mac;
  for (uint32_t i = 0; i < sizeof(cfgbuf) / 4 - 1; i++) x += p[i];
  cfgbuf.chk = -x + 10;
  printf("RTC write: chk=%x x=%x ip=%08x mode=%d\n", cfgbuf.chk, x, cfgbuf.ip, cfgbuf.mode);
  p = (uint32_t *)cfgbuf.mac;
  for (uint32_t i = 0; i < sizeof(cfgbuf) / 4; i++) printf(" %08x", p[i]);
  printf("\n");
}


void setDebugLED(bool on) {
  if (ledEnable)
    digitalWrite(LED, on ? HIGH : LOW);
}

void wifiBegin(void){
    switch (cfgbuf.mode) {
    case 0:
      WiFi.begin(SSID, PASSWORD);
      break;
    case 1:
      WiFi.begin(SSID, PASSWORD, cfgbuf.chl, cfgbuf.mac);
      break;
    case 2: {
        bool ok = WiFi.config(cfgbuf.ip, cfgbuf.gw, cfgbuf.msk, cfgbuf.dns);
        if (!ok) Serial.print("*** Wifi.config failed");
        WiFi.begin(SSID, PASSWORD);
        break;
      }
    default:
      bool ok = WiFi.config(cfgbuf.ip, cfgbuf.gw, cfgbuf.msk, cfgbuf.dns);
      if (!ok) Serial.print("*** Wifi.config failed");
      WiFi.begin(SSID, PASSWORD, cfgbuf.chl, cfgbuf.mac);
      break;
  }
}


DHT dht(DHTPin, DHTTYPE);

void setup() {
  digitalWrite(LED, LOW);
  pinMode(LED, OUTPUT);
  
  checkcfg();

  Serial.begin(115200);
  if (cfgbuf.round < MAX_ROUND) {
    cfgbuf.round++;
    Serial.printf("Round %i: Enable Debug LED\n", cfgbuf.round);
    ledEnable = true;
  }

  setDebugLED(true);

  //Kontrollausgabe aktivieren
  Serial.println();
  Serial.print("Versuche Verbindung zum AP mit der SSID=");
  Serial.print(SSID);
  Serial.println(" herzustellen");

  if (WiFi.getMode() != WIFI_OFF) {
    WiFi.persistent(true);
    WiFi.mode(WIFI_OFF);
  }

  WiFi.persistent(false);
  WiFi.mode(WIFI_STA);

  wifiBegin();

  dht.begin();
  
  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();
  Serial.print("Read temprature: ");
  Serial.println(temperature);

  /*Solange keineVerbindung zu einem AccessPoint (AP) aufgebaut wurde*/
  bool ledon = 1;
  while (WiFi.status() != WL_CONNECTED) {
    wifiBegin();
    setDebugLED(ledon = !ledon);
    delay(500);
    Serial.print(".");
  }

  setDebugLED(true);

  Serial.println();

  Serial.print("Verbunden mit IP ");
  Serial.println(WiFi.localIP());

  /*Signalstärke des AP*/
  long rssi = WiFi.RSSI();
  Serial.print("Signalstaerke(RSSI) des AP:");
  Serial.print(rssi);
  Serial.println(" dBm");

  WiFiClient client;
  int i;
  for (i = RETRIES; !client.connect(HOST, serverPort) && (i > 0); i-- ) {
    Serial.print("X");
    delay(1000);
  }
  if (!i)
    return;


  Serial.print("Connected to ");
  Serial.println(HOST);

  client.print(ROOM);
  client.print(": ");
  client.print(temperature);
  client.print('/');
  client.println(humidity);

  client.flush();
  client.stop();

  Serial.println("Connection Closed");
  
  cfgbuf.mode=RESTORE_MODE;
  writecfg();
}

void loop() {
  WiFi.disconnect();
  setDebugLED(false);
  Serial.println("Deepsleep");
  esp_sleep_enable_timer_wakeup(50000 * 1000); // Deep-Sleep time in microseconds
  esp_deep_sleep_start();
  delay(100);
}

