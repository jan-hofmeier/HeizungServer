package de.recondita.heizung.server.googleservices;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

public class GoogleCalendar {

	private static final String APPLICATION_NAME = "Heizung";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	public GoogleCalendar() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws GeneralSecurityException, IOException {
		final GoogleCredentials credential = ServiceAccountCredentials
				.fromStream(new FileInputStream("config/google-credentials.json"))
				.createScoped(CalendarScopes.CALENDAR_READONLY);

		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

		Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credential))
				.setApplicationName(APPLICATION_NAME).build();

		long mills = System.currentTimeMillis();
		DateTime min = new DateTime(mills);
		DateTime max = new DateTime(mills+ 60 *100);
		Events events = service.events().list("isunrle4o51q24ofd21htinem4@group.calendar.google.com").setTimeMin(min)
				.setTimeMax(max)
				.setSingleEvents(true)
				.execute();

		for (Event event : events.getItems()) {
			DateTime start = event.getStart().getDateTime();
			if (start == null) {
				start = event.getStart().getDate();
			}
			System.out.printf("%s (%s)\n", event.getSummary(), start);
		}

	}

}
