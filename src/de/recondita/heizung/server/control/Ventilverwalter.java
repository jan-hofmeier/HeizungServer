package de.recondita.heizung.server.control;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class Ventilverwalter {

	private static final Ventilverwalter INSTANCE = new Ventilverwalter();

	private final HashMap<Integer, Ventil> gpioMap = new HashMap<Integer, Ventil>();
	private final HashMap<String, Ventil> nameMap = new HashMap<String, Ventil>();
	
	private ArrayList<VentilStateListener> listener=new ArrayList<VentilStateListener>();

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

	public synchronized void createVentil(int pin, String name) throws IOException {
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
			} else
				enable(pin);
			v = new Ventil(pin, name,this);
			gpioMap.put(pin, v);
			nameMap.put(name, v);
			System.out.println("Ventil " + name + " erstelt");
		}
	}

	private void enable(int pin) throws IOException {
		try (FileWriter export = new FileWriter("/sys/class/gpio/export");) {
			export.write(Integer.toString(pin));
		}
		while (!new File("/sys/class/gpio/gpio" + pin).exists())
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
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

	public synchronized void shutdown() {
		Collection<Integer> c = gpioMap.keySet();
		try (FileWriter unexport = new FileWriter("/sys/class/gpio/unexport");) {
			for (int gpio : c) {
				unexport.write(Integer.toString(gpio));
				unexport.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		gpioMap.clear();
		nameMap.clear();
	}
	
	void notifyChange(Ventil v){
		synchronized(listener){
			for(VentilStateListener l: listener){
				l.stateChanged(v.getName(), v.getMode(), v.isOn());
			}
		}
	}
	
	public void addChangeListener(VentilStateListener l){
		synchronized(listener){
			listener.add(l);
		}
	}
}
