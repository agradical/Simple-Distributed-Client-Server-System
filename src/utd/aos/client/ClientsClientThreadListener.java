package utd.aos.client;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import utd.aos.utils.MutexMessage;
import utd.aos.utils.MutexMessage.MessageType;
import utd.aos.utils.SocketMap;

public class ClientsClientThreadListener extends Client {
	
	Socket socket;
	public ClientsClientThreadListener(Socket socket) {
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
				MutexMessage return_message = new MutexMessage();
				int client_id = message.getId();

				
				if(message.getType().equals(MessageType.REQUEST)) {
					/*
					if(pendingRepliesToReceive.containsKey(message.getId())) {
						if(id < message.getId()) {
							gotallReleases.acquire();
						}
					} else {
						gotallReleases.acquire();
					}*/
					
					if(pendingReplyofEnquire != 0) {
						gotReplyofEnquire.acquire();
						gotReplyofEnquire.release();
					}
					
					if(pendingReleaseToReceive == 0 ) {
						
						gotallReleases.acquire();
						if(pendingRepliesToReceive.size() == 0) {
							pendingReleaseToReceive = client_id;

							return_message.setId(id);
							return_message.setType(MessageType.REPLY);

							System.out.println("--got request message from "+socketHostname+"--");
							System.out.println("--REPLY to "+socketHostname+"--");
							System.out.println("---waiting for release message from id "+ client_id+"--");

							o_out.writeObject(return_message);
						} else if (pendingRepliesToReceive.size() != 0) {
							
							pendingReleaseToReceive = client_id;

							return_message.setId(id);
							return_message.setType(MessageType.REPLY);

							System.out.println("--got concurrent request message from "+socketHostname+"--");
							System.out.println("--REPLY to "+socketHostname+"--");
							System.out.println("--waiting for release message from id "+ client_id+"--");

							o_out.writeObject(return_message);
						}
						

					} else if (pendingReleaseToReceive != 0) {
						
						//lower id = high priority
						if(pendingReleaseToReceive < client_id) {

							return_message.setId(id);
							return_message.setType(MessageType.FAILED);

							System.out.println("--FAILED SENT to "+socketHostname+"--");
							o_out.writeObject(return_message);

						} else {
							
							gotReplyofEnquire.acquire();
							pendingReplyofEnquire = pendingReleaseToReceive;
							
							return_message.setId(id);
							return_message.setType(MessageType.ENQUIRE);
							
							System.out.println("--ENQUIRE SENT to "+socketHostname+"--");

							InetSocketAddress addr = otherClients.get(pendingReleaseToReceive);
							String client_hostname = addr.getHostName();
							SocketMap client_socket_map = allClientsSockets.get(client_hostname);

							client_socket_map.getO_out().writeObject(return_message);
						}

					} 
				}
				
				if(message.getType().equals(MessageType.RELEASE)) {
					System.out.println("---release message from id "+ message.getId()+" received--");
					if(pendingReleaseToReceive == client_id) {

						pendingReleaseToReceive = 0;
						gotallReleases.release();
					
					}
				}
				
				if(message.getType().equals(MessageType.ENQUIRE)) {
					//sends grant or reply to top request in the queue
					//TODO
					
					if(pendingReplyofEnquire != 0) {
						gotReplyofEnquire.acquire();
						gotReplyofEnquire.release();
					}
					
					if(gotFailedMessageFrom != null && gotFailedMessageFrom.size() != 0) {
						
						return_message.setId(id);
						return_message.setType(MessageType.YIELD);
						
						sentYieldMessageTo.put(client_id, true);
						
						System.out.println("--YIELD SENT to "+socketHostname+"--");
						
						o_out.writeObject(return_message);
					
					} else if (sentYieldMessageTo != null && sentYieldMessageTo.size() != 0) {
												
						return_message.setId(id);
						return_message.setType(MessageType.YIELD);
											
						InetSocketAddress addr = otherClients.get(client_id);
						String client_hostname = addr.getHostName();
						SocketMap client_socket_map = allClientsSockets.get(client_hostname);
						
						System.out.println("--YIELD SENT to "+client_hostname+"--");

						client_socket_map.getO_out().writeObject(return_message);
					} 
				}
				
				if(message.getType().equals(MessageType.YIELD)) {
					//sends grant or reply to top request in the queue
					//TODO send to top request
					gotReplyofEnquire.release();
					pendingReplyofEnquire = 0;
					
					fifo.add(client_id);
					
					Integer client_granted = fifo.remove();
					InetSocketAddress addr = otherClients.get(client_granted);
					String client_hostname = addr.getHostName();
					SocketMap client_socket_map = allClientsSockets.get(client_hostname);
							
					return_message.setId(id);
					return_message.setType(MessageType.REPLY);
					
					System.out.println("--GRANT SENT to "+socketHostname+"--");
					
					client_socket_map.getO_out().writeObject(return_message);
				}
			}
			
			this.socket.close();
		
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

}
