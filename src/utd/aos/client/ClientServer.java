package utd.aos.client;

import java.io.ObjectInputStream;
import utd.aos.utils.MutexMessage;
import utd.aos.utils.MutexMessage.MessageType;
import utd.aos.utils.SocketMap;

public class ClientServer extends Client {
	
	SocketMap socketmap;
	public ClientServer(SocketMap socketmap) {
		this.socketmap = socketmap;
	}
	
	@Override
	public void run() {
		try {
			ObjectInputStream o_in = socketmap.getO_in();

			MutexMessage message = (MutexMessage)o_in.readObject();
			if(message != null && message.getType().equals(MessageType.REPLY) 
					&& pendingRepliesToReceive.get(message.getId())) {
				System.out.println("--got reply from "+socketmap.getAddr().getHostName()+"--");
				pendingRepliesToReceive.remove(message.getId());
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
