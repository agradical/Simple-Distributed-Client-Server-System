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
		
			
			SocketMap socketMap = new SocketMap(socket, o_out, o_in);
			
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
					
					record.request++;
					
					System.out.println("--RECV REQUEST "+socketHostname+"--");


					if(pendingReleaseToReceive == 0 ) {

						pendingReleaseToReceive = client_id;

						return_message.setId(id);
						return_message.setType(MessageType.REPLY);

						System.out.println("--SENT REPLY to "+socketHostname+"--");

						o_out.writeObject(return_message);
						
						record.reply++;

					} else {

						//lower id = high priority
						if(pendingReleaseToReceive < client_id) {
														
							return_message.setId(id);
							return_message.setType(MessageType.FAILED);

							System.out.println("--SENT FAILED "+socketHostname+"--");

							o_out.writeObject(return_message);
							
							record.fail++;

						} else {

							if(pendingReleaseToReceive != id) {

								return_message.setId(id);
								return_message.setType(MessageType.ENQUIRE);


								InetSocketAddress addr = otherClients.get(pendingReleaseToReceive);
								String client_hostname = addr.getHostName();
								SocketMap client_socket_map = allClientsSockets.get(client_hostname);

								System.out.println("--SENT ENQUIRE  "+client_hostname+"--");

								client_socket_map.getO_out().writeObject(return_message);

								record.enquire++;
								
								sentEnquire = 1;
								
							} else {
								
								request_fifo.add(client_id);
							}

						}
					} 
					
				}
				
				if(message.getType().equals(MessageType.RELEASE)) {
										
					if(pendingReleaseToReceive == client_id) {
						System.out.println("---RECV RELEASE  "+ socketHostname+" --");

						pendingReleaseToReceive = 0;
						
						System.out.println("--Releasing release sema(release)-");
						//gotallReleases.release();
						record.release++;
					}
				}
				
				
				if(message.getType().equals(MessageType.ENQUIRE)) {
					//sends grant or reply to top request in the queue
					//TODO
					
					/*if(pendingReplyofEnquire != 0) {
						
						System.out.println("--wait for enquire sema(enquire)-");
						gotReplyofEnquire.acquire();
						
						System.out.println("--released enquire sema(enquire)-");
						gotReplyofEnquire.release();
					
					}*/
					
					record.enquire++;
					System.out.println("--RECV ENQUIRE "+socketHostname);
					
					while(gotFailed != 1 && sentYield != 1 && pendingReleaseToReceive != 0 && sentEnquire != 1) {
						Thread.sleep(20);
						System.out.println("WAITING for ENQUIRE to process");
					}
					
					if(pendingReleaseToReceive == 0) {
						
						pendingReleaseToReceive = client_id;

						return_message.setId(id);
						return_message.setType(MessageType.REPLY);
						
						System.out.println("--SENT REPLY "+socketHostname+"--");

						o_out.writeObject(return_message);
						
						record.reply++;
						
					} else if(gotFailed == 1) {

						return_message.setId(id);
						return_message.setType(MessageType.YIELD);

						sentYieldMessageTo.put(client_id, true);

						sentYield = 1;
						System.out.println("--SENT YIELD "+socketHostname+"--");

						o_out.writeObject(return_message);

						record.yield++;


					} else if (sentYield == 1) {

						return_message.setId(id);
						return_message.setType(MessageType.YIELD);

						sentYieldMessageTo.put(client_id, true);
						sentYield = 1;

						System.out.println("--SENT YIELD "+socketHostname+"--");

						o_out.writeObject(return_message);

						record.yield++;

					} else {
						
						return_message.setId(id);
						return_message.setType(MessageType.YIELD);

						sentYieldMessageTo.put(client_id, true);
						sentYield = 1;

						System.out.println("--SENT YIELD "+socketHostname+"--");

						o_out.writeObject(return_message);

						record.yield++;
					} 
					
				}
				
				if(message.getType().equals(MessageType.GRANT)) {

					System.out.println("--RECV GRANT "+socketHostname+"--");
					
					if(sentYieldMessageTo.containsKey(client_id)) {
						sentYieldMessageTo.remove(client_id);
						if(sentYieldMessageTo.size() == 0) {
							sentYield = 0;
						}
					}
					
					if(pendingRepliesToReceive.containsKey(client_id)) {
						pendingRepliesToReceive.remove(client_id);
					}
					
					if(pendingReleaseToReceive == client_id)  {
						pendingReleaseToReceive = 0;
					}
					record.grant++;
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
