package de.recondita.heizung.server.network;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import de.recondita.heizung.server.control.Mode;
import de.recondita.heizung.server.control.Ventil;
import de.recondita.heizung.server.control.Ventilverwalter;

@SuppressWarnings("restriction")
public class NetworkControl implements Closeable {

	private final static String header = "<!DOCTYPE html>\n<html lang=\"de\"><head><title>Heizung</title></head><body><form>";
	private final static String footer = "<button id=\"apply\" formmethod=\"post\">Übernehmen</button>"
			+ "<button type=\"reset\"><Reset</button>" + "</form></body></html>";

	private final Ventilverwalter ventilverwalter;
	private HttpServer httpServer;

	private final static Logger LOGGER = Logger.getLogger(NetworkControl.class.getName());

	public NetworkControl(int port, Ventilverwalter ventilverwalter) throws IOException {
		this.ventilverwalter = ventilverwalter;
		httpServer = HttpServer.create(new InetSocketAddress(port), 0);
		httpServer.createContext("/", new MyHandler());
		httpServer.setExecutor(null); // creates a default executor
	}

	private class MyHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) {
			try {
				LOGGER.info("Start handling Request from: " + exchange.getRemoteAddress().getHostName());
				StringBuilder responseStr = new StringBuilder(header);

				for (Ventil room : ventilverwalter) {
					responseStr.append("<p><h1>");
					responseStr.append(room.getName());
					responseStr.append("</h1>");
					generateRadio(responseStr, room.getName(), room.getMode());
					responseStr.append("</p>");
				}

				responseStr.append(footer);
				
				Headers responseHeaders = exchange.getResponseHeaders();
				responseHeaders.set("Content-Type", "text/html; charset=utf-8");
				byte[] responseBytes = responseStr.toString().getBytes("UTF-8");
				exchange.sendResponseHeaders(200, responseBytes.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(responseBytes);
				}
				LOGGER.info("Finished handling Request from: " + exchange.getRemoteAddress().getHostName());
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Exception while hanfling HTTP Request:\n" + e.getMessage(), e);
			}
		}
		
		private void generateRadio(StringBuilder sb, String room, Mode value) {
			sb.append("<fieldset>");
			for (Mode modeE : Mode.values()) {
				String modeStr = modeE.name();
				sb.append("<input type=\"radio\" id=\"");
				sb.append(room);
				sb.append(modeStr);
				sb.append("\" name=\"");
				sb.append(room);
				sb.append("\" value=\"");
				sb.append(modeStr);
				sb.append("\"");
				if (modeE == value)
					sb.append(" checked");
				sb.append(" />");
				sb.append("<label for=\"");
				sb.append(room);
				sb.append(modeStr);
				sb.append("\">");
				sb.append(modeStr);
				sb.append("</label>");
			}
			sb.append("</fieldset>");
		}

	}

	public void start() {
		httpServer.start();
	}

	@Override
	public void close() throws IOException {
		httpServer.stop(5);
	}

}
