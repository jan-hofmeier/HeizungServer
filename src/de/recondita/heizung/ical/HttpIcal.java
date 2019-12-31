package de.recondita.heizung.ical;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.filter.Filter;
import net.fortuna.ical4j.filter.PeriodRule;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.util.MapTimeZoneCache;

public class HttpIcal {

	static {
		System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache.class.getName());
	}

	private URL url;
	
	public Calendar calendar = new Calendar();

	public HttpIcal(URL icalUrl) {
		this.url = icalUrl;
	}

	private Calendar requestCalendar() throws IOException, ParserException {
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setInstanceFollowRedirects(true);
		int status = con.getResponseCode();
		if (status != 200)
			throw new IOException("Non 200 status Code: " + status);

		try (InputStream cis = con.getInputStream()) {
			return new CalendarBuilder().build(cis);
		}
	}

	public List<String> getActiveGroups() {
		
		try {
			calendar = requestCalendar();
		} catch (IOException e) {
			System.err.println("Can not get Calendar from " + url);
			e.printStackTrace();
		} catch (ParserException e) {
			System.err.println("Can not parse Calendar from " + url);
			e.printStackTrace();
		}

		DateTime now = new DateTime(java.util.Calendar.getInstance().getTime());

		// create a period starting now with a duration of one (1) day..
		Predicate<CalendarComponent> periodRule = new PeriodRule<>(new Period(now, now));
		Filter<CalendarComponent> filter = new Filter<>(periodRule);

		Collection<CalendarComponent> eventsNow = filter.filter(calendar.getComponents(Component.VEVENT));
		for (CalendarComponent event : eventsNow)
			System.out.println(event.getProperty("SUMMARY").getValue());
		
		return eventsNow.stream().map((event) -> event.getProperty("SUMMARY").getValue()).collect(Collectors.toList());

	}

}
