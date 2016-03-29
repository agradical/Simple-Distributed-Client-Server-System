package utd.aos.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import utd.aos.utils.Message;
import utd.aos.utils.Operations;
import utd.aos.utils.Resource;
import utd.aos.utils.SocketMap;
import utd.aos.utils.Operations.OperationMethod;
import utd.aos.utils.Operations.OperationType;

public class Main {
	public static final int NTHREADS = 20;
    public static final ExecutorService exec = Executors.newFixedThreadPool(NTHREADS);
  
	public static void main (String[] args) throws Exception {
		
		//Server configuration
		String filename = "server.list";
		File file = new File(filename);	
		String server = "";
		int server_port = 0;
		if(file.exists()) {
			BufferedReader br = new BufferedReader(new FileReader(file));
			List<String> serverlist = new ArrayList<String>();
			String _servers = "";
			while((_servers = br.readLine()) != null) {
				String _server = _servers;
				serverlist.add(_server);
			}
			br.close();
			
			//Select random server from list to connect
			Random rand = new Random();
			Integer id = rand.nextInt(serverlist.size());
			
			server = serverlist.get(id).split(" ")[0];
			server_port = Integer.parseInt(serverlist.get(id).split(" ")[1]);
			
			if(server.equals("")) {
				throw new Exception("No Host found");
			}
		}	
		
		//Client and Quorum Configuration
		//Client client = new Client();
		String client_filename = "client_id.list";
		File client_id_file = new File(client_filename);
		
		Map<Integer, InetSocketAddress> client_map = new HashMap<Integer, InetSocketAddress>();
		Map<String, Integer> hostIdMap = new HashMap<String, Integer>();
		if(client_id_file.exists()) {
			BufferedReader c_br = new BufferedReader(new FileReader(client_id_file));
			String _client = "";
			while((_client = c_br.readLine()) != null) {
				String inet[] = _client.split(" ");				
				InetAddress addr = InetAddress.getByName(inet[0]);
				Integer port = Integer.parseInt(inet[1]);
				Integer id = Integer.parseInt(inet[2]);
				
				InetSocketAddress client_addr = new InetSocketAddress(addr, port);
				if(client_addr.getHostName().equals(InetAddress.getLocalHost().getHostName())) {
					Client.id = id;
					Client.ip = addr;
					Client.port = port;
				}else {
					client_map.put(id, client_addr);
					hostIdMap.put(client_addr.getHostName(), id);
				}
			}
			Client.otherClients = client_map;
			Client.hostIdMap = hostIdMap;
			c_br.close();
			
		} else {
			System.out.println("No client id file found");
			System.exit(1);
		}
		

		String client_quorum_filename = "client_quorum.list";
		File client_quorum_file = new File(client_quorum_filename);
		
		if(client_quorum_file.exists()) {
			BufferedReader cq_br = new BufferedReader(new FileReader(client_quorum_file));
			String _client_quorum = "";
			while((_client_quorum = cq_br.readLine()) != null) {
				String ids[] = _client_quorum.split(" ");
				if(Client.id == Integer.parseInt(ids[0])) {
					String quorum_client_ids[] = ids[1].split(",");
					for(String quorum_client_id: quorum_client_ids) {
						if(Integer.parseInt(quorum_client_id) != Client.id) {
							InetSocketAddress addr = client_map.get(Integer.parseInt(quorum_client_id));
							if(Client.quorum == null) {
								Client.quorum = new HashMap<InetSocketAddress, SocketMap>();
								Client.quorum.put(addr, null);
							} else {
								Client.quorum.put(addr, null);
							}
						}
					}
				}
			}
			cq_br.close();
		}
		
			
		try {

			Socket socketToServer = new Socket(server, server_port);
			
			OutputStream out = socketToServer.getOutputStream();
			ObjectOutputStream o_out = new ObjectOutputStream(out);
			InputStream in = socketToServer.getInputStream();
			ObjectInputStream o_in = new ObjectInputStream(in);		
			
			//Create client
			SocketMap serverSocketMap = new SocketMap(socketToServer, o_out, o_in);
			Client.serverSocketMap = serverSocketMap;
			
			//Default run
			if (args.length == 0) {
				Scanner scan = new Scanner(System.in);
				System.out.println("Select Operation to perform");

				int count = 1;
				for(OperationMethod method: OperationMethod.values()) {
					System.out.println(count+") "+method.toString());
					count++;
				}

				boolean close = false;
				while (true) {

					String input = scan.nextLine();

					String arg[] = input.split(" ");
					if(arg.length < 1) {
						System.out.println("Please write valid command or argument");
						continue;
					}
					Operations operation = new Operations();
					try {
						OperationMethod.valueOf(arg[0]);
					} catch (Exception e) {
						System.out.println("ERROR: Select only from listed operations");
						continue;
					}
					switch (OperationMethod.valueOf(arg[0])) {
					case CREATE:
						operation.setOperation(OperationMethod.CREATE);
						break;
					case SEEK:
						operation.setOperation(OperationMethod.SEEK);
						break;
					case READ:
						operation.setOperation(OperationMethod.READ);
						break;
					case WRITE:
						operation.setOperation(OperationMethod.WRITE);
						break;
					case DELETE:
						operation.setOperation(OperationMethod.DELETE);
						break;
					case TERMINATE:
						System.out.println("Good Bye!");
						close = true;
						break;
					}

					if(close) {
						break;
					}
					Resource resource = new Resource();
					resource.setFilename(arg[1]);
					operation.setInputResource(resource);

					if(arg.length > 2) {
						operation.setArg(arg[2]);
					}
					Client client = new Client();
					operation.setType(OperationType.PERFORM);				
					Message m = client.request(operation);
					System.out.println(m.getServerId()+" :: "+m.messsage);			
				}
				scan.close();
			} else {
				Client client = new Client();
				client.execute(args[0]);
				ServerSocket clientServerSocket = new ServerSocket(Client.port);
				while(true) {
					try {
						Socket socket = clientServerSocket.accept();
						exec.submit(new Client(socket));
					} catch(Exception e) {
						break;
					}
				}
				clientServerSocket.close();
			}
			socketToServer.close();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
		}

	}
}
