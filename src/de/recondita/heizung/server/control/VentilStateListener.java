package de.recondita.heizung.server.control;

import java.util.EventListener;

public interface VentilStateListener extends EventListener {
	
	public abstract void stateChanged(String ventilName, Mode mode, boolean on);

}
