package de.recondita.heizung.xml;

import java.io.IOException;
import java.io.StringReader;
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

import de.recondita.heizung.server.control.Ventil;

public class XMLLoader {

	private XPath xPath = XPathFactory.newInstance().newXPath();
	private DocumentBuilder builder;

	public XMLLoader() throws ParserConfigurationException {
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}

	public ArrayList<Ventil> getVentile(String xml) throws XPathExpressionException, SAXException, IOException {
		StringReader stringReader = new StringReader(xml);
		Document xmlDocument = builder.parse(new InputSource(stringReader));
		XPathExpression root = xPath.compile("/ventile");

		NodeList ventile = (NodeList) root.evaluate(xmlDocument, XPathConstants.NODESET);

		ArrayList<Ventil> ret = new ArrayList<Ventil>(ventile.getLength());
		for (int i = 0; i < ventile.getLength(); i++) {
			Node v = ventile.item(i);
			int id = Integer.parseInt(v.getAttributes().getNamedItem("id").getNodeValue());
			String name = null;
			int gpio = -1;
			NodeList childs = v.getChildNodes();
			for (int j = 0; j < childs.getLength(); j++) {
				Node n = childs.item(i);
				switch (n.getNodeName()) {
				case "name":
					name = n.getNodeValue();
					break;
				case "gpio":
					gpio = Integer.parseInt(n.getNodeValue());
				}
			}
			ret.add(new Ventil(id,gpio,name));
		}
		return ret;
	}

}
