package de.recondita.heizung.server.verwalter;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import de.recondita.heizung.server.control.Ventilverwalter;
import de.recondita.heizung.server.control.Zeitplan;
import de.recondita.heizung.xml.XMLLoader;
import de.recondita.heizung.xml.XMLLoader.PunktOrderException;

public class ZeitplanVerwalter implements Closeable {
	private Ventilverwalter ventile;
	private XMLLoader configurationLoader;
	private ArrayList<Zeitplan> zeitPlaene;
	private ScheduledThreadPoolExecutor timer;

	private final static Logger LOGGER = Logger
			.getLogger(ZeitplanVerwalter.class.getName());

	public ZeitplanVerwalter(Ventilverwalter ventilverwalter,
			XMLLoader configurationLoader) throws FileNotFoundException,
			XPathExpressionException, IOException, SAXException,
			PunktOrderException {
		this.ventile = ventilverwalter;
		this.configurationLoader = configurationLoader;
		this.configurationLoader.loadVentile(ventile);
		this.zeitPlaene = this.configurationLoader
				.loadZeitplaene(ventilverwalter);
	}

	@Override
	public void close() throws IOException {
		if (timer != null)
			timer.shutdownNow();
	}

	private void check() {
		LocalDateTime date = LocalDateTime.now();
		int day = date.getDayOfWeek().ordinal();
		LocalTime time = date.toLocalTime();
		try {
			synchronized (zeitPlaene) {
				for (Zeitplan zp : zeitPlaene) {
					zp.check(day, time);
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			setFatalError();
		}
	}

	private void setFatalError() {
		//TODO
	}

	public void start() {
		timer = new ScheduledThreadPoolExecutor(1);
		timer.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				check();
			}
		}, 0, 1, TimeUnit.MINUTES);
	}
}
