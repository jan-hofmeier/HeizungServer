package de.recondita.heizung.server.googleservices;

public class Room {

	String name;
	float offtemp;
	Activation activations[];
	
	Room(String name, float offtemp, Activation[] activations) {
		super();
		this.name = name;
		this.offtemp = offtemp;
		this.activations = activations;
	}

	public String getName() {
		return name;
	}

	public float getOfftemp() {
		return offtemp;
	}

	public Activation[] getActivations() {
		return activations;
	}
}
