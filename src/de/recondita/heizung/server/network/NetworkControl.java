package de.recondita.heizung.server.network;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
			+ "<button type=\"reset\">Reset</button>" + "</form></body></html>";

	private final Ventilverwalter ventilverwalter;
	private HttpServer httpServer;

	private final static Logger LOGGER = Logger.getLogger(NetworkControl.class.getName());

	public NetworkControl(int port, Ventilverwalter ventilverwalter) throws IOException {
		this.ventilverwalter = ventilverwalter;
		httpServer = HttpServer.create(new InetSocketAddress(port), 0);
		httpServer.createContext("/", new WebsiteHandler());
		httpServer.createContext("/state", new JsonStateHandler());
		httpServer.setExecutor(null); // creates a default executor
	}

	private static void sendResonseString(HttpExchange exchange, String response, String contentType)
			throws IOException {
		Headers responseHeaders = exchange.getResponseHeaders();
		responseHeaders.set("Content-Type", contentType + "; charset=utf-8");
		byte[] responseBytes = response.getBytes("UTF-8");
		exchange.sendResponseHeaders(200, responseBytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(responseBytes);
		}
	}

	private class JsonStateHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String responseStr = ventilverwalter.toJson().toString();
			sendResonseString(exchange, responseStr, "application/json");
		}

	}

	private class WebsiteHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) {
			try {
				LOGGER.info("Start handling Request from: " + exchange.getRemoteAddress().getHostName());
				LOGGER.info("Request Method: " + exchange.getRequestMethod());
				if ("POST".equals(exchange.getRequestMethod())) {
					String requestBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody())).lines()
							.parallel().collect(Collectors.joining("\n"));
					LOGGER.info("Request Body: " + requestBody);

					String[] valveSettings = requestBody.split("&");
					for (String valveSetting : valveSettings) {
						String[] parts = valveSetting.split("=");
						String valveName = URLDecoder.decode(parts[0], "UTF-8");
						ventilverwalter.getVentilByName(valveName).override(Mode.valueOf(parts[1]));
					}

					Headers responseHeaders = exchange.getResponseHeaders();
					responseHeaders.set("Location", "/");
					exchange.sendResponseHeaders(303, 0);

				} else {
					StringBuilder responseStr = new StringBuilder(header);

					for (Ventil valve : ventilverwalter) {
						responseStr.append("<p><h1>");
						responseStr.append(valve.getName());
						responseStr.append(" ");
						responseStr.append(valve.getCurrentTemp());
						responseStr.append("°C ");
						responseStr.append("<font color=");
						if (valve.isOn())
							responseStr.append("\"green\">AN");
						else
							responseStr.append("\"red\">AUS");
						responseStr.append("</font></h1>");
						generateRadio(responseStr, valve.getName(), valve.getMode());
						responseStr.append("</p>");
					}

					responseStr.append(footer);

					sendResonseString(exchange, responseStr.toString(), "text/html");
				}

			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Exception while hanfling HTTP Request:\n" + e.getMessage(), e);
			}
			LOGGER.info("Finished handling Request from: " + exchange.getRemoteAddress().getHostName());
		}

		private void generateRadio(StringBuilder sb, String valve, Mode value) {
			sb.append("<fieldset>");
			for (Mode modeE : Mode.values()) {
				String modeStr = modeE.name();
				sb.append("<input type=\"radio\" id=\"");
				sb.append(valve);
				sb.append(modeStr);
				sb.append("\" name=\"");
				sb.append(valve);
				sb.append("\" value=\"");
				sb.append(modeStr);
				sb.append("\"");
				if (modeE == value)
					sb.append(" checked");
				sb.append(" />");
				sb.append("<label for=\"");
				sb.append(valve);
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
