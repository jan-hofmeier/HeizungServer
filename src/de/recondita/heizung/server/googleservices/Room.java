package de.recondita.heizung.server.googleservices;

public class Room {

	private String name;
	private float offtemp;
	private float ontemp;
	private Activation activations[];
	
	Room(String name, float ontemp, float offtemp, Activation[] activations) {
		super();
		this.name = name;
		this.ontemp = ontemp;
		this.offtemp = offtemp;
		this.activations = activations;
	}

	public String getName() {
		return name;
	}

	public float getOntemp() {
		return ontemp;
	}

	public float getOfftemp() {
		return offtemp;
	}

	public Activation[] getActivations() {
		return activations;
	}
}
