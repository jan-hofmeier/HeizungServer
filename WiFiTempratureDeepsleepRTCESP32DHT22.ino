#include "DHT.h"
#include "BLEDevice.h" 
#include <WiFi.h>
#include "wifisettings.h"

#define LED 2 //LED_BUILTIN
#define DHTTYPE DHT22
#define DHTPin 4 
#define RETRIES 5
#define RESTORE_MODE 3

#define MAX_ROUND 5000

const int oneWireBus = 4;
const int serverPort = 50000;

bool ledEnable = false;

void IRAM_ATTR resetModule(){
    ets_printf("reboot\n");
    ESP.restart();
    //esp_restart_noos();
}

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
  if (x == 10 && cfgbuf.ip != 0)
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

void wifiBegin(byte mode){
    Serial.print("Wifi Begin mode:");
    Serial.println(mode);
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
        break;
      }
    default:
      bool ok = WiFi.config(cfgbuf.ip, cfgbuf.gw, cfgbuf.msk, cfgbuf.dns);
      if (!ok) Serial.print("*** Wifi.config failed");
      WiFi.begin(SSID, PASSWORD, cfgbuf.chl, cfgbuf.mac);
      break;
  }
}

BLEClient* thisOurMicrocontrollerAsClient = BLEDevice::createClient();

String receivedTemperatureValue = "";
static void notifyAsEachTemperatureValueIsReceived(BLERemoteCharacteristic* pBLERemoteCharacteristic, uint8_t* receivedNotification, size_t length, bool isNotify) { // The temperature measurement value broadcasts are notifications that each time that they come fire this function.                                                                                                              // If we already have gotten a valid temperature measurement value (or an error announcement) these is no need to analyze further this newest notificaton
  for (int i = 2; i <= 5; i++) receivedTemperatureValue += (char) * (receivedNotification + i);
}

String readTempAsString(std::string addr) {
  unsigned long startTime;
  Serial.println("Connect to BLE sensor...");
  if (thisOurMicrocontrollerAsClient->isConnected() == false) {
    thisOurMicrocontrollerAsClient->disconnect();  // Here the our ESP32 as a client asks for a connection to the desired target device.
    delay(20);
    thisOurMicrocontrollerAsClient->connect(addr);
    startTime = millis();
  }
  if ( thisOurMicrocontrollerAsClient->isConnected() == false ) {
    return "ERROR: e4 Connection couln't be established"; // Here we check if we succeded to make the connection.
  }

  Serial.println("Getting BLE Service...");
  BLERemoteService* remoteServiceOfTheThermometer = thisOurMicrocontrollerAsClient->getService("226c0000-6476-4566-7562-66734470666d");  // Here we are obtaining a reference to the service that we are after (hosted by the wireless BLE thermometer).
  
  if (remoteServiceOfTheThermometer == nullptr) {
    thisOurMicrocontrollerAsClient->disconnect();
    return "ERROR: e6 ERROR Found the thermometer, but failed to find the needed service. Try again later.";
  }

  Serial.println("Get BLECharacteristic");
  BLERemoteCharacteristic* characteristicOfTheTemperatureMeasurementValue = remoteServiceOfTheThermometer->getCharacteristic("226caa55-6476-4566-7562-66734470666d");  // Here we are obtain a reference to a sensor characteristic (of the service that is hosted by the wireless BLE thermometer).

  if (characteristicOfTheTemperatureMeasurementValue == nullptr) {
    thisOurMicrocontrollerAsClient->disconnect();
    return "ERROR: e8 ERROR Found the thermometer and its service, but failed to find the neede characteristic. Try again later.";
  }

  //Serial.println("Read BLE Value");
  //std::string readTemperatureValue = characteristicOfTheTemperatureMeasurementValue->readValue();
  //return readTemperatureValue.c_str();
  
  Serial.println("Register for notify...");
  characteristicOfTheTemperatureMeasurementValue->registerForNotify(notifyAsEachTemperatureValueIsReceived);
  

  BLERemoteDescriptor* descriptorForStartingAndEndingNotificationsFromCharacteristic = characteristicOfTheTemperatureMeasurementValue->getDescriptor(BLEUUID((uint16_t)0x2902)); // Here we are enabling the notifying of the sensor characteristic (i.e. temperature measurement value).
  
  if (descriptorForStartingAndEndingNotificationsFromCharacteristic == nullptr) {
    thisOurMicrocontrollerAsClient->disconnect();
    return "ERROR: e10 ERROR Found the thermometer, its service and its charasteristic, but failed to find othe needed descriptor at UUID = 0x2902. Try again later.";
  }

  Serial.println("Receive BLE Temprature...");
  receivedTemperatureValue = "";
  uint8_t startNotifications[2] = {0x01, 0x00}; descriptorForStartingAndEndingNotificationsFromCharacteristic->writeValue(startNotifications, 2, false);                                                                                                                                                                     // Ideas: https://stackoverflow.com/questions/1269568/how-to-pass-a-constant-array-literal-to-a-function-that-takes-a-pointer-without
  startTime = millis();
  while ( ( (millis() - startTime) < 5000) && (receivedTemperatureValue.length() < 4) ) {
    if (thisOurMicrocontrollerAsClient->isConnected() == false) {
      return "ERROR: e12 ERROR After succesfully done all the setup we unexpectidly lost the connection. Try moving the thermometer closer to the ESP32."; // https://github.com/nkolban/esp32-snippets/issues/228  https://github.com/nkolban/esp32-snippets/issues/228  https://forum.arduino.cc/index.php?topic=122413.0
    }
  }

  Serial.println("Disconnect BLE");
  characteristicOfTheTemperatureMeasurementValue->registerForNotify(NULL);                                                                                             // Stop reacting to futher notifications...
  uint8_t endNotifications[2] = {0x00, 0x00}; descriptorForStartingAndEndingNotificationsFromCharacteristic->writeValue(endNotifications, 2, false);                   // ...and ask the thermometer to stop sending notifications
  if (receivedTemperatureValue.length() < 4) return "ERROR: e14 No proper temperature measurement value catched.";
  return receivedTemperatureValue;
}

DHT dht(DHTPin, DHTTYPE);

hw_timer_t *timer = NULL;
void setup() {
  digitalWrite(LED, LOW);
  pinMode(LED, OUTPUT);

  timer = timerBegin(0, 80, true); //timer 0, div 80
  timerAttachInterrupt(timer, &resetModule, true);
  timerAlarmWrite(timer, 60*1000000, false); //set time in us
  timerAlarmEnable(timer); //enable interrupt
  
  checkcfg();

  Serial.begin(115200);
  if (cfgbuf.round < MAX_ROUND) {
    cfgbuf.round++;
    Serial.printf("Round %i: Enable Debug LED\n", cfgbuf.round);
    ledEnable = true;
  }

  setDebugLED(true);

  Serial.print("BLE temprature: ");
  BLEDevice::init("Esszimmer");
  String bleTemp = readTempAsString(BLEMAC);
  Serial.println(bleTemp);

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

  wifiBegin(cfgbuf.mode);

  dht.begin();
  
  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();
  Serial.print("Read temprature: ");
  Serial.println(temperature);

  /*Solange keineVerbindung zu einem AccessPoint (AP) aufgebaut wurde*/
  bool ledon = 1;
  int i;
  for (i = 0; (WiFi.status() != WL_CONNECTED) && i<30; i++) {
    if(i%5 == 0)
      wifiBegin(i<10 ? cfgbuf.mode : 0);
    setDebugLED(ledon = !ledon);
    delay(500);
    Serial.print(".");
  }

  if(WiFi.status() != WL_CONNECTED){
    Serial.println("giving up on Wifi");
    return;
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

  for (int i = RETRIES; !client.connect(HOST, serverPort) && (i > 0); i-- ) {
    Serial.print("X");
    delay(1000);
  }
  if (!client.connected())
    return;


  Serial.print("Connected to ");
  Serial.println(HOST);

  client.print(ROOM);
  client.print(": ");
  client.print(temperature);
  client.print('/');
  client.println(humidity);
  client.print(BLEROOM);
  client.print(": ");
  client.print(bleTemp);

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

