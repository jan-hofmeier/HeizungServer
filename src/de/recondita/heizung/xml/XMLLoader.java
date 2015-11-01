package de.recondita.heizung.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

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

import de.recondita.heizung.server.control.Ventilverwalter;
import de.recondita.heizung.server.control.Zeitplan;

public class XMLLoader {

	private final XPath xPath = XPathFactory.newInstance().newXPath();
	private DocumentBuilder builder;
	private File configdir;
	private final XPathExpression zeitplaenePath;
	private final XPathExpression tagesPlaenePath;
	private final XPathExpression tagesPlanPath;
	private final XPathExpression namePath;
	private final XPathExpression zeitplanePath;
	private final XPathExpression gpioPath;
	private final XPathExpression schaltpunktePath;
	private final XPathExpression tagePath;
	private final XPathExpression ventilePath;

	public XMLLoader(File configdir) throws ParserConfigurationException, XPathExpressionException {
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		zeitplaenePath = xPath.compile("/zeitplaene");
		tagesPlaenePath = xPath.compile("tagesplaene");
		tagesPlanPath = xPath.compile("tagesplane");
		namePath = xPath.compile("name");
		zeitplanePath = xPath.compile("/ventile");
		gpioPath = xPath.compile("gpio");
		schaltpunktePath = xPath.compile("schaltpunkte");
		tagePath = xPath.compile("tage/tag");
		ventilePath= xPath.compile("ventile/ventile");
		this.configdir = configdir;
	}

	public void loadVentile(Ventilverwalter ventilverwalter)
			throws FileNotFoundException, IOException, XPathExpressionException, SAXException {
		try (FileInputStream fis = new FileInputStream(
				configdir.getAbsolutePath() + File.separator + "Ventile.xml");) {
			loadVentile(new InputStreamReader(fis), ventilverwalter);
		}
	}

	public void loadVentile(Reader xml, Ventilverwalter ventilverwalter)
			throws XPathExpressionException, SAXException, IOException {
		Document xmlDocument = builder.parse(new InputSource(xml));

		NodeList ventile = (NodeList) zeitplanePath.evaluate(xmlDocument, XPathConstants.NODESET);

		for (int i = 0; i < ventile.getLength(); i++) {
			Node v = ventile.item(i);
			ventilverwalter.createVentil(Integer.parseInt(gpioPath.evaluate(v)), namePath.evaluate(v));
		}
	}

	public ArrayList<Zeitplan> loadZeitplaene(Ventilverwalter ventilverwalter)
			throws FileNotFoundException, IOException, XPathExpressionException, SAXException, PunktOrderException {
		try (FileInputStream fis = new FileInputStream(
				configdir.getAbsolutePath() + File.separator + "Zeitplane.xml");) {
			return loadZeitplaene(new InputStreamReader(fis), ventilverwalter);
		}
	}
	
	public ArrayList<Zeitplan> loadZeitplaene(Reader xml, Ventilverwalter ventilverwalter)
			throws SAXException, IOException, XPathExpressionException, PunktOrderException {
		Document xmlDocument = builder.parse(new InputSource(xml));
		NodeList zeitPlaene = (NodeList) zeitplaenePath.evaluate(xmlDocument, XPathConstants.NODESET);

		ArrayList<Zeitplan> ret = new ArrayList<Zeitplan>(zeitPlaene.getLength());

		for (int i = 0; i < zeitPlaene.getLength(); i++) {
			ret.add(evaluateZeitplan(zeitPlaene.item(i),ventilverwalter));
		}
		return ret;
	}

	private Zeitplan evaluateZeitplan(Node zeitplanNode, Ventilverwalter ventilverwalter) throws XPathExpressionException, PunktOrderException {
		int id = Integer.parseInt(zeitplanNode.getAttributes().getNamedItem("id").getNodeValue());
		String name = namePath.evaluate(zeitplanNode);
		Node tagesplaene = (Node) tagesPlaenePath.evaluate(zeitplanNode, XPathConstants.NODE);
		LocalTime[][] plan = evaluateTagesplan(tagesplaene);
		Zeitplan zp= new Zeitplan(id, name, plan);
		NodeList ventilNodes = (NodeList) ventilePath.evaluate(zeitplanNode, XPathConstants.NODESET);
		for(int i=0; i<ventilNodes.getLength(); i++)
		{
			ventilverwalter.getVentilByName(ventilNodes.item(i).getNodeValue()).setZeitplan(zp);
		}
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
				ret[Integer.parseInt(tage.item(j).getNodeValue())] = tagesPlan;
			}
		}
		return ret;
	}

	private LocalTime[] evaluateSchaltpunkte(Node schaltpunkte) throws XPathExpressionException, PunktOrderException {
		NodeList punktNodes = (NodeList) schaltpunktePath.evaluate(schaltpunkte, XPathConstants.NODESET);
		TreeSet<LocalTime> an = new TreeSet<LocalTime>();
		TreeSet<LocalTime> aus = new TreeSet<LocalTime>();
		for (int i = 0; i < punktNodes.getLength(); i++) {
			Node punktNode = punktNodes.item(i);
			switch (punktNode.getNodeName()) {
			case "an":
				an.add(toLocalTime(punktNode.getNodeValue()));
				break;
			case "aus":
				aus.add(toLocalTime(punktNode.getNodeValue()));
				break;
			}
		}
		LocalTime[] ret = new LocalTime[an.size() + aus.size()];
		Iterator<LocalTime> anIt = an.iterator();
		Iterator<LocalTime> ausIt = aus.iterator();
		for (int i = 0; i < ret.length; i++) {
			ret[i] = i % 2 == 0 ? anIt.next() : ausIt.next();
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
}
