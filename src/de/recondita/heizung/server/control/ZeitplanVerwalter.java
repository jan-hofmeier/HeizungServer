package de.recondita.heizung.server.control;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;

public class ZeitplanVerwalter implements Closeable {
	private ArrayList<Ventil> ventile;
	private ArrayList<Zeitplan> zeitPlaene = new ArrayList<Zeitplan>();

	public ZeitplanVerwalter(ArrayList<Ventil> ventile) {
		this.ventile = ventile;
	}

	public ZeitplanVerwalter() {
		this(new ArrayList<Ventil>());
	}

	@Override
	public void close() throws IOException {
		synchronized (zeitPlaene) {
			for (Zeitplan z : zeitPlaene)
				z.close();
		}

	}

}
