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
import utd.aos.utils.Resource;
import utd.aos.utils.Operations.OperationMethod;
import utd.aos.utils.Operations.OperationType;

public class ServerCore implements Server {
	
	private Map<InetAddress, Integer> otherServers;
	private InetAddress ip;
	private Integer port;
	
	public String DATADIRECTORY = "data";
	
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
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(this.getPort());
			System.out.println("Socket created");
			while(true) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("Socket accepted");

				try {
					execute(clientSocket);				
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Closed socket");
				}
				
				clientSocket.close();
				System.out.println("Closing client socketss");
			}

		} catch (IOException i) {
			i.printStackTrace();
		}
		
		System.out.println("Closing server socketss");

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
		Map<String, Resource> activeResourceMap = new HashMap<String, Resource>();
		
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
				
				if(operation.getOperation().equals(OperationMethod.TERMINATE)) {
					in.close();
					break;
				}
				
				Message perform_message = null;
				boolean sync_status = true;

				System.out.println("object not null");
				
				
				if (operation.getType().equals(OperationType.PERFORM)) {
					
					System.out.println("checking operation perform");
					
					Resource inputResource = operation.getInputResource();
					String filename = inputResource.getFilename();
					
					Resource resource = new Resource();
					if(activeResourceMap.get(filename) != null) {
						resource = activeResourceMap.get(filename);
					} else {
						activeResourceMap.put(filename, resource);
						resource.setFilename(inputResource.getFilename());
						resource.setSeek(inputResource.getSeek());
						resource.setWriteOffset(inputResource.getWriteOffset());
						resource.setFileContent(inputResource.getFileContent());

					}
					
					perform_message = operation.perform(this.getDATADIRECTORY(), resource);			
					
					
					if (perform_message.getStatusCode() == 200) {
						System.out.println("perform success");

						if(operation.getOperation().equals(OperationMethod.READ)) {
							o_out.writeObject(perform_message);
							continue;
						}
						
						if(!otherServers.containsKey(clientSocket.getInetAddress())) {					
							System.out.println("this server is connected with client");
							
							if(sockets.isEmpty()) {
								for (Map.Entry<InetAddress, Integer> entry : otherServers.entrySet()) {						
									Socket socket = new Socket(entry.getKey(), entry.getValue());

									OutputStream sock_out = socket.getOutputStream();
									ObjectOutputStream sock_o_out = new ObjectOutputStream(sock_out);

									InputStream sock_in = socket.getInputStream();
									ObjectInputStream sock_o_in = new ObjectInputStream(sock_in);

									sockets.put(entry.getKey(), new SocketMap(socket, sock_o_out, sock_o_in));
								}
							}
							System.out.println("attempt to synchronize");
							Message sync_message  = null;
							for (Map.Entry<InetAddress, SocketMap> entry : sockets.entrySet()) {
								sync_message = synchronize(operation, entry.getValue().getO_in(), entry.getValue().getO_out());
								if(sync_message.getStatusCode() != 200) {
									sync_status = false;
								}
							}
							

							if (sync_status) {
								System.out.println("all sync");

								operation.commit(this.getDATADIRECTORY(), resource);
								Operations commit_op = new Operations();
								commit_op.setType(OperationType.COMMIT);
								System.out.println(commit_op.getType().toString());
								
								for (Map.Entry<InetAddress, SocketMap> entry : sockets.entrySet()) {						
									sync_message = synchronize(commit_op, entry.getValue().getO_in(), entry.getValue().getO_out());
									if(sync_message.getStatusCode() != 200) {
										sync_status = false;
									}
								}
								
								if(!sync_status) {
									o_out.writeObject(sync_message);
								} else {
									o_out.writeObject(perform_message);
								}
								//System.out.println("closing socket");
							}
							//o_out.writeObject(sync_message);
							
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
									operation.commit(this.getDATADIRECTORY(), resource);
									m = new Message();
									m.setStatusCode(200);
									o_out.writeObject(m);
									System.out.println("commited");
								}
							}						
							//closing the socket
							//System.out.println("closing socket");

							//in.close();
						}
					}
					else {
						o_out.writeObject(perform_message);
					}
				}
			}
		}
		
		System.out.println("closing server socket");

	}

	public Message synchronize(Operations operation, ObjectInputStream o_in, ObjectOutputStream o_out) throws IOException, ClassNotFoundException {
		//send operation to other servers
		System.out.println("synchronize to other servers");	
		o_out.writeObject(operation);
		
		//wait for their status		
		Object object = o_in.readObject();
		
		Message m = null;
		
		if (object instanceof Message) {
			m = (Message)object;
			System.out.println( "got ack");
			return m;			
		} else {
			return null;
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

	public String getDATADIRECTORY() {
		return this.DATADIRECTORY;
	}

	public void setDATADIRECTORY(String dATADIRECTORY) {
		this.DATADIRECTORY = dATADIRECTORY;
	}



}
