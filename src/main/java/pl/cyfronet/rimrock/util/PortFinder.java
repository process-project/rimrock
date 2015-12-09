package pl.cyfronet.rimrock.util;

import java.io.IOException;
import java.net.ServerSocket;

public class PortFinder {
	public static int getFreePort() throws IOException {
		ServerSocket socket = null;
		
		try {
			socket = new ServerSocket(0);
			
			return socket.getLocalPort();
		} finally {
			socket.close();
		}
	}
}
