package de.recondita.heizung.server.googleservices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

public class SheetRoomSettings {

	private final static Logger LOGGER = Logger.getLogger(SheetRoomSettings.class.getName());

	private String sheetId;
	private Sheets service;
	private File backupFile;
	private List<List<Object>> lastValues;
	private List<Room> rooms;

	public SheetRoomSettings(GoogleCredentials credentials, String applicationName, String sheetId, File backupFile)
			throws FileNotFoundException, IOException, GeneralSecurityException {
		LOGGER.fine("Create SheetRoomSettings for " + sheetId);
		this.sheetId = sheetId;
		this.backupFile = backupFile;
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		credentials = credentials.createScoped(SheetsScopes.SPREADSHEETS);
		final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
		service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
				.setApplicationName(applicationName).build();
		if (backupFile != null && backupFile.exists()) {
			try {
				loadFromBackupFile();
				LOGGER.info("Loaded Backup file " + backupFile.getName());
			} catch (IOException | CsvException e) {
				LOGGER.log(Level.WARNING, "Can't load backup file");
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
		} else {
			LOGGER.info("No Backup file " + backupFile.getName());
		}
	}

	private static float getOrDefault(List<Object> list, int index, float defaultValue) {
		if (list.size() <= index)
			return defaultValue;
		String str = list.get(index).toString().trim();
		if ("".equals(str))
			return defaultValue;
		try {
			return Float.parseFloat(str.replace(',', '.'));
		} catch (NumberFormatException e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
			return defaultValue;
		}
	}

	public void saveToBackupFile(List<List<Object>> values) throws IOException {
		Path backupPath = backupFile.toPath();
		// (backupFile.getAbsolutePath());
		Path tmpFile = backupPath.resolveSibling("~" + backupPath.getFileName());
		try (CSVWriter writer = new CSVWriter(new FileWriter(tmpFile.toFile()))) {
			for (List<Object> line : values) {
				String[] buffer = line.toArray(new String[line.size()]);
				writer.writeNext(buffer);
			}
		}
		Files.move(tmpFile, backupPath, StandardCopyOption.REPLACE_EXISTING);
	}

	private void setRooms(List<List<Object>> values) {
		rooms = new ArrayList<>(values.size());
		for (List<Object> row : values) {
			LOGGER.fine("Got row: " + row);
			String name = row.get(0).toString().trim();
			if ("".equals(name))
				continue;
			float onTemp = getOrDefault(row, 1, Float.MAX_VALUE);
			float offTemp = getOrDefault(row, 2, Float.MIN_VALUE);

			String[] schedules = row.size() < 4 ? new String[0] : row.get(3).toString().toLowerCase().split(" ");
			Activation[] activations = new Activation[schedules.length];

			for (int i = 0; i < schedules.length; i++) {
				String[] parts = schedules[i].split("=");
				float temp = onTemp;
				if (parts.length > 1)
					try {
						temp = Float.parseFloat(parts[1]);
					} catch (NumberFormatException e) {
						LOGGER.log(Level.WARNING, e.getMessage(), e);
					}
				activations[i] = new Activation(parts[0], temp);
			}

			rooms.add(new Room(name, onTemp, offTemp, activations));
		}
		lastValues = values;
	}

	public void loadFromBackupFile() throws FileNotFoundException, IOException, CsvException {
		try (CSVReader reader = new CSVReader(new FileReader(backupFile))) {
			List<List<Object>> values = reader.readAll().stream().map(l -> new ArrayList<Object>(Arrays.asList(l)))
					.collect(Collectors.toList());
			setRooms(values);
		}
	}

	public List<Room> getRoomSettings() throws IOException, CsvException {
		LOGGER.log(Level.INFO, "getting room settings from google");
		List<List<Object>> values;
		try {
			ValueRange response = service.spreadsheets().values().get(sheetId, "Räume!A2:D").execute();
			values = response.getValues();
		} catch (IOException e) {
			LOGGER.log(Level.INFO, e.getMessage(), e);
			if (lastValues != null)
				return rooms; // last good result
			throw e;
		}

		if (values.equals(lastValues))
			return rooms;

		setRooms(values);

		if (backupFile != null) {
			try {
				saveToBackupFile(values);
			} catch (IOException e) {
				LOGGER.severe("Can not save new Room Settings to " + backupFile.getAbsolutePath());
			}
		}

		return rooms;
	}
}
