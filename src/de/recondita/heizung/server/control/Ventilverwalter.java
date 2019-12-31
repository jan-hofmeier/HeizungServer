package de.recondita.heizung.server.control;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Ventilverwalter implements Iterable<Ventil> {

	private static final Ventilverwalter INSTANCE = new Ventilverwalter();

	private final Map<Integer, Ventil> gpioMap = new HashMap<Integer, Ventil>();
	private final Map<String, Ventil> nameMap = new HashMap<String, Ventil>();
	private final Map<String, List<Ventil>> groupMap = new HashMap<String, List<Ventil>>();

	private ArrayList<VentilStateListener> listener = new ArrayList<VentilStateListener>();

	private final static Logger LOGGER = Logger.getLogger(Ventilverwalter.class.getName());

	private Ventilverwalter() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				shutdown();
			}
		});
	}

	public static Ventilverwalter getInstance() {
		return INSTANCE;
	}

	public synchronized void createVentil(int pin, String name, String group) throws IOException {
		Ventil v = getVentilByName(name);
		if (group == null)
			group = name;
		
		if (v != null) {
			LOGGER.severe("Ventil already exists: " + name);
			return;
		}
		v = getVentilByPin(pin);
		if (v != null) {
			LOGGER.severe("GPIO already in use: " + pin);
		} else
			enable(pin);
		v = new Ventil(pin, name, group, this);
		gpioMap.put(pin, v);
		nameMap.put(name, v);
		LOGGER.info("Ventil " + name + " erstelt");

		groupMap.getOrDefault(group, new ArrayList<Ventil>()).add(v);
	}

	private void enable(int pin) throws IOException {
		try (FileWriter export = new FileWriter("/sys/class/gpio/export");) {
			export.write(Integer.toString(pin));
		}
		while (!new File("/sys/class/gpio/gpio" + pin + "/active_low").exists())
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		try (FileWriter activelow = new FileWriter("/sys/class/gpio/gpio" + pin + "/active_low");) {
			activelow.write("1");
		}
		try (FileWriter direction = new FileWriter("/sys/class/gpio/gpio" + pin + "/direction");
				FileWriter value = new FileWriter("/sys/class/gpio/gpio" + pin + "/value");) {
			direction.write("out");
			direction.flush();
			value.write("0");
		}
	}

	public Ventil getVentilByPin(int pin) {
		return gpioMap.get(pin);
	}

	public Ventil getVentilByName(String name) {
		return nameMap.get(name);
	}

	public List<Ventil> getGroup(String group) {
		return groupMap.get(group);
	}

	public synchronized void shutdown() {
		Collection<Integer> c = gpioMap.keySet();
		try (FileWriter unexport = new FileWriter("/sys/class/gpio/unexport");) {
			for (int gpio : c) {
				unexport.write(Integer.toString(gpio));
				unexport.flush();
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		gpioMap.clear();
		nameMap.clear();
	}

	void notifyChange(Ventil v) {
		synchronized (listener) {
			for (VentilStateListener l : listener) {
				l.stateChanged(v.getName(), v.getMode(), v.isOn());
			}
		}
	}

	public void addChangeListener(VentilStateListener l) {
		synchronized (listener) {
			listener.add(l);
		}
	}

	@Override
	public Iterator<Ventil> iterator() {
		return gpioMap.values().iterator();
	}

	public void setActiveGroups(Set<String> activeGroups) {
		for(Ventil v: this) {
			v.setPlanOn(activeGroups.contains(v.getGroup()));
		}
	}
}
