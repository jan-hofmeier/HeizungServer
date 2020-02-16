#include <ESP8266WiFi.h>
#include "wifisettingsTobi.h"
#include <OneWire.h>
#include <DallasTemperature.h>

#define LED LED_BUILTIN
#define RETRIES 5
#define RESTORE_MODE 3

#define MAX_ROUND 3

const int oneWireBus = 4;
const int serverPort = 50000;

bool ledEnable = false;

struct {
  byte mac [ 6 ] ;
  byte mode ;
  byte chl ;
  IPAddress ip ;
  IPAddress gw ;
  IPAddress msk ;
  IPAddress dns ;
  uint16_t localPort;
  unsigned char round;
  uint32_t chk ;
} cfgbuf;


uint32_t readcfg(void) {
  uint32_t x = 0 ;
  uint32_t *p = (uint32_t *)cfgbuf.mac ;
  ESP.rtcUserMemoryRead(0 ,p ,sizeof(cfgbuf));
  for (uint32_t i = 0; i < sizeof(cfgbuf)/4; i++) x += p[i];
  if (x) {
    for (uint32_t i = 0; i < 6; i++) cfgbuf.mac[i] = 0xff;
    cfgbuf.chl = 0;
    cfgbuf.ip = IPAddress(0, 0, 0, 0);
    cfgbuf.gw = IPAddress(0, 0, 0, 0);
    cfgbuf.msk = IPAddress(255, 255, 255, 0);
    cfgbuf.dns = IPAddress(0, 0, 0, 0);
    cfgbuf.localPort = 10000;
    cfgbuf.round = 0;
    x = 1;
  }
  return !x;
}


void writecfg(void) {
  int x = 0;
  static struct station_config conf;
  wifi_station_get_config(&conf);
  // save new info
  for (uint32_t i = 0; i < sizeof(conf.bssid); i++) cfgbuf.mac[i] = conf.bssid[i];
  cfgbuf.chl = wifi_get_channel();
  cfgbuf.ip = WiFi.localIP();
  cfgbuf.gw = WiFi.gatewayIP();
  cfgbuf.msk = WiFi.subnetMask();
  cfgbuf.dns = WiFi.dnsIP();
  // recalculate checksum
  uint32_t *p = (uint32_t *)cfgbuf.mac;
  for (uint32_t i = 0; i < sizeof(cfgbuf)/4-1 ; i++) x += p[i];
  cfgbuf.chk =- x ;
  ESP.rtcUserMemoryWrite(0, p, sizeof(cfgbuf));
}


void setDebugLED(bool on){
  if(ledEnable)
    digitalWrite(LED, on?LOW:HIGH);
}


void setup() {
  digitalWrite(LED, HIGH);
  pinMode(LED, OUTPUT);
  
  int mode = RESTORE_MODE;
  if(!readcfg())
    mode = 0;
    
  Serial.begin(74880);
  if(cfgbuf.round < MAX_ROUND){
    cfgbuf.round++;
    Serial.printf("Round %i: Enable Debug LED\n", cfgbuf.round);
    ledEnable=true;
  }else
  
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

    switch (mode) {
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
      break; }
    default:
      bool ok = WiFi.config(cfgbuf.ip, cfgbuf.gw, cfgbuf.msk, cfgbuf.dns);
      if (!ok) Serial.print("*** Wifi.config failed");
      WiFi.begin(SSID, PASSWORD, cfgbuf.chl, cfgbuf.mac);
      break;
  }

  OneWire oneWire(oneWireBus);
  DallasTemperature sensors(&oneWire);
  sensors.requestTemperatures();
  float temperature = sensors.getTempCByIndex(0);

  /*Solange keineVerbindung zu einem AccessPoint (AP) aufgebaut wurde*/
  bool ledon = 1;
  while (WiFi.status() != WL_CONNECTED) {
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
  for(i=RETRIES; !client.connect(HOST, serverPort)&& (i>0); i-- ) {
    Serial.print("X");
    delay(1000);
  } 
  if(!i)
    return;
  

  Serial.print("Connected to ");
  Serial.println(HOST);

  client.print(ROOM);
  client.print(": ");
  client.println(temperature);

  client.flush();
  client.stop();

  writecfg();
  
  delay(100);
  Serial.println("Connection Closed");
}

void loop() {
  WiFi.disconnect();
  setDebugLED(false);
  ESP.deepSleep(50e6);
  delay(100);
}
