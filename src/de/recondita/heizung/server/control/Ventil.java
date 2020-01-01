package de.recondita.heizung.server.control;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

public class Ventil {

	private boolean gpioOn = false;
	private boolean planOn = false;
	private Mode mode = Mode.AUTO;
	private String name;
	private int gpio;
	private Ventilverwalter ventilverwalter;

	private static final Object lock = new Object();
	
	private final static Logger LOGGER = Logger
			.getLogger(Ventilverwalter.class.getName());

	public Ventil(int gpio, String name, Ventilverwalter ventilverwalter) {
		this.gpio = gpio;
		this.name = name;
		this.ventilverwalter = ventilverwalter;
		setValue();
	}

	public String getName() {
		return name;
	}

	void setPlanOn(boolean on) {
		this.planOn = on;
		setValue();
	}

	public synchronized void override(Mode mode) {
		this.mode = mode;
		setValue();
	}

	public Mode getMode() {
		return this.mode;
	}

	public boolean getPlanOn() {
		return planOn;
	}

	private void setValue() {
		synchronized (lock) {
			gpioOn = mode == Mode.ON || (mode == Mode.AUTO && planOn);
			ventilverwalter.notifyChange(this);
			if (gpio >= 0)
				try (FileWriter fw = new FileWriter("/sys/class/gpio/gpio" + gpio + "/value");) {
					fw.write(gpioOn ? "1" : "0");
				} catch (IOException e) {
					e.printStackTrace();
				}
			LOGGER.info("Schalte Ventil " + name + " " + gpioOn);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	int getGpio() {
		return gpio;
	}

	void setGpio(int gpio) {
		this.gpio = gpio;
	}

	public boolean isOn() {
		return gpioOn;
	}
}
