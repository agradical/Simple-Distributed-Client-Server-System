package utd.aos.client;

import java.io.ObjectInputStream;

import utd.aos.utils.MutexMessage;
import utd.aos.utils.Request;
import utd.aos.utils.MutexMessage.MessageType;
import utd.aos.utils.SocketMap;

public class ClientsServerThreadListener extends Client {
	
	SocketMap socketmap;
	public ClientsServerThreadListener(SocketMap socketmap) {
		this.socketmap = socketmap;
	}
	
	@Override
	public void run() {
		try {
			ObjectInputStream o_in = socketmap.getO_in();
			
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
				System.out.println("--->"+socketmap.getSocket().getInetAddress().getHostName()+"<---");
				System.out.println("------------DOOMED----------"+ object.getClass());
			}
			
			int client_id = message.getId();
			//String hostname = socketmap.getAddr().getHostName();
			
			if(message.getType().equals(MessageType.YIELD)) {	
				
				
				Request req = new Request(client_id, socketmap, MessageType.YIELD);
				request_q.add(req);
				
				/*
				record.yield++;
				System.out.println("--RECV YIELD "+hostname);
				
				while(pendingReleaseToReceive == id) {
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
				sentEnquire = 0;
				
				InetSocketAddress addr = otherClients.get(min_id_queued);
				
				MutexMessage return_message = new MutexMessage();
				
				String client_hostname = addr.getHostName();
				SocketMap client_socket_map = allClientsSockets.get(client_hostname);
						
				return_message.setId(id);
				return_message.setType(MessageType.GRANT);
				
				System.out.println("--SENT GRANT "+client_hostname+"--");
				
				client_socket_map.getO_out().writeObject(return_message);
				
				record.grant++;
				*/
			}
			
			if(message.getType().equals(MessageType.FAILED)) {
			
				Request req = new Request(client_id, socketmap, MessageType.FAILED);
				request_q.add(req);
				/*
				System.out.println("--RECV FAILED "+hostname+"-");
				gotFailed = 1;
				
				record.fail++;
				*/
			}
			
			
			if(message.getType().equals(MessageType.REPLY) && pendingRepliesToReceive.containsKey(client_id)) {
				
				Request req = new Request(client_id, socketmap, MessageType.REPLY);
				request_q.add(req);
				/*
				System.out.println("--RECV REPLY "+hostname+"--");
				
				pendingRepliesToReceive.remove(client_id);
				
				if(pendingRepliesToReceive.size() == 0) {
					
					//gotallReplies.release();
					System.out.println("--RELEASING allreply sema in listener--");
				
				}
				record.reply++;
				*/
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				shutdown();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}
}
