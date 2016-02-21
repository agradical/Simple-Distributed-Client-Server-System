package utd.aos.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
	
	public static final int NTHREADS = 20;
    public static final ExecutorService exec = Executors.newFixedThreadPool(NTHREADS);

	public static void main(String[] args) {
		String filename = "server.configuration";
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
			
			Server myServer = new ServerCore();
						
			for(InetSocketAddress addr: server_ips) {
				if(addr.getHostName().equals(InetAddress.getLocalHost().getHostName())) {
					myServer.setServer(addr.getAddress(), addr.getPort(), null);
					server_ips.remove(addr);
				}
			}		

			exec.submit(myServer);	
			
		} catch(FileNotFoundException f) {
			System.out.print(f.getMessage());
			return;
		} catch(IOException i) {
			System.out.print(i.getMessage());
			return;
		}
	}
	
}
