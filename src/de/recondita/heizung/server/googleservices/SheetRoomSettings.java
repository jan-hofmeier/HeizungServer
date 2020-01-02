package de.recondita.heizung.server.googleservices;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import de.recondita.heizung.ical.HttpIcal;

public class SheetRoomSettings {

	private final static Logger LOGGER = Logger.getLogger(HttpIcal.class.getName());

	private static final String APPLICATION_NAME = "Heizung";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	private String sheetId;
	private Sheets service;
	private List<Room> rooms;

	public SheetRoomSettings(InputStream googleCredentials, String sheetId)
			throws FileNotFoundException, IOException, GeneralSecurityException {
		LOGGER.fine("Create SheetRoomSettings for " + sheetId);
		this.sheetId = sheetId;

		final GoogleCredentials credential = ServiceAccountCredentials.fromStream(googleCredentials)
				.createScoped(SheetsScopes.SPREADSHEETS);

		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

		service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credential))
				.setApplicationName(APPLICATION_NAME).build();
	}

	private static float getOrDefault(List<Object> list, int index, float defaultValue) {
		if (list.size() >= index)
			return defaultValue;
		String str = list.get(index).toString().trim();
		if ("".equals(str))
			return defaultValue;
		try {
			return Float.parseFloat(str);
		} catch (NumberFormatException e) {
			LOGGER.log(Level.INFO, e.getMessage(), e);
			return defaultValue;
		}
	}

	public List<Room> getConfig() {
		ValueRange response;
		try {
			response = service.spreadsheets().values().get(sheetId, "Räume!A1:D").execute();
		} catch (IOException e) {
			LOGGER.log(Level.INFO, e.getMessage(), e);
			return rooms; // last good result
		}

		List<List<Object>> values = response.getValues();
		rooms = new ArrayList<>(values.size());
		for (List<Object> row : values) {
			LOGGER.fine("Got row: " + row);
			String name = row.get(0).toString().trim();
			if ("".equals(name))
				continue;
			float onTemp = getOrDefault(row, 1, Float.MAX_VALUE);
			float offTemp = getOrDefault(row, 1, Float.MIN_VALUE);

			String[] schedules = row.size() < 4 ? new String[0] : row.get(3).toString().toLowerCase().split(" ");

			rooms.add(new Room(name, onTemp, offTemp, schedules));
		}
		return rooms;
	}
}
