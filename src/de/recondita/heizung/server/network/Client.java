package de.recondita.heizung.server.network;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

public class Client implements Closeable{

	private Socket socket;
	
	public Client(Socket socket){
		this.socket=socket;
	}

	@Override
	public void close() throws IOException {
		socket.close();
	}

}
