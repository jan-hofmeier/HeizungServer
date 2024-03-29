package de.recondita.heizung.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import de.recondita.heizung.ical.HttpIcal;
import de.recondita.heizung.server.control.TempratureGetter;
import de.recondita.heizung.server.control.Ventil;
import de.recondita.heizung.server.control.Ventilverwalter;
import de.recondita.heizung.server.control.Zeitplan;
import de.recondita.heizung.server.googleservices.SheetRoomSettings;

public class ConfigLoader {

	private final XPath xPath = XPathFactory.newInstance().newXPath();
	private DocumentBuilder builder;
	private File configdir;
	private final XPathExpression zeitplaenePath;
	private final XPathExpression tagesPlaenePath;
	private final XPathExpression tagesPlanPath;
	private final XPathExpression namePath;
	private final XPathExpression ventilePath;
	private final XPathExpression gpioPath;
	private final XPathExpression schaltpunktePath;
	private final XPathExpression tagePath;

	private static final String GOOGLE_APPLICATION_NAME = "Heizung";

	private final static Logger LOGGER = Logger.getLogger(ConfigLoader.class.getName());

	private GoogleCredentials googleCredentials;

	public ConfigLoader(File configdir)
			throws ParserConfigurationException, XPathExpressionException, FileNotFoundException {
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		zeitplaenePath = xPath.compile("/zeitplaene/zeitplan");
		tagesPlaenePath = xPath.compile("tagesplaene");
		tagesPlanPath = xPath.compile("tagesplan");
		namePath = xPath.compile("name");
		ventilePath = xPath.compile("/ventile/ventil");
		gpioPath = xPath.compile("gpio");
		schaltpunktePath = xPath.compile("schaltpunkte");
		tagePath = xPath.compile("tage/tag");
		this.configdir = configdir;
	}

	public void loadVentile(Ventilverwalter ventilverwalter)
			throws FileNotFoundException, IOException, XPathExpressionException, SAXException {
		try (FileInputStream fis = new FileInputStream(configdir.getAbsolutePath() + File.separator + "Ventile.xml");) {
			loadVentile(new InputStreamReader(fis), ventilverwalter);
		}
	}

	public void loadVentile(Reader xml, Ventilverwalter ventilverwalter)
			throws XPathExpressionException, SAXException, IOException {
		Document xmlDocument = builder.parse(new InputSource(xml));

		NodeList ventile = (NodeList) ventilePath.evaluate(xmlDocument, XPathConstants.NODESET);
		for (int i = 0; i < ventile.getLength(); i++) {
			Node v = ventile.item(i);
			ventilverwalter.createVentil(Integer.parseInt(gpioPath.evaluate(v)), namePath.evaluate(v));
		}
	}

	public ArrayList<Zeitplan> loadZeitplaene(Ventilverwalter ventilverwalter)
			throws FileNotFoundException, IOException, XPathExpressionException, SAXException, PunktOrderException {
		try (FileInputStream fis = new FileInputStream(
				configdir.getAbsolutePath() + File.separator + "Zeitplaene.xml");) {
			return loadZeitplaene(new InputStreamReader(fis), ventilverwalter);
		}
	}

	public ArrayList<Zeitplan> loadZeitplaene(Reader xml, Ventilverwalter ventilverwalter)
			throws SAXException, IOException, XPathExpressionException, PunktOrderException {
		Document xmlDocument = builder.parse(new InputSource(xml));
		NodeList zeitPlaene = (NodeList) zeitplaenePath.evaluate(xmlDocument, XPathConstants.NODESET);

		ArrayList<Zeitplan> ret = new ArrayList<Zeitplan>(zeitPlaene.getLength());

		for (int i = 0; i < zeitPlaene.getLength(); i++) {
			ret.add(evaluateZeitplan(zeitPlaene.item(i), ventilverwalter));
		}
		return ret;
	}

	private Zeitplan evaluateZeitplan(Node zeitplanNode, Ventilverwalter ventilverwalter)
			throws XPathExpressionException, PunktOrderException {
		int id = Integer.parseInt(zeitplanNode.getAttributes().getNamedItem("id").getTextContent());
		String name = namePath.evaluate(zeitplanNode);
		Node tagesplaene = (Node) tagesPlaenePath.evaluate(zeitplanNode, XPathConstants.NODE);
		LocalTime[][] plan = evaluateTagesplan(tagesplaene);
		NodeList ventileNodes = (NodeList) ventilePath.evaluate(zeitplanNode, XPathConstants.NODESET);
		List<Ventil> ventile = new ArrayList<>(ventileNodes.getLength());
		for (int i = 0; i < ventileNodes.getLength(); i++) {
			String vname = ventileNodes.item(i).getTextContent();
			LOGGER.info("Fuege Ventil " + vname + " zu Plan " + name + " hinzu");
			ventile.add(ventilverwalter.getVentilByName(name));
		}
		Zeitplan zp = new Zeitplan(id, name, plan, ventile);
		return zp;
	}

	private LocalTime[][] evaluateTagesplan(Node tagesplaene) throws XPathExpressionException, PunktOrderException {
		NodeList tagNodes = (NodeList) tagesPlanPath.evaluate(tagesplaene, XPathConstants.NODESET);
		LocalTime[][] ret = new LocalTime[7][];
		for (int i = 0; i < tagNodes.getLength(); i++) {
			Node tagNode = tagNodes.item(i);
			LocalTime[] tagesPlan = evaluateSchaltpunkte(
					(Node) schaltpunktePath.evaluate(tagNode, XPathConstants.NODE));
			NodeList tage = (NodeList) tagePath.evaluate(tagNode, XPathConstants.NODESET);
			for (int j = 0; j < tage.getLength(); j++) {
				ret[Integer.parseInt(tage.item(j).getTextContent())] = tagesPlan;
			}
		}
		return ret;
	}

	private LocalTime[] evaluateSchaltpunkte(Node schaltpunkte) throws XPathExpressionException, PunktOrderException {
		NodeList punktNodes = schaltpunkte.getChildNodes();
		LOGGER.info(punktNodes.getLength() + " Schaltpunkte");
		TreeSet<LocalTime> an = new TreeSet<LocalTime>();
		TreeSet<LocalTime> aus = new TreeSet<LocalTime>();
		for (int i = 0; i < punktNodes.getLength(); i++) {
			Node punktNode = punktNodes.item(i);
			switch (punktNode.getNodeName()) {
			case "an":
				an.add(toLocalTime(punktNode.getTextContent()));
				break;
			case "aus":
				aus.add(toLocalTime(punktNode.getTextContent()));
				break;
			}
		}
		LocalTime[] ret = new LocalTime[an.size() + aus.size()];
		Iterator<LocalTime> anIt = an.iterator();
		Iterator<LocalTime> ausIt = aus.iterator();
		for (int i = 0; i < ret.length; i++) {
			ret[i] = i % 2 == 0 ? anIt.next() : ausIt.next();
			LOGGER.info("Schaltpunkt " + ret[i]);
			if (i > 0 && ret[i].isBefore(ret[i - 1]))
				throw new PunktOrderException(ret[i], ret[i - 1]);
		}
		return ret;
	}

	private LocalTime toLocalTime(String time) {
		String[] parts = time.split(":");
		return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
	}

	public class PunktOrderException extends Exception {
		private static final long serialVersionUID = -289478342144267298L;

		public PunktOrderException(LocalTime before, LocalTime after) {
			super(before.toString() + " before " + after.toString());
		}
	}

	public HttpIcal[] loadIcal() throws IOException {
		String[] urls = Files.lines(Paths.get(configdir + File.separator + "icalurls.txt")).toArray(String[]::new);
		HttpIcal[] icals = new HttpIcal[urls.length];
		for (int i = 0; i < urls.length; i++) {
			String[] parts = urls[i].split(" ");
			File backupFile = new File(configdir + File.separator + "schedule" + i + ".ical");
			if (parts.length > 1)
				icals[i] = new HttpIcal(new URL(parts[0]), backupFile, parts[1]);
			else
				icals[i] = new HttpIcal(new URL(parts[0]), backupFile);
		}
		return icals;
	}

	private GoogleCredentials loadGoogleCredentials() throws IOException {
		if (googleCredentials == null) {
			InputStream gc = new FileInputStream(new File(configdir + File.separator + "google-credentials.json"));
			googleCredentials = ServiceAccountCredentials.fromStream(gc);
		}
		return googleCredentials;
	}

	public SheetRoomSettings loadSheetRoomSettings() throws IOException, GeneralSecurityException {
		String id = Files.lines(Paths.get(configdir + File.separator + "roomconfig-sheetid")).toArray(String[]::new)[0];
		return new SheetRoomSettings(loadGoogleCredentials(), GOOGLE_APPLICATION_NAME, id,
				new File(configdir + File.separator + "roomsettings.csv"));
	}

	public TempratureGetter loadTempratureGetter() {
		return new TempratureGetter(new File(configdir + File.separator + "homematic").exists());
	}

	public String[] loadMQTTConfig() throws IOException {
		String[] ret = null; // default config
		File mqttfile = new File(configdir + File.separator + "mqtt-config.txt");
		if (mqttfile.exists()) {
			LOGGER.info("Found MQTT config: " + mqttfile);
			try (BufferedReader br = new BufferedReader(new FileReader(mqttfile))) {
				ret = new String[4];
				for (int i = 0; i < ret.length; i++) {
					ret[i] = br.readLine(); // username
				}
			}
		}
		return ret;
	}
}
