package de.recondita.heizung.ical;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.filter.Filter;
import net.fortuna.ical4j.filter.PeriodRule;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentGroup;
import net.fortuna.ical4j.model.DateRange;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.util.MapTimeZoneCache;

public class HttpIcal {

	private final static Logger LOGGER = Logger.getLogger(HttpIcal.class.getName());
	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	static {
		System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache.class.getName());
	}

	private URL url;

	private Calendar calendar;
	private String lastCalendarStr;

	private File backupFile;

	private String room;

	public HttpIcal(URL icalUrl, File backupFile, String room) {
		this(icalUrl, backupFile);
		this.room = room.toLowerCase();
	}

	public HttpIcal(URL icalUrl, File backupFile) {
		this.url = icalUrl;
		this.backupFile = backupFile;
		LOGGER.fine("Create HTTP iCal for " + url);
	}

	private static String convert(InputStream inputStream) throws IOException {

		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, DEFAULT_CHARSET))) {
			return br.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	private String requestCalendarStr() throws IOException {
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setInstanceFollowRedirects(true);
		int status = con.getResponseCode();
		if (status != 200) {
			throw new IOException("Non 200 status Code: " + status);
		}
		return convert(con.getInputStream());
	}

	private String readCalendarStrFromFile() throws FileNotFoundException, IOException {
		return convert(new FileInputStream(backupFile));
	}

	private void saveToBackupFile() throws IOException {
		Path backupPath = backupFile.toPath();
		// (backupFile.getAbsolutePath());
		Path tmpFile = backupPath.resolveSibling("~" + backupPath.getFileName());
		try (BufferedWriter bw = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(tmpFile.toFile()), DEFAULT_CHARSET))) {
			bw.write(lastCalendarStr);
		}
		Files.move(tmpFile, backupPath, StandardCopyOption.REPLACE_EXISTING);
	}

	private Calendar getCalendar() throws IOException, ParserException {
		String calendarStr;
		try {
			calendarStr = requestCalendarStr();
		} catch (IOException e) {
			LOGGER.severe("Can not get Calendar from " + url);
			LOGGER.log(Level.WARNING, e.getMessage(), e);
			if (lastCalendarStr == null) {
				if (backupFile != null) {
					LOGGER.info("Try to load Calendar from File " + backupFile.getAbsolutePath());
					try {
						calendarStr = readCalendarStrFromFile();
					} catch (IOException e1) {
						LOGGER.severe("Can not load valendar from file " + backupFile.getAbsolutePath());
						LOGGER.log(Level.WARNING, e1.getMessage(), e1);
						throw e1;
					}
				} else
					throw e;
			} else {
				return calendar;
			}
		}

		// no need to reparse if it didn't change
		if (calendarStr.equals(lastCalendarStr))
			return calendar;

		lastCalendarStr = calendarStr;

		try (Reader reader = new StringReader(calendarStr)) {
			calendar = new CalendarBuilder().build(reader);
			saveToBackupFile();
		} catch (IOException e) {
			LOGGER.severe("Can not save new Calendar to " + backupFile.getAbsolutePath());
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		} catch (ParserException e) {
			LOGGER.severe("Can not parse Calendar from " + url);
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			if (calendar == null)
				throw e;
		}

		return calendar;
	}

	public List<String> getActiveGroups() throws IOException, ParserException {

		Calendar calendar = getCalendar();

		DateTime now = new DateTime(java.util.Calendar.getInstance().getTime());

		Period period = new Period(now, now);
		Predicate<VEvent> periodRule = new PeriodRule<>(period);
		Filter<VEvent> filter = new Filter<>(periodRule);

		Collection<VEvent> eventsNow = filter.filter(calendar.getComponents(Component.VEVENT));

		// Handle Recurrent Events with changed time for single instance
		List<VEvent> removed = new ArrayList<>();
		for (VEvent event : eventsNow) {
			Date start = event.calculateRecurrenceSet(period).stream().findFirst().get().getStart();
			ComponentGroup<VEvent> group = new ComponentGroup<>(calendar.getComponents(Component.VEVENT),
					event.getUid());
			for (VEvent revision : group.getRevisions()) {
				RecurrenceId recurrenceid = revision.getRecurrenceId();
				if (recurrenceid != null && start.equals(recurrenceid.getDate()) && !period.intersects(
						new DateRange(revision.getStartDate().getDate(), revision.getEndDate().getDate()))) {
					removed.add(event);
					LOGGER.log(Level.FINE, "Found moved event: " + event.getName());
				}
			}
		}
		eventsNow.removeAll(removed);

		if(room != null && !eventsNow.isEmpty()) {
			return Arrays.asList(room);
		}
			
			
		return eventsNow.stream().map((event) -> event.getProperty("SUMMARY").getValue().toLowerCase())
				.collect(Collectors.toList());
	

	}
}
