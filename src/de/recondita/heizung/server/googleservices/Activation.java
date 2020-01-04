package de.recondita.heizung.server.googleservices;

public class Activation {

	private String name;
	private float temp;
	
	public Activation(String name, float temp) {
		this.name = name;
		this.temp = temp;
	}
	
	public String getName() {
		return name;
	}
	
	public float getTemp() {
		return temp;
	}
	

}
