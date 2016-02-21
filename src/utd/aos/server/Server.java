package utd.aos.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public interface Server extends Runnable {
    
	public Server getServer();
	public void setServer(InetAddress ip, Integer port);
	public void addServer(InetAddress server, Integer port);
	
	public void execute(Socket clientSocket) throws IOException, ClassNotFoundException;	
	
}
