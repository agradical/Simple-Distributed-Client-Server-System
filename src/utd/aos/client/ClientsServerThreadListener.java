package utd.aos.client;

import java.io.ObjectInputStream;
import utd.aos.utils.MutexMessage;
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

			
			if(message.getType().equals(MessageType.FAILED)) {
				
			
				System.out.println("--RECV FAILED "+socketmap.getAddr().getHostName()+"-");
				gotFailed = 1;
			}
			
			
			if(message.getType().equals(MessageType.REPLY) && pendingRepliesToReceive.containsKey(client_id)) {
				
				System.out.println("--RECV REPLY "+socketmap.getAddr().getHostName()+"--");
				
				pendingRepliesToReceive.remove(client_id);
				
				if(pendingRepliesToReceive.size() == 0) {
					
					//gotallReplies.release();
					System.out.println("--RELEASING allreply sema in listener--");
				
				}
			}
			
			if(message.getType().equals(MessageType.GRANT)) {

				System.out.println("--RECV GRANT "+socketmap.getAddr().getHostName()+"--");
				
				if(sentYieldMessageTo.containsKey(client_id)) {
					sentYieldMessageTo.remove(client_id);
					if(sentYieldMessageTo.size() == 0) {
						sentYield = 0;
					}
				}
				if(pendingReleaseToReceive == client_id)  {
					pendingReleaseToReceive = 0;
				}
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
