package utd.aos.client;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

import utd.aos.utils.MutexMessage;
import utd.aos.utils.MutexMessage.MessageType;
import utd.aos.utils.SocketMap;

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
				MutexMessage return_message = new MutexMessage();
				
				if(message.getType().equals(MessageType.REQUEST)) {
					/*
					if(pendingRepliesToReceive.containsKey(message.getId())) {
						if(id < message.getId()) {
							gotallReleases.acquire();
						}
					} else {
						gotallReleases.acquire();
					}*/
					
					if(pendingRepliesToReceive.size() == 0) {
						
						state = State.BLOCKED;
						System.out.println("--got request message from "+socketHostname+"--");
						int client_id = message.getId();
						pendingReleasesToReceive.put(client_id, true);
						return_message.setId(id);
						return_message.setType(MessageType.REPLY);
						System.out.println("--REPLY--");
						System.out.println("---waiting for release message from id "+ client_id+"--");
						o_out.writeObject(return_message);
						
					} else {
						//lower id = high priority
						boolean highpriorityreq = false;
						Integer lowerPriorityreq = message.getId();
						for(Map.Entry<String, SocketMap> entry : quorum.entrySet()) {
							int _id = hostIdMap.get(entry.getKey());
							if( _id < message.getId()) {
								if(pendingReleasesToReceive.containsKey(_id)) {
									return_message.setId(id);
									return_message.setType(MessageType.FAILED);
									System.out.println("--FAILED SENT to "+socketHostname+"--");
									o_out.writeObject(return_message);
								}
							} else {
								lowerPriorityreq = _id;
							}
						}
						if(!highpriorityreq) {
							return_message.setId(id);
							return_message.setType(MessageType.ENQUIRE);

							//should go through server channel
							InetSocketAddress addr = otherClients.get(lowerPriorityreq);
							String client_hostname = addr.getHostName();
							
							SocketMap client_socket_map = quorum.get(client_hostname);
							System.out.println("--ENQUIRE SENT to "+client_hostname+"--");

							client_socket_map.getO_out().writeObject(return_message);
							gotallReleases.acquire();

						}
					}
					
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
				
				if(message.getType().equals(MessageType.ENQUIRE)) {
					//sends grant or reply to top request in the queue
					//TODO
					if(gotFailedMessageFrom != null && gotFailedMessageFrom.size() != 0) {
						return_message.setId(id);
						return_message.setType(MessageType.YIELD);
						System.out.println("--YIELD SENT to "+socketHostname+"--");
						o_out.writeObject(return_message);
					} else if (sentYieldMessageTo != null && sentYieldMessageTo.size() != 0) {
						return_message.setId(id);
						return_message.setType(MessageType.YIELD);
						sentYieldMessageTo.put(message.getId(), true);
						InetSocketAddress addr = otherClients.get(message.getId());
						String client_hostname = addr.getHostName();
						SocketMap client_socket_map = quorum.get(client_hostname);
						System.out.println("--YIELD SENT to "+client_hostname+"--");

						client_socket_map.getO_out().writeObject(return_message);
					}
				}
				
				if(message.getType().equals(MessageType.YIELD)) {
					//sends grant or reply to top request in the queue
					//TODO send to top request
					return_message.setId(id);
					return_message.setType(MessageType.REPLY);
					System.out.println("--GRANT SENT to "+socketHostname+"--");
					o_out.writeObject(return_message);
				}
			}
			this.socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

}
