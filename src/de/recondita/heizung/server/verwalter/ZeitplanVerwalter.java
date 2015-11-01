package de.recondita.heizung.server.verwalter;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import de.recondita.heizung.server.control.Ventilverwalter;
import de.recondita.heizung.server.control.Zeitplan;
import de.recondita.heizung.xml.XMLLoader;
import de.recondita.heizung.xml.XMLLoader.PunktOrderException;

public class ZeitplanVerwalter implements Closeable {
	private Ventilverwalter ventile;
	private XMLLoader configurationLoader;
	private ArrayList<Zeitplan> zeitPlaene;


	public ZeitplanVerwalter(Ventilverwalter ventilverwalter, XMLLoader configurationLoader)
			throws FileNotFoundException, XPathExpressionException, IOException, SAXException, PunktOrderException {
		this.ventile=ventilverwalter;
		this.configurationLoader=configurationLoader;
		this.configurationLoader.loadVentile(ventile);
		this.zeitPlaene=this.configurationLoader.loadZeitplaene(ventilverwalter);
		for(Zeitplan z:zeitPlaene)
		{
			z.start();
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (zeitPlaene) {
			for (Zeitplan z : zeitPlaene)
				z.close();
		}
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) throws FileNotFoundException, XPathExpressionException, IOException, SAXException, PunktOrderException, ParserConfigurationException
	{
		new ZeitplanVerwalter(Ventilverwalter.getInstance(),new XMLLoader(new File("config")));
	}

}
