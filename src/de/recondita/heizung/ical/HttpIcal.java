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
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.filter.Filter;
import net.fortuna.ical4j.filter.predicate.PeriodRule;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentGroup;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
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
		if (backupFile != null && backupFile.exists()) {
			try {
				loadFromBackupFile();
				LOGGER.info("Loaded Backup file " + backupFile.getName());
			} catch (IOException | ParserException e) {
				LOGGER.log(Level.WARNING, "Can't load backup file");
				LOGGER.log(Level.WARNING, e.getMessage(), e);
			}
		} else {
			LOGGER.info("No Backup file " + (backupFile != null ? backupFile.getName() : "<none>"));
		}
	}

	private void setCalendar(String calendarStr) throws ParserException {
		try (Reader reader = new StringReader(calendarStr)) {
			calendar = new CalendarBuilder().build(reader);
			lastCalendarStr = calendarStr;
		} catch (IOException e) {
			// that wont happen
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}

	private void loadFromBackupFile() throws FileNotFoundException, IOException, ParserException {
		setCalendar(readCalendarStrFromFile());
	}

	private static String convert(InputStream inputStream) throws IOException {

		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, DEFAULT_CHARSET))) {
			return br.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	private String requestCalendarStr() throws IOException {
		java.net.URLConnection con = url.openConnection();
		if (con instanceof HttpURLConnection) {
			HttpURLConnection httpCon = (HttpURLConnection) con;
			httpCon.setConnectTimeout(10000);
			httpCon.setReadTimeout(20000);
			httpCon.setRequestMethod("GET");
			httpCon.setInstanceFollowRedirects(true);
			int status = httpCon.getResponseCode();
			if (status != 200) {
				throw new IOException("Non 200 status Code: " + status);
			}
		}
		return convert(con.getInputStream());
	}

	private String readCalendarStrFromFile() throws FileNotFoundException, IOException {
		return convert(new FileInputStream(backupFile));
	}

	private void saveToBackupFile(String calendarStr) throws IOException {
		LOGGER.info("Write new version of calendar to backup file " + backupFile.getName());
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
			if (lastCalendarStr != null)
				return calendar;
			throw e;
		}

		// Google setzt DTSTAMP for all Events to the time of the request. This would
		// cause the strings to mismatch
		calendarStr = calendarStr.replaceAll("DTSTAMP:[\\d]{8}T[\\d]{6}Z\n", "");

		// no need to reparse if it didn't change
		if (calendarStr.equals(lastCalendarStr))
			return calendar;

		try {
			setCalendar(calendarStr);
			lastCalendarStr = calendarStr;
			// save the new version of the calendar only if it changed and is parseable.
			if (backupFile != null)
				saveToBackupFile(calendarStr);
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

	public Set<String> getActiveGroups() throws IOException, ParserException {
		LOGGER.log(Level.INFO, "getting calendar for room " + room);
		
		Calendar calendar = getCalendar();

		ZonedDateTime now = ZonedDateTime.now();

		Period<ZonedDateTime> period = new Period<>(now, now);
		Predicate<VEvent> periodRule = new PeriodRule<>(period);
		Filter<VEvent> filter = new Filter<>(periodRule);

		Collection<VEvent> eventsNow = filter.filter(calendar.getComponents(Component.VEVENT));

		// Handle Recurrent Events with changed time for single instance
		List<VEvent> removed = new ArrayList<>();
		for (VEvent event : eventsNow) {
			// calculateRecurrenceSet returns a Set<Period<Instant>> (or similar)
			Set<?> recurrences = event.calculateRecurrenceSet(period);
			if (recurrences.isEmpty()) {
			    continue;
			}
			Period<?> p = (Period<?>) recurrences.stream().findFirst().get();
			Temporal start = p.getStart();
			
			ComponentGroup<VEvent> group = new ComponentGroup<VEvent>(
					new ArrayList<>(calendar.getComponents(Component.VEVENT)),
					event.getUid().orElse(null));
			for (VEvent revision : group.getRevisions()) {
				RecurrenceId<Temporal> recurrenceid = revision.getRecurrenceId();
				if (recurrenceid != null && start.equals(recurrenceid.getDate()) && !period.intersects(
						new Period<>(revision.getStartDate().get().getDate(), revision.getEndDate().get().getDate()))) {
					removed.add(event);
					LOGGER.log(Level.FINE, "Found moved event: " + event.getName());
				}
			}
		}
		eventsNow.removeAll(removed);

		return eventsNow.stream()
				.map((event) -> event.getProperty("SUMMARY"))
				.filter(Optional::isPresent)
				.map(p -> p.get().getValue().toLowerCase())
				.collect(Collectors.toSet());

	}

	public String getRoom() {
		return room;
	}
}
