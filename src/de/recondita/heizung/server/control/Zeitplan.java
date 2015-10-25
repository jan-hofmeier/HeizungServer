package de.recondita.heizung.server.control;

import java.io.Closeable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class Zeitplan implements Closeable {

	/**
	 * Zeiten [wochentag][schaltpunkt] schaltpunkt&1==0 = an !schaltpunkt&1==0 =
	 * aus
	 */
	private LocalTime[][] plan;
	private ArrayList<Ventil> ventile = new ArrayList<Ventil>();
	private Timer timer;
	private Timer dailyTimer;
	private boolean on = false;

	public Zeitplan(LocalTime[][] plan, Ventil[] ventile) {
		this.ventile = new ArrayList<Ventil>(ventile.length);
		for (Ventil ventil : ventile)
			addVentil(ventil);
		setPlan(plan);
	}

	public void addVentil(Ventil ventil) {
		synchronized (ventile) {
			ventile.add(ventil);
			ventil.setPlan(on);
		}
	}

	public synchronized void setPlan(LocalTime[][] plan) {
		this.plan = plan;
	}

	public Ventil[] getVentile() {
		synchronized (ventile) {
			return ventile.toArray(new Ventil[ventile.size()]);
		}
	}

	public synchronized void start() {
		close();
		Calendar today = Calendar.getInstance();
		today.add(Calendar.DAY_OF_WEEK, 1);
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 2);
		dailyTimer = new Timer();
		dailyTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					synchronized (Zeitplan.this) {
						cancelTimerSafe();
						timer = new Timer(); // vorsichtshalber erneuern
					}
					// eigener Thread für alle fälle
					new Thread(new ZeitplanTask()).start();
				} catch (Exception e) {
					e.printStackTrace();
					logVeryBadException(e);
				}
			}
		}, today.getTime(), TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));
		timer = new Timer();
		run();
	}

	private void cancelTimerSafe()
	{
		if (timer != null)
			try {
				timer.cancel();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	
	@Override
	public synchronized void close() {
		cancelTimerSafe();
		if (dailyTimer != null)
			try {
				dailyTimer.cancel();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private void setVentile(boolean on) {
		this.on = on;
		synchronized (ventile) {
			for (Ventil v : ventile)
				v.setPlan(on);
		}
	}

	public boolean isOn() {
		return on;
	}

	private synchronized void run() {
		timer.cancel();

		LocalDate date = LocalDate.now();
		int day = date.getDayOfWeek().ordinal();
		LocalTime now = LocalTime.now();

		int punkt = -1;
		for (int i = 0; i < plan[day].length && now.compareTo(plan[day][i]) < 0; i++)
			punkt = i;

		if (punkt + 1 < plan[day].length) {
			timer.schedule(
					new ZeitplanTask(),
					Date.from(plan[day][punkt + 1].atDate(date)
							.atZone(ZoneId.systemDefault()).toInstant()));

		}

		setVentile((punkt & 1) == 0);

	}

	private class ZeitplanTask extends TimerTask
	{
		@Override
		public void run() {
			try {
				Zeitplan.this.run();
			} catch (Exception e) {
				e.printStackTrace();
				logVeryBadException(e);
			}
		}
	}
	
	private void logVeryBadException(Exception e) {
		// TODO
	}
}
