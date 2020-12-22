package de.recondita.heizung.server.network;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import de.recondita.heizung.server.control.Ventil;

public class MqttListener implements Closeable {

	private final static Logger LOGGER = Logger.getLogger(MqttListener.class.getName());

	private MqttClient mqttClient;

	public MqttListener(String[] config, Iterable<Ventil> valves) throws MqttException {
		mqttClient = new MqttClient(config[0], config[1], new MemoryPersistence());
		mqttClient.setCallback(new MqttCallbackExtended() {

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				LOGGER.log(Level.INFO, "MQTT: " + topic + " " + message.toString());
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				// TODO Auto-generated method stub

			}

			@Override
			public void connectionLost(Throwable cause) {
				LOGGER.log(Level.SEVERE, "MQTT: connection lost");
			}

			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
				try {
					subscribeValves(valves);
				} catch (MqttException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
				LOGGER.log(Level.SEVERE, "MQTT: connection complete");
			}
		});
		MqttConnectOptions conOpt = new MqttConnectOptions();
		conOpt.setAutomaticReconnect(true);
		conOpt.setCleanSession(true);
		if (config[2] != null) {
			conOpt.setUserName(config[1]);
			conOpt.setPassword(config[2].toCharArray());
		}
		mqttClient.connect(conOpt);
	}

	private void subscribeValve(Ventil valve) throws MqttException {
		String basetopic = "home/" + valve.getName().toLowerCase() + "/sensor/";
		String tempTopic = basetopic + "temprature";
		String humTopic = basetopic + "humidity";

		LOGGER.log(Level.INFO, "MQTT: subscribing to: " + tempTopic);
		mqttClient.subscribe(tempTopic, new IMqttMessageListener() {
			@Override
			public void messageArrived(String topic, MqttMessage message) {
				LOGGER.log(Level.INFO, "MQTT: " + topic + " " + message.toString());
				try {
					valve.setCurrentTemp(Float.parseFloat(message.toString()));
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		});

		LOGGER.log(Level.INFO, "MQTT: subscribing to: " + humTopic);
		mqttClient.subscribe(humTopic, new IMqttMessageListener() {

			@Override
			public void messageArrived(String topic, MqttMessage message) {
				LOGGER.log(Level.INFO, "MQTT: " + topic + " " + message.toString());
				try {
					valve.setCurrentHumidity(Float.parseFloat(message.toString()));
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		});
	}

	private void subscribeValves(Iterable<Ventil> valves) throws MqttException {
		for (Ventil v : valves)
			subscribeValve(v);
	}

	@Override
	public void close() throws IOException {
		try {
			mqttClient.close();
		} catch (MqttException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}
}
