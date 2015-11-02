package de.recondita.heizung.server.control;

import java.time.LocalTime;
import java.util.ArrayList;

public class Zeitplan {

	/**
	 * Zeiten [wochentag][schaltpunkt] schaltpunkt&1==0 = an !schaltpunkt&1==0 =
	 * aus
	 */
	private LocalTime[][] plan;
	private ArrayList<Ventil> ventile = new ArrayList<Ventil>();
	//private Timer timer;
	//private Timer dailyTimer;
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
		int punkt = -2;
		if (plan[day] != null) {
			for (int i = 0; i < plan[day].length; i++) {
				if (now.isBefore((plan[day][i]))) {
					punkt = i;
					break;
				}
			}
		}
		setVentile((punkt & 1) == 1);
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
