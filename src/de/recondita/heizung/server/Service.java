package de.recondita.heizung.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.xml.sax.SAXException;

import de.recondita.heizung.server.control.Ventilverwalter;
import de.recondita.heizung.server.network.MqttListener;
import de.recondita.heizung.server.network.NetworkControl;
import de.recondita.heizung.server.network.TempratureReceiver;
import de.recondita.heizung.server.network.TempratureReceiver.TempratureCallBack;
import de.recondita.heizung.server.verwalter.ZeitplanVerwalter;
import de.recondita.heizung.xml.ConfigLoader;
import de.recondita.heizung.xml.ConfigLoader.PunktOrderException;

public class Service implements Daemon {

	private ZeitplanVerwalter zeitplanverwalter;
	private static Ventilverwalter ventilverwalter = Ventilverwalter
			.getInstance();
	private NetworkControl networkControl;
	
	private TempratureReceiver tempratureReceiver;
	
	private MqttListener mqttListener;

	private final static Logger LOGGER = Logger.getLogger(Service.class.getName());

	public static void main(String[] args) throws FileNotFoundException,
			XPathExpressionException, IOException, SAXException,
			PunktOrderException, ParserConfigurationException {
		createZeitplanVerwalter(args).start();
	}

	@Override
	public void destroy() {

	}

	@Override
	public void init(DaemonContext context) throws Exception {
		try {
			this.zeitplanverwalter = createZeitplanVerwalter(context
					.getArguments());
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
		try {
			networkControl = new NetworkControl(80,ventilverwalter);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		try {
			tempratureReceiver = new TempratureReceiver(new TempratureCallBack() {
				
				@Override
				public void updateTemp(String room, float temp) {
					ventilverwalter.setTemprature(room,temp);					
				}
				
				@Override
				public void updateHumidity(String room, float temp) {
					ventilverwalter.setHumidity(room,temp);
				}
			});
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	private static ZeitplanVerwalter createZeitplanVerwalter(String[] args)
			throws FileNotFoundException, XPathExpressionException,
			IOException, SAXException, PunktOrderException,
			ParserConfigurationException {
		return new ZeitplanVerwalter(ventilverwalter, new ConfigLoader(new File(
				args.length == 0 ? "config" : args[0])));
	}

	@Override
	public void start() throws Exception {
		try {
			zeitplanverwalter.start();
			tempratureReceiver.startListener();
			networkControl.start();
			mqttListener = new MqttListener("localhost", "heizung");
			mqttListener.subscribeValves(ventilverwalter);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}

	}

	@Override
	public void stop() throws Exception {
		try {
			tempratureReceiver.close();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
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
