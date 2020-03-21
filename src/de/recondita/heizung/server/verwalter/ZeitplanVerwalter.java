package de.recondita.heizung.server.verwalter;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import com.opencsv.exceptions.CsvException;

import de.recondita.heizung.ical.HttpIcal;
import de.recondita.heizung.server.control.TempratureGetter;
import de.recondita.heizung.server.control.Ventil;
import de.recondita.heizung.server.control.Ventilverwalter;
import de.recondita.heizung.server.control.Zeitplan;
import de.recondita.heizung.server.googleservices.Activation;
import de.recondita.heizung.server.googleservices.Room;
import de.recondita.heizung.server.googleservices.SheetRoomSettings;
import de.recondita.heizung.xml.ConfigLoader;
import de.recondita.heizung.xml.ConfigLoader.PunktOrderException;
import net.fortuna.ical4j.data.ParserException;

public class ZeitplanVerwalter implements Closeable {
	private Ventilverwalter ventile;
	private ConfigLoader configurationLoader;
	private ArrayList<Zeitplan> zeitPlaene;
	private ScheduledThreadPoolExecutor timer;
	private HttpIcal[] iCalPlaene;
	private SheetRoomSettings roomSettings;
	private TempratureGetter thermometers;

	private final static Logger LOGGER = Logger.getLogger(ZeitplanVerwalter.class.getName());

	public ZeitplanVerwalter(Ventilverwalter ventilverwalter, ConfigLoader configurationLoader)
			throws FileNotFoundException, XPathExpressionException, IOException, SAXException, PunktOrderException {
		this.ventile = ventilverwalter;
		this.configurationLoader = configurationLoader;
		this.configurationLoader.loadVentile(ventile);
		thermometers = configurationLoader.loadTempratureGetter();
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

	private void checkICal() throws IOException, ParserException, CsvException {
		Set<String> activeSchedules = new HashSet<>();
		for (HttpIcal ical : iCalPlaene) {
			activeSchedules.addAll(ical.getActiveGroups());
		}

		activeSchedules.add("an");
		activeSchedules.add("on");

		Map<String, Float> tempratures = thermometers.getTempratures();
		long now = System.currentTimeMillis();

		for (Room room : roomSettings.getRoomSettings()) {

			float activeTemp = room.getOntemp();
			boolean active = activeSchedules.contains(room.getName().toLowerCase());

			if (!active)
				for (Activation activation : room.getActivations()) {
					if (activeSchedules.contains(activation.getName())) {
						activeTemp = activation.getTemp();
						active = true;
					}
				}

			Ventil ventil = ventile.getVentilByName(room.getName());
			if (ventil != null) {
				Float currentTempUpdate = tempratures.get(room.getName());
				if (currentTempUpdate != null && !currentTempUpdate.isNaN())
					ventil.setCurrentTemp(currentTempUpdate);

				if ((now - ventil.getLastTempUpdate()) > 15 * 60000)
					ventil.setPlanOn(active);
				else {
					float currentTemp = ventil.getCurrentTemp();
					float targetTemp = active ? activeTemp : room.getOfftemp();
					boolean targetChanged = targetTemp != ventil.getTargetTemp();
					ventil.setTargetTemp(targetTemp);
					LOGGER.log(Level.INFO,
							room.getName() + ": Target Temp: " + targetTemp + " Current Temp: " + currentTemp);

					if (ventil.getPlanOn() && now - ventil.getLastChanged() > 15 * 60000
							|| !ventil.getPlanOn() && now - ventil.getLastChanged() > 30 * 60000 || targetChanged) {

						if (Math.abs(targetTemp - currentTemp) < 0.1)
							ventil.setPlanOn(!ventil.getPlanOn());
						else
							ventil.setPlanOn(targetTemp > currentTemp);
					}
					
					if (Math.abs(targetTemp - currentTemp) > 0.2)
						ventil.setPlanOn(targetTemp > currentTemp);
				}
			} else
				LOGGER.log(Level.WARNING, "Can't find valve " + room.getName());
		}
	}

	private void check() {
		try {
			if (iCalPlaene == null)
				checkXML();
			else
				checkICal();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	private void setFatalError() {
		// TODO
	}

	public void start() {
		timer = new ScheduledThreadPoolExecutor(1);
		timer.scheduleAtFixedRate(() -> check(), 0, 1, TimeUnit.MINUTES);
	}
}
