package de.recondita.heizung.server.control;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

public class Ventilverwalter {

	private static final Ventilverwalter INSTANCE = new Ventilverwalter();

	private final HashMap<Integer, Ventil> gpioMap = new HashMap<Integer, Ventil>();
	private final HashMap<String, Ventil> nameMap = new HashMap<String, Ventil>();

	private Ventilverwalter() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Collection<Integer> c = gpioMap.keySet();
				try (FileWriter unexport = new FileWriter("/sys/class/gpio/unexport");) {
					for (int gpio : c) {
						unexport.write(Integer.toString(gpio));
						unexport.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static Ventilverwalter getInstance() {
		return INSTANCE;
	}

	public void createVentil(int pin, String name) {
		Ventil v = getVentilByName(name);
		if (v != null) {
			int altpin = v.getGpio();
			if (v.getGpio() != pin) {
				gpioMap.remove(altpin);
				disable(altpin);
				enable(pin);
				v.setGpio(pin);
				getVentilByPin(pin).setGpio(-1);
				gpioMap.put(pin, v);
			}
		} else {
			v = getVentilByPin(pin);
			if (v != null) {
				v.setGpio(-1);
			}
			enable(pin);
			v = new Ventil(pin, name);
			gpioMap.put(pin, v);
			nameMap.put(name, v);
		}
	}

	private void enable(int pin) {
		try (FileWriter export = new FileWriter("/sys/class/gpio/export");) {
			export.write(Integer.toString(pin));
			try(FileWriter out=new FileWriter("/sys/class/gpio/gpio"+pin+"/direction");)
			{
				out.write("out");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void disable(int pin) {
		try (FileWriter unexport = new FileWriter("/sys/class/gpio/unexport");) {
			unexport.write(Integer.toString(pin));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Ventil getVentilByPin(int pin) {
		return gpioMap.get(pin);
	}

	public Ventil getVentilByName(String name) {
		return nameMap.get(name);
	}

}
