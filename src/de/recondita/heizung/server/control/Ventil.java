package de.recondita.heizung.server.control;

public class Ventil{
	
	private boolean gpioOn=false;
	private boolean planOn=false;
	private Mode mode=Mode.AUTO;
	private String name;
	private Zeitplan zeitplan;
	private int gpio;
	public final int id;
	
	public int getId() {
		return id;
	}

	public Ventil(int id,int gpio, String name) {
		this.id=id;
		this.name=name;
		this.gpio=gpio;
	}
	
	public int getGpio() {
		return gpio;
	}

	public String getName()
	{
		return name;
	}
	
	public void setPlanOn(boolean on)
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
	
	synchronized void setZeitplan(Zeitplan zp)
	{
		removeFromZeitplan();
		this.zeitplan=zp;
		zp.addVentil(this);
	}
	
	public Zeitplan getZeitplan()
	{
		return this.zeitplan;
	}
	
	public synchronized void removeFromZeitplan()
	{
		if(zeitplan!=null)
		{
			zeitplan.removeVentil(this);
			setZeitplan(null);
		}
	}

}
