package de.recondita.heizung.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalTime;
import java.util.ArrayList;

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

	public XMLLoader(File configdir) throws ParserConfigurationException, XPathExpressionException {
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		zeitplaenePath = xPath.compile("/zeitplaene");
		tagesPlaenePath = xPath.compile("tagesplaene");
		tagesPlanPath = xPath.compile("tagesplane");
		namePath = xPath.compile("name");
		zeitplanePath = xPath.compile("/ventile");
		gpioPath = xPath.compile("gpio");
		schaltpunktePath = xPath.compile("schaltpunkte");
		;
		this.configdir = configdir;
	}

	public void loadVentile(Ventilverwalter ventilverwalter)
			throws FileNotFoundException, IOException, XPathExpressionException, SAXException {
		try (FileInputStream fis = new FileInputStream(
				configdir.getAbsolutePath() + File.pathSeparator + "Ventile.xml");) {
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

	public ArrayList<Zeitplan> loadZeitplaene(Reader xml, Ventilverwalter ventilverwalter)
			throws SAXException, IOException, XPathExpressionException {
		Document xmlDocument = builder.parse(new InputSource(xml));
		NodeList zeitPlaene = (NodeList) zeitplaenePath.evaluate(xmlDocument, XPathConstants.NODESET);

		ArrayList<Zeitplan> ret = new ArrayList<Zeitplan>(zeitPlaene.getLength());

		for (int i = 0; i < zeitPlaene.getLength(); i++) {
			Node zeitplanNode = zeitPlaene.item(i);
			int id = Integer.parseInt(zeitplanNode.getAttributes().getNamedItem("id").getNodeValue());
			String name = namePath.evaluate(zeitplanNode);
			Node tagesplaene = (Node) tagesPlaenePath.evaluate(zeitplanNode, XPathConstants.NODE);
			LocalTime[][] plan = evaluateZeitplan(tagesplaene);
			ret.add(new Zeitplan(id, name, plan));
		}
		return ret;
	}

	private LocalTime[][] evaluateZeitplan(Node tagesplaene) throws XPathExpressionException {
		NodeList tagNodes = (NodeList) tagesPlanPath.evaluate(tagesplaene, XPathConstants.NODESET);
		LocalTime[][] ret = new LocalTime[7][];
		for (int i = 0; i < tagNodes.getLength(); i++) {
			Node tagNode=tagNodes.item(i);
			
		}
		return ret;
	}
	
	private LocalTime[] evaluateSchaltpunkte(Node schaltpunkte)
	{
		return null;
	}

}
