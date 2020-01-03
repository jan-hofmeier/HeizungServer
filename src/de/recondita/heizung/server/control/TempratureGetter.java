package de.recondita.heizung.server.control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.HashedMap;

public class TempratureGetter {

	private final static Logger LOGGER = Logger.getLogger(TempratureGetter.class.getName());
	private static Runtime rt = Runtime.getRuntime();

	public Map<String, Float> getTempratures() {
		Map<String, Float> temps = new HashedMap<>();

		try {
			Process pr = rt.exec("python3 /home/heizung/printtemp.py");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
					BufferedReader errReader = new BufferedReader(new InputStreamReader(pr.getErrorStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					LOGGER.log(Level.INFO, "getTemprature: " + line);
					String[] parts = line.split(":");
					if (parts.length == 2)
						try {
							temps.put(parts[0], new Float(parts[1].trim()));
						} catch (NumberFormatException e) {
							LOGGER.log(Level.WARNING, e.getMessage(), e);
						}
				}
				while ((line = errReader.readLine()) != null) {
					LOGGER.log(Level.SEVERE,line);
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			e.printStackTrace();
		}

		return temps;
	}
}
