package de.recondita.heizung.server.control;

import java.io.Closeable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import sun.util.calendar.Gregorian;

public class Zeitplan implements Closeable {

	private LocalTime[][] plan;
	private ArrayList<Ventil> ventile = new ArrayList<Ventil>();
	private Timer timer = new Timer();

	public Zeitplan(LocalTime[][] plan, Ventil[] ventile) {
		this.plan = plan;
		for (Ventil ventil : ventile)
			addVentil(ventil);
	}

	public void addVentil(Ventil ventil) {
		synchronized (ventile) {
			ventile.add(ventil);
		}
	}

	public synchronized void setPlan(LocalTime[][] plan) {
		this.plan = plan.clone();
	}

	public Ventil[] getVentile() {
		synchronized (ventile) {
			return ventile.toArray(new Ventil[ventile.size()]);
		}
	}

	public synchronized void start() {
		run();
	}

	@Override
	public void close() {
		timer.cancel();
	}

	private void run() {
		int day=GregorianCalendar.DAY_OF_WEEK;
		LocalTime now = LocalTime.now();
		
		//TODO
		int i=0;
		while(plan[day][i].compareTo(now)<0&& i<plan[day].length)
			i++;
			
		
	}
}
