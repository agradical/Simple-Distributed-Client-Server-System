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
	
	public static int id;
	public static Map<String, SocketMap> quorum;
	public static Map<Integer, InetSocketAddress> otherClients;
	public static Map<String, Integer> hostIdMap;
	
	public static SocketMap serverSocketMap;
	
	public static InetAddress ip;
	public static Integer port;
	
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
	
	public Client() {
		
	}

	//MutexAlgorithm algo;
	
	//blocked: either me executing critical section or didn't get the release from last reply.
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		init();
		int count = 1;
		while(count <= 40) {
			Random rand = new Random();
			Integer delay = rand.nextInt(40);
			delay += 10;

			try {
				Thread.sleep(delay);

				Resource resource = new Resource();
				resource.setFilename("test");

				Operations operation = new Operations();
				operation.setOperation(OperationMethod.WRITE);
				operation.setArg(id+" : "+count+" : "+InetAddress.getLocalHost().getHostName()+"\n");

				if(state.equals(State.BLOCKED)) {
					gotallReleases.acquire();
					gotallReleases.release();
				}
				state = State.BLOCKED;
				gotallReleases.acquire();
				System.out.println("--got mutex semaphore--");
				if(getMutex()) {
					System.out.println("--acquired mutex--");
					gotallReplies.acquire();
					request(operation);
					gotallReplies.release();
				}
				state = State.AVAILABLE;
				System.out.println("--done with CS--");
				sendRelease();
				gotallReleases.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		shutdown();			
	}

	
	protected void listen(Socket socket) throws IOException, InterruptedException {
		String socketHostname = socket.getInetAddress().getHostName();
		
		InputStream in = socket.getInputStream();
		OutputStream out = socket.getOutputStream();

		ObjectOutputStream o_out = new ObjectOutputStream(out);
		ObjectInputStream o_in = new ObjectInputStream(in);
	
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
					gotallReleases.acquire();
					state = State.BLOCKED;
					System.out.println("--got request message from "+socketHostname+"--");
					return_message.setId(id);
					return_message.setType(MessageType.REPLY);
					pendingReleasesToReceive.put(message.getId(), true);
					System.out.println("--REPLY--");

					o_out.writeObject(return_message);
				}
			} else if (state.equals(State.BLOCKED)) {
				if(message.getType().equals(MessageType.RELEASE)) {
					if(pendingReleasesToReceive.get(message.getId())) {
						pendingReleasesToReceive.remove(message.getId());
						if(pendingReleasesToReceive.size() == 0) {
							gotallReleases.release();
							state = State.AVAILABLE;
						}
					}
				}
				
				if(message.getType().equals(MessageType.REPLY) 
						&& pendingRepliesToReceive.get(message.getId())) {
					pendingRepliesToReceive.remove(message.getId());
					if(pendingRepliesToReceive.size()==0) {
						gotallReplies.release();
						System.out.println("--releasing allreply mutex--");
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
					if(quorum.containsKey(addr.getHostName())) {
						InputStream in = socket.getInputStream();
						OutputStream out = socket.getOutputStream();

						ObjectInputStream o_in = new ObjectInputStream(in);
						ObjectOutputStream o_out = new ObjectOutputStream(out);
						System.out.println("--Saving streams--");
						MutexMessage testmessage = new MutexMessage();
						testmessage.setType(MessageType.TEST);
						o_out.writeObject(testmessage);
						quorum.put(addr.getHostName(), (new SocketMap(socket, o_out, o_in, addr)));
						break;
					} else {
						//socket.close();
					}
					System.out.println("Connect success: "+ip.getHostName()+"->"+addr.getHostName());
					break;
			    } catch(ConnectException e) {
			    	//e.printStackTrace();
			        System.out.println("Connect failed, waiting and trying again: "+ip.getHostName()+"->"+addr.getHostName());
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

	}
	
	public void shutdown() {
		
	}
	
	public boolean getMutex() throws InterruptedException, IOException {
		System.out.println("--trying to acquire mutex--");
		gotallReplies.acquire();
		for(Map.Entry<String, SocketMap> entry: quorum.entrySet()) {
			SocketMap quorum_client = entry.getValue();
			String hostname = quorum_client.getAddr().getHostName();
			Integer client_id = hostIdMap.get(hostname);
			System.out.println("--sending request message to "+hostname+"--");
			MutexMessage message = new MutexMessage(id, MessageType.REQUEST);
			quorum_client.getO_out().writeObject(message);
			pendingRepliesToReceive.put(client_id, true);
		}
		return true;
	}
	
	public void sendRelease() throws IOException {
		System.out.println("--semd release to acquired mall--");
		for(Map.Entry<String, SocketMap> entry: quorum.entrySet()) {
			SocketMap quorum_client = entry.getValue();
			MutexMessage message = new MutexMessage(id, MessageType.RELEASE);
			quorum_client.getO_out().writeObject(message);
		}
	}
	
	public Message request(Operations operation) throws IOException, ClassNotFoundException {	
		//creating the request
		System.out.println("--sending request--");

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

	public SocketMap getServerSocketMap() {
		return serverSocketMap;
	}

	public Map<String, SocketMap> getQuorum() {
		return quorum;
	}


	public InetAddress getIp() {
		return ip;
	}

	public Integer getPort() {
		return port;
	}


	public Map<Integer, InetSocketAddress> getOtherClients() {
		return otherClients;
	}


	public Map<String, Integer> getHostIdMap() {
		return hostIdMap;
	}


}
