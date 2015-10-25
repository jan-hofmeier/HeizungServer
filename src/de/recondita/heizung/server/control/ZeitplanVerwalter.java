package de.recondita.heizung.server.control;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;

public class ZeitplanVerwalter implements Closeable {
	private ArrayList<Ventil> ventile = new ArrayList<Ventil>();
	private ArrayList<Zeitplan> zeitPlaene = new ArrayList<Zeitplan>();

	public void addVentil(Ventil v) {
		synchronized (ventile) {
			ventile.add(v);
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (zeitPlaene) {
			for(Zeitplan z: zeitPlaene)
				z.close();
		}

	}

}
