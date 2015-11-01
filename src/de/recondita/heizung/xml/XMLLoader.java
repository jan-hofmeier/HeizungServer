package de.recondita.heizung.xml;

import java.io.IOException;
import java.io.StringReader;

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

public class XMLLoader {

	private XPath xPath = XPathFactory.newInstance().newXPath();
	private DocumentBuilder builder;

	public XMLLoader() throws ParserConfigurationException {
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}

	public void loadVentile(String xml, Ventilverwalter ventilverwalter) throws XPathExpressionException, SAXException, IOException {
		StringReader stringReader = new StringReader(xml);
		Document xmlDocument = builder.parse(new InputSource(stringReader));
		XPathExpression root = xPath.compile("/ventile");
		XPathExpression gpioPath = xPath.compile("gpio");
		XPathExpression namePath = xPath.compile("name");
		
		NodeList ventile = (NodeList) root.evaluate(xmlDocument, XPathConstants.NODESET);

		for (int i = 0; i < ventile.getLength(); i++) {
			Node v = ventile.item(i);
			ventilverwalter.createVentil(Integer.parseInt(gpioPath.evaluate(v)), namePath.evaluate(v));
		}
	}

}
