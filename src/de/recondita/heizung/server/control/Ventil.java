package de.recondita.heizung.server.control;

public class Ventil{
	
	private boolean gpioOn=false;
	private boolean planOn=false;
	private Mode mode=Mode.AUTO;
	private String name;
	
	public Ventil(int gpio, String name) {
		this.name=name;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setPlan(boolean on)
	{
		this.planOn=on;
		setGPIO();
	}
	
	public void override(Mode mode)
	{
		this.mode=mode;
		setGPIO();
	}
	
	private void setGPIO()
	{
		gpioOn=mode==Mode.ON||(mode==Mode.AUTO&&planOn);
	}
	
	public boolean isOn()
	{
		return gpioOn;
	}

}
