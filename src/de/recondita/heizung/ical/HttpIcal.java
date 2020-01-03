package de.recondita.heizung.ical;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
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
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.util.MapTimeZoneCache;

public class HttpIcal {

	private final static Logger LOGGER = Logger
			.getLogger(HttpIcal.class.getName());
	
	static {
		System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache.class.getName());
	}

	private URL url;
	
	public Calendar calendar = new Calendar();

	public HttpIcal(URL icalUrl) {
		this.url = icalUrl;
		LOGGER.fine("Create HTTP iCal for " + url);
	}

	private Calendar requestCalendar() throws IOException, ParserException {
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setInstanceFollowRedirects(true);
		int status = con.getResponseCode();
		if (status != 200) {
			throw new IOException("Non 200 status Code: " + status);
		}

		try (InputStream cis = con.getInputStream()) {
			return new CalendarBuilder().build(cis);
		}
	}

	public List<String> getActiveGroups() {
		
		try {
			calendar = requestCalendar();
		} catch (IOException e) {
			LOGGER.severe("Can not get Calendar from " + url);
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		} catch (ParserException e) {
			LOGGER.severe("Can not parse Calendar from " + url);
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

		DateTime now = new DateTime(java.util.Calendar.getInstance().getTime());

		Period period = new Period(now, now);
		Predicate<VEvent> periodRule = new PeriodRule<>(period);
		Filter<VEvent> filter = new Filter<>(periodRule);

		Collection<VEvent> eventsNow = filter.filter(calendar.getComponents(Component.VEVENT));
		
		//Handle Recurrent Events with changed time for single instance
		List<VEvent> removed = new ArrayList<>();
		for(VEvent event: eventsNow) {
			ComponentGroup<CalendarComponent> group = new ComponentGroup<>(calendar.getComponents(), event.getUid());
			if(group.calculateRecurrenceSet(period).isEmpty()) {
				removed.add(event);
				LOGGER.log(Level.INFO, "Found moved event: " + event.getName());
			}
		}
		eventsNow.removeAll(removed);
		
		return eventsNow.stream().map((event) -> event.getProperty("SUMMARY").getValue().toLowerCase()).collect(Collectors.toList());

	}

}
