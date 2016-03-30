package utd.aos.client;

import java.io.InputStream;
import java.io.ObjectInputStream;

import java.net.Socket;

import utd.aos.utils.MutexMessage;
import utd.aos.utils.MutexMessage.MessageType;

public class ClientServer extends Client {
	
	Socket socket;
	public ClientServer(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		try {
			InputStream in = socket.getInputStream();
			ObjectInputStream o_in =  new ObjectInputStream(in);

			MutexMessage message = (MutexMessage)o_in.readObject();
			if(message.getType().equals(MessageType.REPLY) 
					&& pendingRepliesToReceive.get(message.getId())) {
				System.out.println("--got reply from "+socket.getInetAddress().getHostName()+"--");
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
