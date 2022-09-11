package de.recondita.heizung.server.control;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.gson.JsonObject;

public class Ventil {

	private volatile boolean gpioOn = false;
	private boolean planOn = false;
	private Mode mode = Mode.PLAN;
	private String name;
	private int gpio;
	private long lastChanged;
	private volatile float currentTemp;
	private volatile long lastTempUpdate;
	private float targetTemp;
	private volatile float currentHumidity = -1;

	private final Object lock = new Object();

	private final static Logger LOGGER = Logger.getLogger(Ventilverwalter.class.getName());

	public Ventil(int gpio, String name) {
		this.gpio = gpio;
		this.name = name;
		setValue();
	}

	public String getName() {
		return name;
	}

	public void setPlanOn(boolean on) {
		if (on != planOn) {
			this.planOn = on;
			lastChanged = System.currentTimeMillis();
			setValue();
		}
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
			gpioOn = mode == Mode.ON || (mode == Mode.PLAN && planOn);
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

	public float getTargetTemp() {
		return targetTemp;
	}

	public void setTargetTemp(float targetTemp) {
		this.targetTemp = targetTemp;
	}

	public long getLastChanged() {
		return lastChanged;
	}

	public long getLastTempUpdate() {
		return lastTempUpdate;
	}
	
	public float getCurrentTemp() {
		return currentTemp;
	}

	public void setCurrentTemp(float currentTemp) {
		this.currentTemp = currentTemp;
		this.lastTempUpdate = System.currentTimeMillis();
	}
	
	public float getCurrentHumidity() {
		return currentHumidity;
	}

	public void setCurrentHumidity(float currentHumidity) {
		this.currentHumidity = currentHumidity;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("currentTemprature", getCurrentTemp());
		json.addProperty("lastTempUpdate", getLastTempUpdate());
		json.addProperty("targetTemprature", getTargetTemp());
		json.addProperty("mode", getMode().name());
		json.addProperty("planOn", getPlanOn());
		json.addProperty("on", isOn());
		return json;
	}
	
}
