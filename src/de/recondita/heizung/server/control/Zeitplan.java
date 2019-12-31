package de.recondita.heizung.server.control;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Zeitplan {

	/**
	 * Zeiten [wochentag][schaltpunkt] schaltpunkt&1==0 = an !schaltpunkt&1==0 =
	 * aus
	 */
	private LocalTime[][] plan;
	private ArrayList<Ventil> ventile;
	private boolean on = false;
	private String name;
	public final int id;
	private int lastday = -1;
	private int lastpunkt = -1;
	
	private final static Logger LOGGER = Logger
			.getLogger(Zeitplan.class.getName());

	public int getId() {
		return id;
	}

	public Zeitplan(int id, String name, LocalTime[][] plan, List<Ventil> ventile) {
		this.id = id;
		this.setName(name);
		setPlan(plan);
	}

	public synchronized void setPlan(LocalTime[][] plan) {
		this.plan = plan;
	}

	public Ventil[] getVentile() {
		synchronized (ventile) {
			return ventile.toArray(new Ventil[ventile.size()]);
		}
	}

	private void setVentile(boolean on) {
		if (on != this.on) {
			this.on = on;
			synchronized (ventile) {
				for (Ventil v : ventile)
					v.setPlanOn(on);
			}
		}
	}

	public boolean isOn() {
		return on;
	}

	public synchronized void check(int day, LocalTime now) {
		LOGGER.fine("check Zeitplan "+ getName());
		if (plan[day] != null) {
			int punkt = plan[day].length;
			do {
				punkt--;
			} while (punkt >= 0 && plan[day][punkt].isAfter(now));
			if (lastpunkt != punkt || lastday != day) {
				lastday = day;
				lastpunkt = punkt;
				setVentile((punkt & 1) == 0);
			}
		} else {
			if (lastday != day) {
				setVentile(false);
				lastday=day;
			}
		}
		LOGGER.fine("checked Zeitplan "+ getName());
	}

	boolean removeVentil(Ventil v) {
		synchronized (ventile) {
			return ventile.remove(v);
		}
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
