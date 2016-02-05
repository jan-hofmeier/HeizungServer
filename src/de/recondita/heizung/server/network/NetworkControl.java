package de.recondita.heizung.server.network;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.recondita.heizung.server.control.Ventilverwalter;

public class NetworkControl implements Closeable {

	private ServerSocket serverSocket;
	private AtomicBoolean run = new AtomicBoolean(true);
	private ArrayList<Client> clients = new ArrayList<Client>();
	private Ventilverwalter ventilverwalter;
	
	private final static Logger LOGGER = Logger
			.getLogger(NetworkControl.class.getName());

	public NetworkControl(int port,Ventilverwalter ventilverwalter) throws IOException {
		serverSocket = new ServerSocket(port);
		this.ventilverwalter=ventilverwalter;
	}

	public void start() {
		new Thread() {
			@Override
			public void run() {
				while (run.get()) {
					try {
						clients.add(new Client(serverSocket.accept()));
					} catch (IOException e) {
						LOGGER.log(Level.WARNING, e.getMessage(), e);
					}
				}
			}
		}.start();
	}

	@Override
	public void close() throws IOException {
		run.set(false);
		serverSocket.close();
		synchronized (clients) {
			for (Client client : clients) {
				try {
					client.close();
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}

}
