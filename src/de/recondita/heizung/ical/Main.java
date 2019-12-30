package de.recondita.heizung.ical;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.function.Predicate;

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

public class Main {

	
	static {
		System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache.class.getName());
	}
	
	private final static String urlStr="https://calendar.google.com/calendar/ical/isunrle4o51q24ofd21htinem4%40group.calendar.google.com/private-81e75f523ab6fca709b39435dd447a37/basic.ics"; 
	private final static CalendarBuilder builder = new CalendarBuilder();
	
	
	public static void main(String[] args) {
		try {
			URL url = new URL(urlStr);
			HttpURLConnection con;
			try {
				con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("GET");
			 	con.setInstanceFollowRedirects(true);
				int status = con.getResponseCode();
				if(status != 200)
					throw new IOException("Non 200 status Code: " + status);

				try(InputStream cis = con.getInputStream()){
					Calendar calendar = builder.build(cis);
					
					
					DateTime now = new DateTime(java.util.Calendar.getInstance().getTime());

					// create a period starting now with a duration of one (1) day..
					Predicate<CalendarComponent> periodRule = new PeriodRule<>(new Period(now, now));
					Filter<CalendarComponent> filter = new Filter<>(periodRule);

					Collection<CalendarComponent> eventsNow = filter.filter(calendar.getComponents(Component.VEVENT));
					for(CalendarComponent event: eventsNow){
						System.out.println(event.getProperty("SUMMARY").getValue());
					}
					
					
				} catch (ParserException e) {
					e.printStackTrace();
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
	}

}
