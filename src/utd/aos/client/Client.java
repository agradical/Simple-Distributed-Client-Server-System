package utd.aos.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


import utd.aos.utils.Message;
import utd.aos.utils.Operations;
import utd.aos.utils.Resource;
import utd.aos.utils.Operations.OperationMethod;

public class Client {
	public Message request(Operations operation, ObjectOutputStream o_out, ObjectInputStream o_in) throws IOException, ClassNotFoundException {
		
		if(operation.getOperation().equals(OperationMethod.CREATE)) {
			Resource resource = operation.getResource();
			File file = new File(resource.getFilename());
			if(file.exists()) {
				FileInputStream fis = new FileInputStream(file);
				byte[] fileContent = new byte[(int)file.length()];
				fis.read(fileContent);
						
				resource.setFileContent(fileContent);
				
				operation.setResource(resource);
				fis.close();
			}
		}
		
		o_out.writeObject(operation);
		
		//wait for their status
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
