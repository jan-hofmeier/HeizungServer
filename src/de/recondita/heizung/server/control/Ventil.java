package de.recondita.heizung.server.control;

import com.pi4j.io.gpio.GpioPinDigitalOutput;

public class Ventil {

	private boolean gpioOn;
	private boolean planOn;
	private Mode mode;
	private String name;
	private Zeitplan zeitplan;
	private final GpioPinDigitalOutput gpio;

	public Ventil(GpioPinDigitalOutput gpio, String name) {
		this.gpio = gpio;
		reset(name);
	}

	public void reset(String name) {
		this.name = name;
		mode = Mode.AUTO;
		gpioOn = false;
		planOn = false;
		removeFromZeitplan();
	}

	public int getGpio() {
		return gpio.getPin().getAddress();
	}

	public String getName() {
		return name;
	}

	public void setPlanOn(boolean on) {
		this.planOn = on;
		setGPIO();
	}

	public void override(Mode mode) {
		this.mode = mode;
		setGPIO();
	}

	private void setGPIO() {
		gpioOn = mode == Mode.ON || (mode == Mode.AUTO && planOn);
	}

	public boolean isOn() {
		return gpioOn;
	}

	synchronized void setZeitplan(Zeitplan zp) {
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
