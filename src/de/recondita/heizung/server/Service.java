package de.recondita.heizung.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.xml.sax.SAXException;

import de.recondita.heizung.server.control.Ventilverwalter;
import de.recondita.heizung.server.network.NetworkControl;
import de.recondita.heizung.server.verwalter.ZeitplanVerwalter;
import de.recondita.heizung.xml.XMLLoader;
import de.recondita.heizung.xml.XMLLoader.PunktOrderException;

public class Service implements Daemon {

	private ZeitplanVerwalter zeitplanverwalter;
	private static Ventilverwalter ventilverwalter=Ventilverwalter.getInstance();
	private NetworkControl networkControl;

	public static void main(String[] args) throws FileNotFoundException, XPathExpressionException, IOException, SAXException, PunktOrderException, ParserConfigurationException{
		createZeitplanVerwalter(args).start();
	}

	@Override
	public void destroy() {

	}

	@Override
	public void init(DaemonContext context) throws DaemonInitException, Exception, XPathExpressionException,
			IOException, SAXException, PunktOrderException, ParserConfigurationException {
		this.zeitplanverwalter=createZeitplanVerwalter(context.getArguments()) ;
		networkControl= new NetworkControl(1001);
	}

	private static ZeitplanVerwalter createZeitplanVerwalter(String[] args) throws FileNotFoundException, XPathExpressionException, IOException, SAXException, PunktOrderException, ParserConfigurationException
	{
		return new ZeitplanVerwalter(ventilverwalter,
				new XMLLoader(new File(args.length == 0 ? "config" : args[0])));
	}
	
	@Override
	public void start() throws Exception {
		zeitplanverwalter.start();

	}

	@Override
	public void stop() throws Exception {
		networkControl.close();
		zeitplanverwalter.close();
		ventilverwalter.shutdown();

	}

}
