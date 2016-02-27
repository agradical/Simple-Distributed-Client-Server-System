package utd.aos.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import utd.aos.utils.Message;
import utd.aos.utils.Operations;
import utd.aos.utils.Operations.OperationMethod;

public class Client {
	public Message request(Socket socket, Operations operation) throws IOException, ClassNotFoundException {
		
		OutputStream out = socket.getOutputStream();
		ObjectOutputStream o_out = new ObjectOutputStream(out);	
		
		if(operation.getOperation().equals(OperationMethod.CREATE)) {
			File file = new File(operation.getFilename());
			if(file.exists()) {
				FileInputStream fis = new FileInputStream(file);
				byte[] fileContent = new byte[(int)file.length()];
				fis.read(fileContent);
				operation.setFileContent(fileContent);
				fis.close();
			}
		}
		
		o_out.writeObject(operation);
		
		//wait for their status
		InputStream in = socket.getInputStream();
		ObjectInputStream o_in = new ObjectInputStream(in);
		Object object = o_in.readObject();
		
		
		Message m;
		
		if (object instanceof Message) {
			 m = (Message)object;
			 return m;
		} else {
			return null;
		}
	}
}
