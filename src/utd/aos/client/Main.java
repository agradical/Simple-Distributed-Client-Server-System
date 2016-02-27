package utd.aos.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import utd.aos.utils.Operations;
import utd.aos.utils.Operations.OperationMethod;
import utd.aos.utils.Operations.OperationType;

public class Main {
	public static void main (String[] args) throws Exception {
		
		String filename = "server.list";
		File file = new File(filename);
		
		String server = "";
		
		if(file.exists()) {
			BufferedReader br = new BufferedReader(new FileReader(file));
			List<String> serverlist = new ArrayList<String>();
			String _servers = "";
			while((_servers = br.readLine()) != null) {
				String _server = _servers.split(" ")[0];
				serverlist.add(_server);
			}
			br.close();
			Random rand = new Random();
			Integer id = rand.nextInt(serverlist.size());
			
			server = serverlist.get(id);
			if(server.equals("")) {
				throw new Exception("No Host found");
			}
		}	
		
		try {

			Socket socket = new Socket(server, 1717);
			Client client = new Client();
			while (true) {
				System.out.println("Select Operation to perform");

				int count = 1;
				for(OperationMethod method: OperationMethod.values()) {
					System.out.println(count+") "+method.toString());
					count++;
				}

				Scanner scan = new Scanner(System.in);
				String input = scan.nextLine();
				
				String arg[] = input.split(" ");
				
				Operations operation = new Operations();
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
				}
				
				if (operation.getOperation() == null) {
					System.out.println("Please select the valid operation");
					continue;
				}
				
				operation.setFilename(arg[1]);
				
				if(arg.length > 2) {
					operation.setArg(arg[2]);
				}
				
				operation.setType(OperationType.PERFORM);
				
				client.request(socket, operation);
				
				scan.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
		}

	}
}
