package de.recondita.heizung.server;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.eclipse.paho.client.mqttv3.MqttException;

import de.recondita.heizung.server.control.Ventilverwalter;
import de.recondita.heizung.server.network.MqttWrapper;
import de.recondita.heizung.server.network.NetworkControl;
import de.recondita.heizung.server.verwalter.ZeitplanVerwalter;
import de.recondita.heizung.xml.ConfigLoader;

public class Service implements Daemon {

	private ZeitplanVerwalter zeitplanverwalter;
	private static Ventilverwalter ventilverwalter = Ventilverwalter.getInstance();
	private NetworkControl networkControl;

	private MqttWrapper mqttListener;
	private ConfigLoader configLoader;

	private final static Logger LOGGER = Logger.getLogger(Service.class.getName());

	@Override
	public void destroy() {

	}

	@Override
	public void init(DaemonContext context) throws Exception {
		String[] args = context.getArguments();
		this.configLoader = new ConfigLoader(new File(args.length == 0 ? "config" : args[0]));
		try {
			this.zeitplanverwalter = new ZeitplanVerwalter(ventilverwalter, configLoader);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		try {
			networkControl = new NetworkControl(80, ventilverwalter);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@Override
	public void start() throws Exception {
		try {
			zeitplanverwalter.start();
			networkControl.start();
			String[] mqttConfig = configLoader.loadMQTTConfig();
			if (mqttConfig != null)
				while (mqttListener == null) {
					try {
						mqttListener = new MqttWrapper(mqttConfig, ventilverwalter);
					} catch (MqttException e) {
						LOGGER.log(Level.SEVERE, e.getMessage(), e);
						Thread.sleep(10000L);
					}
				}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}

	}

	@Override
	public void stop() throws Exception {
		try {
			networkControl.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		try {
			mqttListener.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		try {
			zeitplanverwalter.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		try {
			ventilverwalter.shutdown();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

	}

}
