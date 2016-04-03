package utd.aos.client;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;

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
					System.out.println("RECV ENQUIRE "+socketHostname);
					
					while(gotFailed != 1 && sentYield != 1) {
						Thread.sleep(20);
						System.out.println("WAITING for ENQUIRE to process");
					}
					
					if(gotFailed == 1) {

						return_message.setId(id);
						return_message.setType(MessageType.YIELD);

						sentYieldMessageTo.put(client_id, true);

						sentYield = 1;
						System.out.println("--SENT YIELD "+socketHostname+"--");

						o_out.writeObject(return_message);

						record.yield++;

						break;

					} else if (sentYield == 1) {

						return_message.setId(id);
						return_message.setType(MessageType.YIELD);

						sentYieldMessageTo.put(client_id, true);
						sentYield = 1;

						InetSocketAddress addr = otherClients.get(client_id);
						String client_hostname = addr.getHostName();
						SocketMap client_socket_map = allClientsSockets.get(client_hostname);

						System.out.println("--SENT YIELD "+client_hostname+"--");

						client_socket_map.getO_out().writeObject(return_message);

						record.yield++;

						break;
					}
					
				}
				
				if(message.getType().equals(MessageType.YIELD)) {	
										
					record.yield++;
					
					while(pendingReleaseToReceive != 0) {
						Thread.sleep(200);
						System.out.println("WAIT in YIELD for Release");
					} 
					
					//request_fifo.add(client_id);
					
					Iterator<Integer> iterator = request_fifo.iterator();
					int min_id_queued = 100;
					while(iterator.hasNext()) {
						Integer i = iterator.next();
						if(i < min_id_queued) {
							min_id_queued = i;
						}
					}
					
					while(iterator.hasNext()) {
						Integer i = iterator.next();
						if(i == min_id_queued) {
							iterator.remove();
						}
					}
					
					if(min_id_queued == 100 || min_id_queued > client_id) {
						min_id_queued = client_id;
					}

					pendingReleaseToReceive = min_id_queued;
					
					InetSocketAddress addr = otherClients.get(min_id_queued);
					
					
					String client_hostname = addr.getHostName();
					SocketMap client_socket_map = allClientsSockets.get(client_hostname);
							
					return_message.setId(id);
					return_message.setType(MessageType.GRANT);
					
					System.out.println("--SENT GRANT "+client_hostname+"--");
					
					client_socket_map.getO_out().writeObject(return_message);
					
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
