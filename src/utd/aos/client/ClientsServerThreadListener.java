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
			MutexMessage message = (MutexMessage)o_in.readObject();
			int client_id = message.getId();

			if(message.getType().equals(MessageType.FAILED)) {
				gotFailedMessageFrom.put(client_id, true);
				pendingReplyofEnquire = 0;
				gotReplyofEnquire.release();
			}
			
			if(message.getType().equals(MessageType.REPLY) && pendingRepliesToReceive.containsKey(client_id)) {
				
				System.out.println("--got reply from "+socketmap.getAddr().getHostName()+"--");
				
				pendingRepliesToReceive.remove(client_id);
				
				if(sentYieldMessageTo.containsKey(client_id)) {
					sentYieldMessageTo.remove(client_id);
				}
				
				if(pendingRepliesToReceive.size() == 0) {
					
					gotallReplies.release();
					System.out.println("--releasing allreply mutex--");
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
