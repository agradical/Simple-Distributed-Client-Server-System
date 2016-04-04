package utd.aos.client;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;

import utd.aos.utils.MutexMessage;
import utd.aos.utils.MutexMessage.MessageType;
import utd.aos.utils.Request;
import utd.aos.utils.SocketMap;

public class ClientsClientThreadListener extends Client {
	
	Socket socket;
	public ClientsClientThreadListener(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		try {	
			InetAddress inet_addr = socket.getInetAddress();

			String socketHostname = inet_addr.getHostName();
			
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();

			ObjectOutputStream o_out = new ObjectOutputStream(out);
			ObjectInputStream o_in = new ObjectInputStream(in);
		
			//DANGER --its just a workaround
			InetSocketAddress addr = new InetSocketAddress(inet_addr, 1818);
			
			SocketMap socketMap = new SocketMap(socket, o_out, o_in, addr);
			
			if(allClientsListenerSockets == null) {
				allClientsListenerSockets = new HashMap<String, SocketMap>();
			}
			allClientsListenerSockets.put(socketHostname, socketMap);

			while(!socket.isClosed()) {

				Object object = null;
				try {
					object = o_in.readObject();
				} catch (Exception e) {
					//Closing connection with other servers in case of termination from client
					System.out.println("--Closing connection--");
				}
				
				MutexMessage message = null;
				if(object instanceof MutexMessage) {
					message = (MutexMessage)object;
				} else {
					System.out.println("--->"+socket.getInetAddress().getHostName()+"<---");
					System.out.println("------------DOOMED----------"+ object.getClass());
				}
				
				int client_id = message.getId();

				
				if(message.getType().equals(MessageType.REQUEST)) {
					
					Request req = new Request(client_id, socketMap, MessageType.REQUEST);
					request_q.add(req);
					
				}
				
				if(message.getType().equals(MessageType.RELEASE)) {
					
					if(pendingReleaseToReceive == client_id) {
						Request req = new Request(client_id, socketMap, MessageType.RELEASE);
						request_q.add(req);
					}
					
				}
				
				
				if(message.getType().equals(MessageType.ENQUIRE)) {
					
					Request req = new Request(client_id, socketMap, MessageType.ENQUIRE);
					request_q.add(req);					
					
				}
				
				if(message.getType().equals(MessageType.GRANT)) {

					Request req = new Request(client_id, socketMap, MessageType.GRANT);
					request_q.add(req);
					
				}
				
			}
			
			this.socket.close();
		
		} catch (Exception e) {
			try {
				shutdown();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}	
	}

}
