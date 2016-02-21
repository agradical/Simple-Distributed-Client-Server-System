package utd.aos.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import utd.aos.utils.Message;
import utd.aos.utils.Operations;
import utd.aos.utils.Operations.OperationType;

public class ServerCore implements Server {
	
	private Map<InetAddress, Integer> otherServers;
	private InetAddress ip;
	private Integer port;

	@Override
	public Server getServer() {
		return this;
	}
	
	@Override
	public void setServer(InetAddress ip, Integer port, HashMap<InetAddress, Integer> servers) {
		this.ip = ip;
		this.port = port;
		this.otherServers = servers;
	}
	
	@Override
	public void addServer(InetAddress server, Integer port) {
		if (this.otherServers == null) {
			this.otherServers = new HashMap<InetAddress, Integer>();
		}
		this.otherServers.put(server, port);
	}


	@Override
	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(this.getPort());
			System.out.println("Socket created");
			Socket clientSocket = serverSocket.accept();
			System.out.println("Socket accepted");

			execute(clientSocket);				
		
			serverSocket.close();
			
		} catch (IOException i) {
			i.printStackTrace();
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
		}
	}
	
	@Override
	public void execute(Socket clientSocket) throws IOException, ClassNotFoundException {
		
		Map<InetAddress, Socket> sockets = new HashMap<InetAddress, Socket>();
		
		InputStream in = clientSocket.getInputStream();
		OutputStream out = clientSocket.getOutputStream();
		
		ObjectInputStream o_in = new ObjectInputStream(in);
		ObjectOutputStream o_out = new ObjectOutputStream(out);
		
		while(!clientSocket.isClosed()) {

			Object object = o_in.readObject();

			Operations operation = null;

			if (object instanceof Operations) {
				operation = (Operations)object;
				System.out.println("got object");
			}

			if (operation != null) {
				boolean perform_status = false;
				boolean sync_status = true;

				if (operation.getType().equals(OperationType.PERFORM)) {
					perform_status = operation.perform();
					if (perform_status) {
						if(!otherServers.containsKey(clientSocket.getInetAddress())) {					
							
							for (Map.Entry<InetAddress, Integer> entry : otherServers.entrySet()) {						
								Socket socket = new Socket(entry.getKey(), entry.getValue());
								sockets.put(entry.getKey(), socket);
							}

							for (Map.Entry<InetAddress, Socket> entry : sockets.entrySet()) {						
								sync_status &= synchronize(entry.getValue(), operation);
							}
							
							if (sync_status) {
								operation.commit();
								operation.setType(OperationType.COMMIT);
								for (Map.Entry<InetAddress, Socket> entry : sockets.entrySet()) {						
									sync_status &= synchronize(entry.getValue(), operation);
								}
								
								//closing the socket
								Message m = new Message();
								m.setStatusCode(200);
								o_out.writeObject(m);
								System.out.println("closing socket");

								in.close();
							}
						} else {
							//sends back the success signal
							Message m = new Message();
							m.setStatusCode(200);
							o_out.writeObject(m);
							
							System.out.println("not connected with client so returning message");

							
							//and wait for commit signal
							object = o_in.readObject();
							if (object instanceof Operations) {
								Operations op = (Operations)object;
								if(op.getType().equals(OperationType.COMMIT)) {
									operation.commit();
									m = new Message();
									m.setStatusCode(200);
									o_out.writeObject(m);
								}
							}						
							//closing the socket
							in.close();
						}
					}			
				}
			}
		}

	}

	public boolean synchronize(Socket socket, Operations operation) throws IOException, ClassNotFoundException {
		//send operation to other servers
		System.out.println("synchronize to other servers");

		OutputStream out = socket.getOutputStream();
		ObjectOutputStream o_out = new ObjectOutputStream(out);	
		
		o_out.writeObject(operation);
		
		//wait for their status
		InputStream in = socket.getInputStream();
		ObjectInputStream o_in = new ObjectInputStream(in);
		Object object = o_in.readObject();
		
		Message m;
		
		if (object instanceof Message) {
			 m = (Message)object;
			 System.out.println( "got ack");

			 if(m.getStatusCode() == 200) {
				 return true;
			 } else {
				 return false;
			 }
		} else {
			return false;
		}

	}
	
	public InetAddress getIp() {
		return ip;
	}

	public void setIp(InetAddress ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}


}
