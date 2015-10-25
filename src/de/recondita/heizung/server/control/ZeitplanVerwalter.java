package de.recondita.heizung.server.control;

import java.util.ArrayList;

public class ZeitplanVerwalter {
	private ArrayList<Ventil> ventile = new ArrayList<Ventil>();
	private ArrayList<Zeitplan> zeitPlaene = new ArrayList<Zeitplan>();

	public void addVentil(Ventil v) {
		synchronized (ventile) {
			ventile.add(v);
		}
	}

}
