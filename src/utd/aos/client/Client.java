package utd.aos.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;

import utd.aos.utils.Message;
import utd.aos.utils.MessageRecord;
import utd.aos.utils.MutexMessage;
import utd.aos.utils.MutexMessage.MessageType;
import utd.aos.utils.Operations;
import utd.aos.utils.Resource;
import utd.aos.utils.SocketMap;
import utd.aos.utils.Operations.OperationMethod;
import utd.aos.utils.Operations.OperationType;
import utd.aos.utils.Request;

public class Client implements Runnable{
	
	public static int id;
	public static Map<String, SocketMap> quorum;
	public static Map<String, SocketMap> allClientsSockets;
	public static Map<Integer, InetSocketAddress> otherClients;
	public static Map<String, Integer> hostIdMap;	
	public static Map<String, SocketMap> allClientsListenerSockets;
	
	public static SocketMap serverSocketMap;
	
	public static InetAddress ip;
	public static Integer port;
	
	public static Semaphore mutex = new Semaphore(1);	
	
	public static Semaphore gotallReplies = new Semaphore(1);
	public static Semaphore gotallReleases = new Semaphore(1);
	
	public static int pendingReleaseToReceive;
	public static int gotFailed;
	public static int sentYield;
	public static int sentEnquire;
	
	public static long clock;
	
	public static boolean inprocess = false;
	
	public static Map<Integer, Boolean> pendingRepliesToReceive = new HashMap<Integer, Boolean>();
	public static Map<Integer, Boolean> sentYieldMessageTo = new HashMap<Integer, Boolean>();
	
	public static Queue<Integer> request_fifo = new LinkedList<Integer>(); 
	public static Queue<Integer> fail_fifo = new LinkedList<Integer>(); 

	public static int count = 1;
	public static MessageRecord record = new MessageRecord();
	
	public static Queue<Request> request_q;
	public static Queue<Request> enquire_q;
	
	boolean curr_req_done = false;
	public Client() {
	
	}

	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		init();
		Random rand = new Random();
		
		
		while(count <= 40) {

			curr_req_done = false;
			Integer delay = rand.nextInt(40);
			
			delay += 10;
			
			reset();

			try {
				
				Thread.sleep(delay);

				Resource resource = new Resource();
				resource.setFilename("test");

				Operations operation = new Operations();
				operation.setOperation(OperationMethod.WRITE);
				operation.setType(OperationType.PERFORM);
				operation.setInputResource(resource);
				operation.setArg(id+" : "+count+" : "+InetAddress.getLocalHost().getHostName()+"\n");

				
				System.out.println("--adding my request to fifo--");
				
			
				Request request = new Request(id, null, MessageType.REQUEST);
				request_q.add(request);
				while(!request_q.isEmpty() || !curr_req_done) {

					if(!request_q.isEmpty()) {
						Request req = request_q.remove();

						if(req.getType().equals(MessageType.REQUEST)) {
							handleRequest(req, operation);
						}
						else if(req.getType().equals(MessageType.REPLY)) {
							handleReply(req);
						}
						else if(req.getType().equals(MessageType.RELEASE)) {
							handleRelease(req);
						}
						else if(req.getType().equals(MessageType.FAILED)) {
							handleFail(req);
						}
						else if(req.getType().equals(MessageType.ENQUIRE)) {
							handleEnquire(req);
						}
						else if(req.getType().equals(MessageType.YIELD)) {
							handleYield(req);
						}
						else if(req.getType().equals(MessageType.GRANT)) {
							handleGrant(req);
						}
					}

					Thread.sleep(10);
				}

				
			} catch (Exception e) {
				e.printStackTrace();
				try{
					shutdown();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			
			//System.out.print("Herereeeeee");
			//printreport();
			count++;

		}
		
		try {
			shutdown();			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handleRequest(Request req, Operations operation) throws IOException, InterruptedException, ClassNotFoundException {		
		System.out.println("----- "+req.getId()+" ------");
		if(req.getId() == id) {
	
			inprocess = true;
			new Thread(new ClientMainThread(operation)).start();

		} else {	
			
			SocketMap socketmap = req.getSocketmap();
			String socketHostname = socketmap.getAddr().getHostName();

			int client_id = req.getId();
			System.out.println("--RECV REQUEST "+socketHostname+"--");

			//clock = Math.max(message.getClock(), clock) + 1;
			MutexMessage return_message = new MutexMessage();

			if(pendingReleaseToReceive == 0 && !pendingRepliesToReceive.containsKey(client_id)
					&& gotallReplies.availablePermits() >= 0 && gotallReleases.availablePermits() == 1) {

				record.request++;

				pendingReleaseToReceive = client_id;
				
				System.out.println("THREAD CLIENT - Release acquire");
				gotallReleases.acquire();
				
				return_message.setId(id);
				return_message.setType(MessageType.REPLY);

				System.out.println("--SENT REPLY to "+socketHostname+"--");

				socketmap.getO_out().writeObject(return_message);

				record.reply++;

			} else {

				//lower id = high priority
				if(pendingReleaseToReceive < client_id) {

					record.request++;

					return_message.setId(id);
					return_message.setType(MessageType.FAILED);

					System.out.println("--SENT FAILED "+socketHostname+"--");

					socketmap.getO_out().writeObject(return_message);

					record.fail++;

				} else {

					if(pendingReleaseToReceive != id) {

						return_message.setId(id);
						return_message.setType(MessageType.ENQUIRE);

						record.request++;

						InetSocketAddress addr = otherClients.get(pendingReleaseToReceive);
						String client_hostname = addr.getHostName();
						SocketMap client_socket_map = allClientsSockets.get(client_hostname);

						System.out.println("--SENT ENQUIRE  "+client_hostname+"--");

						client_socket_map.getO_out().writeObject(return_message);

						record.enquire++;

						sentEnquire = 1;

					} else {

						request_q.add(req);
					}

				}
			} 
		}
	}
	
	public void handleReply(Request req) {
		SocketMap socketmap = req.getSocketmap();
		String socketHostname = socketmap.getAddr().getHostName();
		int client_id = req.getId();
		
		System.out.println("--RECV REPLY "+socketHostname+"--");
		
		pendingRepliesToReceive.remove(client_id);

		if(pendingRepliesToReceive.size() == 0) {
			
			System.out.println("THREAD MAIN - Reply release");
			gotallReplies.release();
		}
		
		record.reply++;
	}
	
	public void handleRelease(Request req) {
		
		SocketMap socketmap = req.getSocketmap();
		String socketHostname = socketmap.getAddr().getHostName();

		System.out.println("---RECV RELEASE  "+ socketHostname+" --");

		pendingReleaseToReceive = 0;
		gotallReleases.release();
		
		record.release++;

	}
	public void handleEnquire(Request req) throws IOException {

		SocketMap socketmap = req.getSocketmap();
		String socketHostname = socketmap.getAddr().getHostName();
		int client_id = req.getId();
		System.out.println("--RECV ENQUIRE "+socketHostname);
		MutexMessage return_message = new MutexMessage();
		
		if(pendingReleaseToReceive == 0 || gotFailed == 1 || sentYield == 1 ) {
			
			record.enquire++;

			pendingReleaseToReceive = client_id;

			return_message.setId(id);
			return_message.setType(MessageType.REPLY);
			
			System.out.println("--SENT REPLY "+socketHostname+"--");

			socketmap.getO_out().writeObject(return_message);
			
			record.reply++;
			
			handleEnquireQ();
			
		} else if (sentEnquire == 1){
			
			record.enquire++;

			return_message.setId(id);
			return_message.setType(MessageType.YIELD);

			sentYieldMessageTo.put(client_id, true);
			sentYield = 1;

			System.out.println("--SENT YIELD "+socketHostname+"--");

			socketmap.getO_out().writeObject(return_message);

			record.yield++;
			
			handleEnquireQ();
			
		} else {
			
			enquire_q.add(req);
			
		} 

	}
	
	public void handleEnquireQ() throws IOException {
		
		while(!enquire_q.isEmpty()) {
			
			Request req = enquire_q.remove();
			
			SocketMap socketmap = req.getSocketmap();
			String socketHostname = socketmap.getAddr().getHostName();
			int client_id = req.getId();
			
			System.out.println("--RECV ENQUIRE "+socketHostname);
			MutexMessage return_message = new MutexMessage();
			
			record.enquire++;

			return_message.setId(id);
			return_message.setType(MessageType.YIELD);

			sentYieldMessageTo.put(client_id, true);
			sentYield = 1;

			System.out.println("--SENT YIELD "+socketHostname+"--");

			socketmap.getO_out().writeObject(return_message);

			record.yield++;
		}
	}
	
	public void handleFail(Request req) throws IOException {
		SocketMap socketmap = req.getSocketmap();
		String socketHostname = socketmap.getAddr().getHostName();
		
		System.out.println("--RECV FAILED "+socketHostname+"-");
		gotFailed = 1;
		
		record.fail++;
		
		handleEnquireQ();
	}
	
	public void handleYield(Request req) throws IOException, InterruptedException {

		SocketMap socketmap = req.getSocketmap();
		String socketHostname = socketmap.getAddr().getHostName();
		int client_id = req.getId();

		if(pendingReleaseToReceive == 0 && !pendingRepliesToReceive.containsKey(client_id)
				&& gotallReplies.availablePermits() >= 0 && gotallReleases.availablePermits() == 1) {


			record.yield++;
			System.out.println("--RECV YIELD "+socketHostname);

			//request_fifo.add(client_id);
			Iterator<Request> iterator = request_q.iterator();
			int min_id_queued = 100;
			while(iterator.hasNext()) {

				Request r = iterator.next();
				int i = r.getId();
				if(i < min_id_queued) {
					min_id_queued = i;
				}
			}

			while(iterator.hasNext()) {
				Request r = iterator.next();
				int i = r.getId();
				if(i == min_id_queued) {
					iterator.remove();
				}
			}

			if(min_id_queued == 100 || min_id_queued > client_id) {
				min_id_queued = client_id;
			}

			pendingReleaseToReceive = min_id_queued;
			
			System.out.println("THREAD CLIENT - Release acquire");
			gotallReleases.acquire();

			sentEnquire = 0;

			InetSocketAddress addr = otherClients.get(min_id_queued);

			MutexMessage return_message = new MutexMessage();

			String client_hostname = addr.getHostName();
			SocketMap client_socket_map = allClientsSockets.get(client_hostname);

			return_message.setId(id);
			return_message.setType(MessageType.GRANT);

			System.out.println("--SENT GRANT "+client_hostname+"--");

			client_socket_map.getO_out().writeObject(return_message);
		} else {
			request_q.add(req);
		}

	}

	public void handleGrant(Request req) {
		SocketMap socketmap = req.getSocketmap();
		String socketHostname = socketmap.getAddr().getHostName();
		int client_id = req.getId();
		
		System.out.println("--RECV GRANT "+socketHostname+"--");
		
		if(sentYieldMessageTo.containsKey(client_id)) {
			sentYieldMessageTo.remove(client_id);
			if(sentYieldMessageTo.size() == 0) {
				sentYield = 0;
			}
		}
		
		if(pendingRepliesToReceive.containsKey(client_id)) {
			pendingRepliesToReceive.remove(client_id);
			if(pendingRepliesToReceive.size() == 0) {
				
				System.out.println("THREAD MAIN - Relpy release");
				gotallReplies.release();
			}
		}
		
		if(pendingReleaseToReceive == client_id)  {
			pendingReleaseToReceive = 0;
		}
		record.grant++;
	}
	
	
	public void printreport(){
		File file = new File("record_"+id);
		try {
			if(!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file);
			
			int total_message = record.request + record.reply + record.release + record.fail + record.enquire + record.yield + record.grant;
			
			String report = "For Request: "+count+"\n";
			report += "TIME: "+record.time+" milis\n";
			report += "TOTALMESSAGES:"+total_message+"\n";
			report += "REQUEST: "+record.request+"\n";
			report += "REPLY: "+record.reply+"\n";
			report += "RELEASE: "+record.release+"\n";
			report += "FAIL: "+record.fail+"\n";
			report += "ENQUIRE: "+record.enquire+"\n";
			report += "YIELD: "+record.yield+"\n";
			report += "GRANT: "+record.grant+"\n";
			
			report += "---------------\n";
			
			fw.write(report);
			fw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public void init() {
		
		//Check for all clients to be started
		for(Map.Entry<Integer, InetSocketAddress> entry: otherClients.entrySet()) {
			InetSocketAddress addr = entry.getValue();
			while(true) {
			    try {	    	
					Socket socket = new Socket(addr.getHostName(), addr.getPort());
					InputStream in = socket.getInputStream();
					OutputStream out = socket.getOutputStream();

					ObjectInputStream o_in = new ObjectInputStream(in);
					ObjectOutputStream o_out = new ObjectOutputStream(out);
					
					System.out.println("--Saving streams--");
					
					MutexMessage testmessage = new MutexMessage();
					testmessage.setId(id);
					testmessage.setType(MessageType.TEST);
					o_out.writeObject(testmessage);

					SocketMap socketmap = new SocketMap(socket, o_out, o_in, addr);
					
					if(quorum.containsKey(addr.getHostName())) {

						quorum.put(addr.getHostName(), socketmap);
					}
					if(allClientsSockets == null) {
						allClientsSockets = new HashMap<String, SocketMap>();
						allClientsSockets.put(addr.getHostName(), socketmap);
					} else {
						allClientsSockets.put(addr.getHostName(), socketmap);
					}
					
					new Thread(new ClientsServerThreadListener(socketmap)).start();
					
					System.out.println("Connect success: "+ip.getHostName()+"->"+addr.getHostName());

					if(request_q == null) {
						//request_q = new PriorityQueue<Request>(30, new RequestComparator());
						request_q = new LinkedList<Request>();
					}
					if(enquire_q == null) {
						enquire_q = new LinkedList<Request>();
					}
					
					break;
			    
			    } catch(ConnectException e) {
			    	System.out.println("Connect failed, waiting and trying again: "+ip.getHostName()+"->"+addr.getHostName());
			        try {
			            Thread.sleep(1000);
			        }
			        catch(InterruptedException ie) {
			            ie.printStackTrace();
			        }
			    } catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
		}

	}
	
	public void shutdown() throws IOException, ClassNotFoundException {
		Operations operation = new Operations();
		operation.setOperation(OperationMethod.TERMINATE);
		serverSocketMap.getO_out().writeObject(operation);
		serverSocketMap.getO_in().readObject();
		
		if(!allClientsListenerSockets.isEmpty()) {
			for (Map.Entry<String, SocketMap> entry : allClientsListenerSockets.entrySet()) {
				if(!entry.getValue().getSocket().isClosed()) {
					entry.getValue().getO_out().close();
				}
			}
		}
		
		if(!allClientsSockets.isEmpty()) {
			for (Map.Entry<String, SocketMap> entry : allClientsSockets.entrySet()) {
				if(!entry.getValue().getSocket().isClosed()) {
					entry.getValue().getO_out().close();
				}
			}
		}
	}
	
	public void reset() {
		
		record.enquire = 0;
		record.fail = 0;
		record.release = 0;
		record.reply = 0;
		record.request = 0;
		record.yield = 0;
		record.grant = 0;
		record.time = 0;
		
		/*
		pendingReleaseToReceive = 0;
		gotFailed = 0;
		sentYield = 0;
		
		pendingRepliesToReceive = new HashMap<Integer, Boolean>();
		gotFailedMessageFrom = new HashMap<Integer, Boolean>();
		sentYieldMessageTo = new HashMap<Integer, Boolean>();
		*/
		
	}
	
	public boolean getMutex() throws InterruptedException, IOException {
		
		
		//System.out.println("--WAIT allreply sema (mutex)--");
		//gotallReplies.acquire();
		
		for(Map.Entry<String, SocketMap> entry: quorum.entrySet()) {
			SocketMap quorum_client = entry.getValue();
			String hostname = quorum_client.getAddr().getHostName();
			Integer client_id = hostIdMap.get(hostname);
			MutexMessage message = new MutexMessage(id, MessageType.REQUEST);
			
			
			pendingRepliesToReceive.put(client_id, true);
			
			quorum_client.getO_out().writeObject(message);
			
			System.out.println("--SENT REQUEST "+hostname+"--");
			
			//ClientsServerThreadListener clientServer = new ClientsServerThreadListener(quorum_client);
			//Thread t = new Thread(clientServer);
			//t.start();
		}
		
		return true;
	}
	
	public void sendRelease() throws IOException {
		System.out.println("--send release to all--");
		for(Map.Entry<String, SocketMap> entry: allClientsSockets.entrySet()) {
			SocketMap quorum_client = entry.getValue();
			MutexMessage message = new MutexMessage(id, MessageType.RELEASE);
			quorum_client.getO_out().writeObject(message);
		}
		record.release += quorum.size();
	}
	
	public Message request(Operations operation) throws IOException, ClassNotFoundException {	
		//creating the request
		System.out.println("--sending request to server--");

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
