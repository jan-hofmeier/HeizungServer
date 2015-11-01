package de.recondita.heizung.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
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

	private XPath xPath = XPathFactory.newInstance().newXPath();
	private DocumentBuilder builder;
	private File configdir;

	public XMLLoader(File configdir) throws ParserConfigurationException {
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		this.configdir = configdir;
	}

	public void loadVentile(Ventilverwalter ventilverwalter) throws FileNotFoundException, IOException, XPathExpressionException, SAXException {
		try (FileInputStream fis = new FileInputStream(
				configdir.getAbsolutePath() + File.pathSeparator + "Ventile.xml");) {
			loadVentile(new InputStreamReader(fis), ventilverwalter);
		}
	}

	public void loadVentile(Reader xml, Ventilverwalter ventilverwalter)
			throws XPathExpressionException, SAXException, IOException {
		Document xmlDocument = builder.parse(new InputSource(xml));
		XPathExpression root = xPath.compile("/ventile");
		XPathExpression gpioPath = xPath.compile("gpio");
		XPathExpression namePath = xPath.compile("name");

		NodeList ventile = (NodeList) root.evaluate(xmlDocument, XPathConstants.NODESET);

		for (int i = 0; i < ventile.getLength(); i++) {
			Node v = ventile.item(i);
			ventilverwalter.createVentil(Integer.parseInt(gpioPath.evaluate(v)), namePath.evaluate(v));
		}
	}

	
	public ArrayList<Zeitplan> loadZeitplaene(Reader xml, Ventilverwalter ventilverwalter) throws SAXException, IOException, XPathExpressionException {
		Document xmlDocument = builder.parse(new InputSource(xml));
		XPathExpression root = xPath.compile("/zeitplane");
		XPathExpression gpioPath = xPath.compile("schaltpunkte");
		XPathExpression namePath = xPath.compile("name");
		
		NodeList zeitPlaene = (NodeList) root.evaluate(xmlDocument, XPathConstants.NODESET);
		
		ArrayList<Zeitplan> ret= new ArrayList<Zeitplan>(zeitPlaene.getLength());
		
		for(int i=0; i<zeitPlaene.getLength(); i++)
		{
			//TODO
		}
		
		return null;
	}

}
