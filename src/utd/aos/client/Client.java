package utd.aos.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

import utd.aos.utils.Message;
import utd.aos.utils.MutexMessage;
import utd.aos.utils.MutexMessage.MessageType;
import utd.aos.utils.Operations;
import utd.aos.utils.Resource;
import utd.aos.utils.SocketMap;
import utd.aos.utils.Operations.OperationMethod;

public class Client implements Runnable{
	
	private int id;
	private Map<InetSocketAddress, SocketMap> quorum;
	private Map<Integer, InetSocketAddress> otherClients;
	private Map<String, Integer> hostIdMap;
	
	private SocketMap serverSocketMap;
	
	private InetAddress ip;
	private Integer port;
	
	public static Semaphore mutex = new Semaphore(1);	
	public static Semaphore gotallReplies = new Semaphore(1);
	public static Semaphore gotallReleases = new Semaphore(1);

	public static Map<Integer, Boolean> pendingReleasesToReceive = new HashMap<Integer, Boolean>();
	public static Map<Integer, Boolean> pendingRepliesToReceive = new HashMap<Integer, Boolean>();
	
	public static Integer repliedTo;
	public static Integer sentFailedMessageTo;
	public static Integer sentEnquireMessageTo;
	public static Integer sentYieldMessageTo;
	public static Integer sentGrantMessageTo;

	public static boolean requesting;
	
	public State state = State.AVAILABLE;
	public enum State {
		AVAILABLE, BLOCKED 
	}
	
	//MutexAlgorithm algo;
	
	//blocked: either me executing critical section or didn't get the release from last reply.
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(this.getPort());
			while(true) {
				//Accepting the client connection
				Socket clientSocket = serverSocket.accept();
				try {	
					
					talk(clientSocket);
					
				} catch (Exception e) {
					e.printStackTrace();
				}				
				clientSocket.close();
			}

		} catch (IOException i) {
			i.printStackTrace();
		}
		
	}
	
	private void talk(Socket socket) throws IOException, InterruptedException {
		InputStream in = socket.getInputStream();
		OutputStream out = socket.getOutputStream();
		
		ObjectInputStream o_in = new ObjectInputStream(in);
		ObjectOutputStream o_out = new ObjectOutputStream(out);
		
		while(!socket.isClosed()) {

			Object object = null;
			try {
				object = o_in.readObject();
			} catch (Exception e) {
				//Closing connection with other servers in case of termination from client
				System.out.println("--Closing connection--");
				
			}
			
			MutexMessage message = null;
			message = (MutexMessage)object;
			MutexMessage return_message = message;
			if(state.equals(State.AVAILABLE)) {
				//if message is a request message send a reply and mark as blocked 
				//until get the release from the corresponding process
				if(message.getType().equals(MessageType.REQUEST)) {
					return_message.setType(MessageType.REPLY);
					pendingReleasesToReceive.put(id, true);
					state = State.BLOCKED;
					o_out.writeObject(return_message);
				}
			} else if (state.equals(State.BLOCKED)) {
								
				if(message.getType().equals(MessageType.RELEASE)) {
					if(pendingReleasesToReceive.get(message.getId())) {
						pendingReleasesToReceive.remove(message.getId());
						if(pendingReleasesToReceive.size() == 0) {
							state = State.AVAILABLE;
						}
					}
				}
				
				if(message.getType().equals(MessageType.REPLY) 
						&& pendingRepliesToReceive.get(message.getId())) {
					pendingRepliesToReceive.remove(message.getId());
					if(pendingRepliesToReceive.size()==0) {
						gotallReplies.release();
					}
				}
			}	
			
		}
		
	}
	
	public void init() {
		
		//Check for all clients to be started
		for(Map.Entry<Integer, InetSocketAddress> entry: otherClients.entrySet()) {
			InetSocketAddress addr = entry.getValue();
			while(true) {
			    try {	    	
					Socket socket = new Socket(addr.getHostName(), addr.getPort());
					socket.close();
					break;
			    } catch(ConnectException e) {
			        System.out.println("Connect failed, waiting and trying again");
			        try {
			            Thread.sleep(1000);
			        }
			        catch(InterruptedException ie) {
			            ie.printStackTrace();
			        }
			    } catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		}
		
		for(Map.Entry<InetSocketAddress, SocketMap> entry: quorum.entrySet()) {
			InetSocketAddress addr = entry.getKey();

			try {	    	
				Socket socket = new Socket(addr.getHostName(), addr.getPort());

				InputStream in = socket.getInputStream();
				OutputStream out = socket.getOutputStream();

				ObjectInputStream o_in = new ObjectInputStream(in);
				ObjectOutputStream o_out = new ObjectOutputStream(out);

				entry.setValue(new SocketMap(socket, o_out, o_in));
				break;

			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 

		}
	}
	
	public void shutdown() {
		
	}
	
	public void execute(String filename) throws Exception {
		init();
		int count = 1;
		while(count <= 40) {
			Random rand = new Random();
			Integer delay = rand.nextInt(40);
			delay += 10;

			Thread.sleep(delay);
			
			Resource resource = new Resource();
			resource.setFilename(filename);
			
			Operations operation = new Operations();
			operation.setOperation(OperationMethod.WRITE);
			operation.setArg(id+" : "+count+" : "+InetAddress.getLocalHost().getHostName()+"\n");
		
			if(state.equals(State.BLOCKED)) {
				gotallReleases.acquire();
				gotallReleases.release();
			}
			state = State.BLOCKED;
			mutex.acquire();
			if(getMutex()) {
				gotallReplies.acquire();
				request(operation);
				gotallReplies.release();
			}
			state = State.AVAILABLE;
			sendRelease();
			mutex.release();
		}
		shutdown();
	}
	
	public boolean getMutex() throws InterruptedException, IOException {
		gotallReplies.acquire();
		for(Map.Entry<InetSocketAddress, SocketMap> entry: quorum.entrySet()) {
			SocketMap quorum_client = entry.getValue();
			String hostname = entry.getKey().getHostName();
			Integer client_id = hostIdMap.get(hostname);
			MutexMessage message = new MutexMessage(id, MessageType.REQUEST);
			quorum_client.getO_out().writeObject(message);
			pendingRepliesToReceive.put(client_id, true);
		}
		return true;
	}
	
	public void sendRelease() throws IOException {
		for(Map.Entry<InetSocketAddress, SocketMap> entry: quorum.entrySet()) {
			SocketMap quorum_client = entry.getValue();
			MutexMessage message = new MutexMessage(id, MessageType.RELEASE);
			quorum_client.getO_out().writeObject(message);
		}
	}
	
	public Message request(Operations operation) throws IOException, ClassNotFoundException {	
		//creating the request
		if(operation.getOperation().equals(OperationMethod.CREATE)) {
			Resource resource = operation.getInputResource();
			File file = new File(resource.getFilename());
			if(file.exists()) {
				String fileContent = "";
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line = "";
				while((line = br.readLine()) != null ) {
					fileContent += line;
				}					
				resource.setFileContent(fileContent);
				br.close();
				
				operation.setInputResource(resource);
			}
		}
		
		//sends the operation request
		serverSocketMap.getO_out().writeObject(operation);
		
		//wait for the response
		Object object = serverSocketMap.getO_in().readObject();
		
		
		Message m;
		
		if (object instanceof Message) {
			 m = (Message)object;
			 return m;
		} else {
			return null;
		}
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public void addClientToQuorum(InetSocketAddress client, SocketMap socket) {
		if(this.getQuorum() == null) {
			Map<InetSocketAddress, SocketMap> quorum = new HashMap<InetSocketAddress, SocketMap>();
			quorum.put(client, socket);
		} else {
			Map<InetSocketAddress, SocketMap> quorum = this.getQuorum();
			quorum.put(client, socket);
		}
	}

	public SocketMap getServerSocketMap() {
		return serverSocketMap;
	}

	public void setServerSocketMap(SocketMap serverSocketMap) {
		this.serverSocketMap = serverSocketMap;
	}

	public Map<InetSocketAddress, SocketMap> getQuorum() {
		return quorum;
	}

	public void setQuorum(Map<InetSocketAddress, SocketMap> quorum) {
		this.quorum = quorum;
	}

	public InetAddress getIp() {
		return ip;
	}

	public void setIp(InetAddress ip) {
		this.ip = ip;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Map<Integer, InetSocketAddress> getOtherClients() {
		return otherClients;
	}

	public void setOtherClients(Map<Integer, InetSocketAddress> otherClients) {
		this.otherClients = otherClients;
	}

	public Map<String, Integer> getHostIdMap() {
		return hostIdMap;
	}

	public void setHostIdMap(Map<String, Integer> hostIdMap) {
		this.hostIdMap = hostIdMap;
	}

}
