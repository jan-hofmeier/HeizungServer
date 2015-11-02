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
	private String name;
	public final int id;

	public int getId() {
		return id;
	}

	public Zeitplan(int id, String name, LocalTime[][] plan) {
		this.id = id;
		this.setName(name);
		setPlan(plan);
	}

	void addVentil(Ventil ventil) {
		synchronized (ventile) {
			ventile.add(ventil);
			ventil.setPlanOn(on);
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
		dailyTimer.scheduleAtFixedRate(new TimerTask() {
			long lasttime = -1;

			@Override
			public void run() {
				try {
					synchronized (this) {
						long time = System.currentTimeMillis();
						if (time - lasttime < 3600000)
							return;
						lasttime = time;
					}
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

	private void cancelTimerSafe() {
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
				v.setPlanOn(on);
		}
	}

	public boolean isOn() {
		return on;
	}

	private synchronized void run() {

		LocalDate date = LocalDate.now();
		int day = date.getDayOfWeek().ordinal();
		LocalTime now = LocalTime.now();

		int punkt = -1;
		if (plan[day] != null) {
			for (int i = 0; i < plan[day].length; i++) {
				if (now.isBefore((plan[day][i]))) {
					punkt = i;
					break;
				}
			}

			if (punkt + 1 < plan[day].length) {
				Date d = Date.from(plan[day][punkt + 1].atDate(date)
						.atZone(ZoneId.systemDefault()).toInstant());
				timer.schedule(
						new ZeitplanTask(),d);
				System.out.println("Setze Time auf "+d.toString());
			}
		}
		setVentile((punkt & 1) == 0);
	}

	boolean removeVentil(Ventil v) {
		synchronized (ventile) {
			return ventile.remove(v);
		}
	}

	private class ZeitplanTask extends TimerTask {
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("\nPlan " + name + ":\n");
		for (int i = 0; i < plan.length; i++) {
			sb.append("Tag ");
			sb.append(i);
			sb.append(":\n");
			if (plan[i] == null)
				sb.append("null\n");
			else {
				for (int j = 0; j < plan[i].length; j++) {
					sb.append(plan[i][j].toString());
				}
			}
		}
		return sb.toString();
	}
}
