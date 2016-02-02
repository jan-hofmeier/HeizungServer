package de.recondita.heizung.server.network;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkControl implements Closeable {

	private ServerSocket serverSocket;
	private AtomicBoolean run = new AtomicBoolean(true);
	private ArrayList<Client> clients = new ArrayList<Client>();

	public NetworkControl(int port) throws IOException {
		serverSocket = new ServerSocket(port);
	}

	public void start() {
		new Thread() {
			@Override
			public void run() {
				while (run.get()) {
					try {
						clients.add(new Client(serverSocket.accept()));
					} catch (IOException e) {
						e.printStackTrace();
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
					e.printStackTrace();
				}
			}
		}
	}

}
