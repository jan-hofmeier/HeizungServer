package de.recondita.heizung.server.control;

import java.util.HashMap;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class Ventilverwalter {

	private static final Ventilverwalter INSTANCE = new Ventilverwalter();

	private final GpioController gpioController = GpioFactory.getInstance();
	private final HashMap<Integer, Ventil> ventile = new HashMap<Integer, Ventil>();

	private Ventilverwalter() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				gpioController.shutdown();
			}
		});
	}

	public static Ventilverwalter getInstance() {
		return INSTANCE;
	}

	public void createVentil(int pin, String name) {
		Ventil ventil = ventile.get(pin);
		if (ventil == null) {
			GpioPinDigitalOutput gpio = gpioController.provisionDigitalOutputPin(RaspiPin.getPinByName("GPIO " + pin),
					name, PinState.LOW);
			ventile.put(pin, new Ventil(gpio, name));
		} else {
			ventil.reset(name);
		}
	}

	public Ventil getVentil(int pin) {
		return ventile.get(pin);
	}

}
