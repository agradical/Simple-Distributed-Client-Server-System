package utd.aos.utils;

import java.net.InetSocketAddress;
import java.util.Map;

import utd.aos.client.Client;
import utd.aos.client.Client.State;

public class MaekawaAlgorithm implements MutexAlgorithm {

	Client client;
	Map<InetSocketAddress, SocketMap> quorum;
	Map<String, Integer> hostIdMap;
	
	public MaekawaAlgorithm(Client client, Map<InetSocketAddress, SocketMap> quorum, Map<String, Integer> hostIdMap) {
		this.client = client;
		this.quorum = quorum;
		this.hostIdMap = hostIdMap;
	}
	
	//@Override
	public boolean getMutex() {
		// TODO Auto-generated method stub
		// Send request message and
		return true;
	}

	//@Override
	public MutexMessage handleMessage(MutexMessage message) {
		// TODO Auto-generated method stub
		
		return null;
	}

}
