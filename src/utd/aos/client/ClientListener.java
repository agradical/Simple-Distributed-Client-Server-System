package utd.aos.client;

import java.net.Socket;

public class ClientListener extends Client {
	
	Socket socket;
	public ClientListener(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		try {	
			super.talk(this.socket);
			this.socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

}
