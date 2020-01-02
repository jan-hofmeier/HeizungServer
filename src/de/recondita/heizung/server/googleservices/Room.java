package de.recondita.heizung.server.googleservices;

public class Room {

	String name;
	float ontemp;
	float offtemp;
	String plans[];
	
	Room(String name, float ontemp, float offtemp, String[] plans) {
		super();
		this.name = name;
		this.ontemp = ontemp;
		this.offtemp = offtemp;
		this.plans = plans;
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

	public String[] getPlans() {
		return plans;
	}
}
