package utd.aos.client;

import java.io.IOException;
import java.net.Socket;

import utd.aos.utils.Operations;
import utd.aos.utils.Operations.OperationMethod;
import utd.aos.utils.Operations.OperationType;

public class Main {
	public static void main (String[] args) {
		try {
			Socket socket = new Socket("dc01.utdallas.edu", 1717);
			Client client = new Client();
			Operations operation = new Operations();
			operation.setOperation(OperationMethod.CREATE);
			operation.setType(OperationType.PERFORM);
			
			client.request(socket, operation);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
		}
	}
}
