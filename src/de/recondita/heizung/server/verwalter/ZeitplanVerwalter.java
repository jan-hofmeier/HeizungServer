package de.recondita.heizung.server.verwalter;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import de.recondita.heizung.ical.HttpIcal;
import de.recondita.heizung.server.control.Ventil;
import de.recondita.heizung.server.control.Ventilverwalter;
import de.recondita.heizung.server.control.Zeitplan;
import de.recondita.heizung.server.googleservices.Room;
import de.recondita.heizung.server.googleservices.SheetRoomSettings;
import de.recondita.heizung.xml.ConfigLoader;
import de.recondita.heizung.xml.ConfigLoader.PunktOrderException;

public class ZeitplanVerwalter implements Closeable {
	private Ventilverwalter ventile;
	private ConfigLoader configurationLoader;
	private ArrayList<Zeitplan> zeitPlaene;
	private ScheduledThreadPoolExecutor timer;
	private HttpIcal[] iCalPlaene;
	private SheetRoomSettings roomSettings;

	private final static Logger LOGGER = Logger.getLogger(ZeitplanVerwalter.class.getName());

	public ZeitplanVerwalter(Ventilverwalter ventilverwalter, ConfigLoader configurationLoader)
			throws FileNotFoundException, XPathExpressionException, IOException, SAXException, PunktOrderException {
		this.ventile = ventilverwalter;
		this.configurationLoader = configurationLoader;
		this.configurationLoader.loadVentile(ventile);

		try {
			iCalPlaene = configurationLoader.loadIcal();
			roomSettings = this.configurationLoader.loadSheetRoomSettings();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			LOGGER.info("Fallback to XML schedule");
			this.zeitPlaene = this.configurationLoader.loadZeitplaene(ventilverwalter);
		}
	}

	@Override
	public void close() throws IOException {
		if (timer != null)
			timer.shutdownNow();
	}

	private void checkXML() {
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

	private void checkICal() {
		Set<String> activeSchedules = new HashSet<>();
		for (HttpIcal ical : iCalPlaene) {
			activeSchedules.addAll(ical.getActiveGroups());
		}

		activeSchedules.add("an");
		activeSchedules.add("on");

		for (Room room : roomSettings.getConfig()) {
			boolean active = false;
			for (String schedule : room.getPlans())
				active |= activeSchedules.contains(schedule);
			Ventil ventil = ventile.getVentilByName(room.getName());
			if (ventil != null)
				ventil.setPlanOn(active);
			else
				LOGGER.log(Level.SEVERE, "Can't find valve " + room.getName());
		}
	}

	private void check() {
		if (iCalPlaene == null)
			checkXML();
		else
			checkICal();
	}

	private void setFatalError() {
		// TODO
	}

	public void start() {
		timer = new ScheduledThreadPoolExecutor(1);
		timer.scheduleAtFixedRate(() -> check(), 0, 1, TimeUnit.MINUTES);
	}
}
