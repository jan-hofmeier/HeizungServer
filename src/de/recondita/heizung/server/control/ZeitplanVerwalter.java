package de.recondita.heizung.server.control;

import java.util.ArrayList;

public class ZeitplanVerwalter {
	private ArrayList<Ventil> ventile;
	private ArrayList<Zeitplan> zeitPlaene;
	
	public ZeitplanVerwalter(ArrayList<Ventil> ventile,
			ArrayList<Zeitplan> zeitPlaene) {
		this.ventile = ventile;
		this.zeitPlaene = zeitPlaene;
	}
	
	
}
