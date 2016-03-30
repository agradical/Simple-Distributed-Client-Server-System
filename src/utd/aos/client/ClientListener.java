package utd.aos.client;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import utd.aos.utils.MutexMessage;
import utd.aos.utils.MutexMessage.MessageType;

public class ClientListener extends Client {
	
	Socket socket;
	public ClientListener(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		try {	
			String socketHostname = socket.getInetAddress().getHostName();
			
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();

			ObjectOutputStream o_out = new ObjectOutputStream(out);
			ObjectInputStream o_in = new ObjectInputStream(in);
		
			while(!socket.isClosed()) {

				Object object = null;
				try {
					object = o_in.readObject();
				} catch (Exception e) {
					//Closing connection with other servers in case of termination from client
					System.out.println("--Closing connection--");
					
				}
				
				MutexMessage message = null;
				message = (MutexMessage)object;
				MutexMessage return_message = message;
				
				if(message.getType().equals(MessageType.REQUEST)) {
					gotallReleases.acquire();
					state = State.BLOCKED;
					System.out.println("--got request message from "+socketHostname+"--");
					return_message.setId(id);
					return_message.setType(MessageType.REPLY);
					pendingReleasesToReceive.put(message.getId(), true);
					System.out.println("---waiting for release message from id "+ message.getId()+"--");

					System.out.println("--REPLY--");

					o_out.writeObject(return_message);
				}
				
				if(message.getType().equals(MessageType.RELEASE)) {
					System.out.println("---release message from id "+ message.getId()+" received--");
					if(pendingReleasesToReceive.get(message.getId())) {
						pendingReleasesToReceive.remove(message.getId());
						if(pendingReleasesToReceive.size() == 0) {
							gotallReleases.release();
							state = State.AVAILABLE;
						}
					}
				}

			}
			this.socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

}
