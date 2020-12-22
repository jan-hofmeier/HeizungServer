package de.recondita.heizung.server.network;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import de.recondita.heizung.server.control.Ventil;

public class MqttListener implements Closeable{
	
	private final static Logger LOGGER = Logger.getLogger(MqttListener.class.getName());
	
	MqttClient mqttClient;
	
	public MqttListener(String broker, String clientId) throws MqttException {
		mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());
		mqttClient.connect();
	}
	
	public void subscribeValve(Ventil valve) throws MqttException {
		String topic = "home/" + valve.getName().toLowerCase() + "/sensor/";
		mqttClient.subscribe(topic + "temprature", new IMqttMessageListener() {
			
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				LOGGER.log(Level.INFO,"MQTT: " + topic + " " + message.toString());
				valve.setCurrentTemp(Float.parseFloat(message.toString()));
			}
		});
		
		mqttClient.subscribe(topic + "humidity", new IMqttMessageListener() {
			
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				LOGGER.log(Level.INFO,"MQTT: " + topic + " " + message.toString());
				valve.setCurrentHumidity(Float.parseFloat(message.toString()));
			}
		});
	}

	public void subscribeValves(Iterable<Ventil> valves) throws MqttException {
		for(Ventil v: valves)
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
