package de.recondita.heizung.server.control;

import java.io.FileWriter;
import java.io.IOException;

public class Ventil {

	private boolean gpioOn = false;
	private boolean planOn = false;
	private Mode mode = Mode.AUTO;
	private String name;
	private Zeitplan zeitplan;
	private int gpio;

	private static final Object lock = new Object();

	public Ventil(int gpio, String name) {
		this.gpio = gpio;
		this.name = name;
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
			if (gpio >= 0)
				try (FileWriter fw = new FileWriter("/sys/class/gpio/gpio" + gpio + "/direction");) {
					fw.write(gpioOn ? 1 : 0);
					Thread.sleep(1000);
				} catch (IOException | InterruptedException e) {
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

	public synchronized void setZeitplan(Zeitplan zp) {
		removeFromZeitplan();
		this.zeitplan = zp;
		zp.addVentil(this);
	}

	public Zeitplan getZeitplan() {
		return this.zeitplan;
	}

	public synchronized void removeFromZeitplan() {
		if (zeitplan != null) {
			zeitplan.removeVentil(this);
			setZeitplan(null);
		}
	}
}
