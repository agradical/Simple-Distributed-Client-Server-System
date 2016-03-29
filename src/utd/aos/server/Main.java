package utd.aos.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
	
	//Executor service to handle multiple clients
	public static final int NTHREADS = 20;
    public static final ExecutorService exec = Executors.newFixedThreadPool(NTHREADS);
  
	public static void main(String[] args) {
		//List of server with their Hostname and port
		String filename = "server.list";
		if(args.length != 0) {		
			filename = args[0];
		}		
		
		List<InetSocketAddress> server_ips = new ArrayList<InetSocketAddress>();
		
		try {
			File file = new File(filename);		
			BufferedReader br = new BufferedReader(new FileReader(file));
			String conf = "";
			while((conf = br.readLine()) != null) {
				String inet[] = conf.split(" ");				
				InetAddress addr = InetAddress.getByName(inet[0]);
				Integer port = Integer.parseInt(inet[1]);
				InetSocketAddress socket = new InetSocketAddress(addr, port);
				server_ips.add(socket);
			}
			br.close();
			
			//Server myServer = new ServerCore();
			InetSocketAddress myinet = null;
			
			//Comparing the Hostname with localhost name
			//Add the other server information.
			for(InetSocketAddress addr: server_ips) {
				if(addr.getHostName().equals(InetAddress.getLocalHost().getHostName())) {
					ServerCore.ip = addr.getAddress();
					ServerCore.port = addr.getPort();
					myinet = addr;
				} else {
					if(ServerCore.otherServers == null) {
						ServerCore.otherServers = new HashMap<InetAddress, Integer>();
					}
					ServerCore.otherServers.put(addr.getAddress(), addr.getPort());
				}
			}
			
			//server.configuration file to mention the data directory and 
			//other configuration parameters for server
			String conf_filename = "server.configuration";
			File conf_file = new File(conf_filename);	
			BufferedReader conf_br = new BufferedReader(new FileReader(conf_file));
			String data_conf = "";
			while((data_conf = conf_br.readLine()) != null) {
				String inet[] = data_conf.split(" ");				
				InetAddress addr = InetAddress.getByName(inet[0]);
				String data_directory = inet[1];
				if(ServerCore.ip.equals(addr)) {
					ServerCore.DATADIRECTORY = data_directory;
				}
			}
			conf_br.close();
			
			if (myinet == null) {
				System.out.println("This is not listed server");
				return;
			}
			
			ServerSocket serverSocket = new ServerSocket(ServerCore.port);
			while(true) {
				try {
				Socket socket = serverSocket.accept();
				exec.submit(new ServerCore(socket));
				} catch(Exception e) {
					break;
				}
			}
			serverSocket.close();
			
		} catch(FileNotFoundException f) {
			System.out.print(f.getMessage());
			return;
		} catch(IOException i) {
			System.out.print(i.getMessage());
			return;
		}
	}
	
}
