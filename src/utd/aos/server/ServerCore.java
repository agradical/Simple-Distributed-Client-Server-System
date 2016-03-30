package utd.aos.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import utd.aos.utils.Message;
import utd.aos.utils.Operations;
import utd.aos.utils.Resource;
import utd.aos.utils.SocketMap;
import utd.aos.utils.Operations.OperationMethod;
import utd.aos.utils.Operations.OperationType;

public class ServerCore implements Server {
	
	public static Map<InetAddress, Integer> otherServers;
	public static InetAddress ip;
	public static Integer port;
	public static Map<String, Resource> activeResourceMap = new HashMap<String, Resource>();

	private Socket socket;
	
	public static String DATADIRECTORY = "data";
	
	public ServerCore() {
		
	}
	public ServerCore(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public Server getServer() {
		return this;
	}
	
	@Override
	public void setServer(InetAddress addr, Integer port_num) {
		ip = addr;
		port = port_num;
	}
	
	@Override
	public void addServer(InetAddress server, Integer port) {
		if (otherServers == null) {
			otherServers = new HashMap<InetAddress, Integer>();
		}
		otherServers.put(server, port);
	}


	@Override
	public void run() {		
		try {
			execute(socket);
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
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

			Object object = null;
			try {
				//Reading Object stream
				System.out.println("---------------------------");
				object = o_in.readObject();
			} catch (Exception e) {
				//Closing connection with other servers in case of termination from client
				System.out.println("--Closing connection--");
				if(!sockets.isEmpty()) {
					for (Map.Entry<InetAddress, SocketMap> entry : sockets.entrySet()) {
						entry.getValue().getO_out().close();
					}
				}
				break;
			}
			
			Operations operation = null;
			//Checking if Object received is of type Operations
			if (object instanceof Operations) {
				operation = (Operations)object;
			}

			if (operation != null) {
				System.out.println("--Closing connection--");
				//In case of Termination from client close connections with other servers
				//Line added for graceful termination
				if(operation.getOperation().equals(OperationMethod.TERMINATE)) {
					in.close();
					if(!sockets.isEmpty()) {
						for (Map.Entry<InetAddress, SocketMap> entry : sockets.entrySet()) {
							entry.getValue().getO_out().close();
						}
					}
					break;
				}
				
				Message perform_message = null;
				boolean sync_status = true;	
				
				//Operation can be either to perform locally or signal to commit
				//after performed on every server
				if (operation.getType().equals(OperationType.PERFORM)) {
					
					System.out.println(operation.getOperation().toString()+ " Request");
					
					Resource inputResource = operation.getInputResource();
					String filename = inputResource.getFilename();
					
					//Keeping ResourceMap active for a particular session
					//and Update in case of new Seek position update
					Resource resource = new Resource();
					if(activeResourceMap.get(filename) != null) {
						resource = activeResourceMap.get(filename);
					} else {
						activeResourceMap.put(filename, resource);
						resource.setFilename(inputResource.getFilename());
						resource.setSeek(inputResource.getSeek());
						resource.setWriteOffset(inputResource.getWriteOffset());
						System.out.println("--write offst main: "+resource.getWriteOffset());

						resource.setFileContent(inputResource.getFileContent());
					}
					
					//operation.setInputResource(resource);
					perform_message = operation.perform(this.getDATADIRECTORY(), resource);			
					
					if (perform_message.getStatusCode() == 200) {
						if(operation.getOperation().equals(OperationMethod.READ)) {
							perform_message.setServerId(ip.getHostName());
							o_out.writeObject(perform_message);
							continue;
						}
						
						//Checking if the server is connected with client
						//If connected with client this server will be responsible to sync the operation.
						//So creating connections with other servers
						if(!otherServers.containsKey(clientSocket.getInetAddress())) {												
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
							
							//Attempt to synchronize the operation
							
							Message sync_message  = null;
							for (Map.Entry<InetAddress, SocketMap> entry : sockets.entrySet()) {
								sync_message = synchronize(operation, entry.getValue().getO_in(), entry.getValue().getO_out());
								if(sync_message.getStatusCode() != 200) {
									sync_status = false;
								}
							}
							
							if (sync_status) {
								
								//If Synced to all servers send commit signal to all servers
								//to finally commit the operation
								System.out.println("--Operation Synced successfully--");
								operation.commit(this.getDATADIRECTORY(), resource);
								Operations commit_op = new Operations();
								commit_op.setType(OperationType.COMMIT);
								
								for (Map.Entry<InetAddress, SocketMap> entry : sockets.entrySet()) {						
									sync_message = synchronize(commit_op, entry.getValue().getO_in(), entry.getValue().getO_out());
									if(sync_message.getStatusCode() != 200) {
										sync_status = false;
									}
								}
								
								//Send response back to Client
								if(!sync_status) {
									sync_message.setServerId(ip.getHostName());
									o_out.writeObject(sync_message);
								} else {
									System.out.println("--Operation committed--");
									perform_message.setServerId(ip.getHostName());
									o_out.writeObject(perform_message);
								}
							} else {
								sync_message.setServerId(ip.getHostName());
								o_out.writeObject(sync_message);
							}
						} else {
							
							//If not connected with client 
							//sends back the success signal
							Message m = new Message();
							m.setStatusCode(200);
							m.setServerId(ip.getHostName());
							o_out.writeObject(m);
							
							//wait for commit signal
							object = o_in.readObject();
							if (object instanceof Operations) {
								Operations op = (Operations)object;
								System.out.println(op.getType().toString());
								if(op.getType().equals(OperationType.COMMIT)) {
									operation.commit(this.getDATADIRECTORY(), resource);
									m = new Message();
									m.setStatusCode(200);
									m.setServerId(ip.getHostName());
									o_out.writeObject(m);
								}
							}
						}
					}
					else {
						perform_message.setServerId(ip.getHostName());
						o_out.writeObject(perform_message);
					}
				}
			}
		}
	}

	public Message synchronize(Operations operation, ObjectInputStream o_in, ObjectOutputStream o_out) throws IOException, ClassNotFoundException {
		
		//send operation to other servers
		o_out.writeObject(operation);
		
		//wait for their ACK		
		Object object = o_in.readObject();
		
		Message m = null;		
		if (object instanceof Message) {
			m = (Message)object;
			return m;			
		} else {
			return null;
		}

	}
	
	public InetAddress getIp() {
		return ip;
	}

	public void setIp(InetAddress addr) {
		ip = addr;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port_num) {
		port = port_num;
	}

	public String getDATADIRECTORY() {
		return DATADIRECTORY;
	}

	public void setDATADIRECTORY(String dATADIRECTORY) {
		DATADIRECTORY = dATADIRECTORY;
	}



}
