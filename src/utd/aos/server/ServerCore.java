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
	public void setServer(InetAddress ip, Integer port) {
		this.ip = ip;
		this.port = port;
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
		
			clientSocket.close();
			serverSocket.close();
			System.out.println("Closing socketss");

		} catch (IOException i) {
			i.printStackTrace();
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
		}
	}
	
	class SocketMap {
		Socket socket;
		ObjectOutputStream o_out;
		ObjectInputStream o_in;
		
		public SocketMap(Socket socket,ObjectOutputStream o_out, ObjectInputStream o_in) {
			this.socket = socket;
			this.o_in = o_in;
			this.o_out = o_out;
		}

		public Socket getSocket() {
			return socket;
		}

		public void setSocket(Socket socket) {
			this.socket = socket;
		}

		public ObjectOutputStream getO_out() {
			return o_out;
		}

		public void setO_out(ObjectOutputStream o_out) {
			this.o_out = o_out;
		}

		public ObjectInputStream getO_in() {
			return o_in;
		}

		public void setO_in(ObjectInputStream o_in) {
			this.o_in = o_in;
		}
	}
	
	@Override
	public void execute(Socket clientSocket) throws IOException, ClassNotFoundException {
		
		Map<InetAddress, SocketMap> sockets = new HashMap<InetAddress, SocketMap>();
		
		InputStream in = clientSocket.getInputStream();
		OutputStream out = clientSocket.getOutputStream();
		
		ObjectInputStream o_in = new ObjectInputStream(in);
		ObjectOutputStream o_out = new ObjectOutputStream(out);
		
		while(!clientSocket.isClosed()) {

			System.out.println("attemp to read object");

			Object object = o_in.readObject();

			Operations operation = null;

			if (object instanceof Operations) {
				operation = (Operations)object;
				System.out.println("got object");
			}

			if (operation != null) {
				boolean perform_status = false;
				boolean sync_status = true;

				System.out.println("object not null");
				
				if (operation.getType().equals(OperationType.PERFORM)) {
					System.out.println("checking operation perform");
					perform_status = operation.perform();
					if (perform_status) {
						System.out.println("perform success");

						if(!otherServers.containsKey(clientSocket.getInetAddress())) {					
							System.out.println("this server is connected with client");

							for (Map.Entry<InetAddress, Integer> entry : otherServers.entrySet()) {						
								Socket socket = new Socket(entry.getKey(), entry.getValue());
								
								OutputStream sock_out = socket.getOutputStream();
								ObjectOutputStream sock_o_out = new ObjectOutputStream(sock_out);
								
								InputStream sock_in = socket.getInputStream();
								ObjectInputStream sock_o_in = new ObjectInputStream(sock_in);
								
								sockets.put(entry.getKey(), new SocketMap(socket, sock_o_out, sock_o_in));
							}
							
							System.out.println("attempt to synchronize");

							for (Map.Entry<InetAddress, SocketMap> entry : sockets.entrySet()) {						
								sync_status &= synchronize(operation, entry.getValue().getO_in(), entry.getValue().getO_out());
							}
							

							if (sync_status) {
								System.out.println("all sync");

								operation.commit();
								Operations op = new Operations();
								op.setType(OperationType.COMMIT);
								System.out.println(op.getType().toString());
								for (Map.Entry<InetAddress, SocketMap> entry : sockets.entrySet()) {						
									sync_status &= synchronize(op, entry.getValue().getO_in(), entry.getValue().getO_out());
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
								System.out.println("commit signal");
								Operations op = (Operations)object;
								System.out.println(op.getType().toString());
								if(op.getType().equals(OperationType.COMMIT)) {
									operation.commit();
									m = new Message();
									m.setStatusCode(200);
									o_out.writeObject(m);
									System.out.println("commited");
								}
							}						
							//closing the socket
							System.out.println("closing socket");

							in.close();
						}
					}			
				}
			}
		}
		
		System.out.println("closing server socket");

	}

	public boolean synchronize(Operations operation, ObjectInputStream o_in, ObjectOutputStream o_out) throws IOException, ClassNotFoundException {
		//send operation to other servers
		System.out.println("synchronize to other servers");	
		
		o_out.writeObject(operation);
		
		//wait for their status
		
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
