package utd.aos.utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;

public class Operations implements Serializable{

	private static final long serialVersionUID = 1L;
	
	public static enum OperationMethod {
		CREATE, SEEK, READ, WRITE, DELETE, TERMINATE
	}
	
	public static enum OperationType {
		PERFORM, COMMIT
	}
	
	public OperationType type;
	public OperationMethod operation;

	public String arg;
	
	public Resource resource;
	
	//Perform the operation
	public Message perform(String DATADIRECTORY, Resource resource) {
		
		Message m = new Message();
		this.setResource(resource);	
		
		if(this.operation.equals(OperationMethod.CREATE)) {
			if(resource.getFileContent() != null) {
				File dir = new File(DATADIRECTORY);
				if(!dir.exists()) {
					dir.mkdirs();
				}
				
				File file =  new File(DATADIRECTORY, "."+resource.getFilename()+".tmp");
				try {
					file.createNewFile();
					FileWriter fw = new FileWriter(file);
					fw.write(resource.getFileContent());
					fw.close();
					m.setStatusCode(200);
					m.setMesssage("File creation successful");
				} catch (IOException e) {
					m.setStatusCode(100);
					m.setMesssage("File creation failed");
				}
			}
		}
		
		else if(this.operation.equals(OperationMethod.READ)) {
			File file = new File(DATADIRECTORY, resource.getFilename());
			try {
				int count = Integer.parseInt(this.getArg());
				char[] result = new char[count];
				FileInputStream fis = new FileInputStream(file);
				InputStreamReader isr = new InputStreamReader(fis);
				
				isr.read(result, resource.getSeek(), count);
				
				m.setStatusCode(200);
				m.setMesssage(result.toString());
				isr.close();
				
			} catch (FileNotFoundException f) {
				m.setStatusCode(100);
				m.setMesssage("File not Found");
			} catch (IOException i) {
				m.setStatusCode(100);
				m.setMesssage("File not Found");
			}
		}
		
		else if(this.operation.equals(OperationMethod.WRITE)) {
			
		}
		
		else if(this.operation.equals(OperationMethod.DELETE)) {
			File file = new File(DATADIRECTORY, resource.getFilename() );
			if (file.exists()){
				m.setStatusCode(200);
				m.setMesssage("File deletion successful");
			}
		}
		
		return m;
	}

	//Commit and send signal to commit operation.
	public Message commit(String DATADIRECTORY) {
		
		Message m = new Message();
		
		if(this.operation.equals(OperationMethod.DELETE)) {
			File file = new File(DATADIRECTORY, this.resource.getFilename());
			if(file.exists()) {
				file.delete();
				m.setStatusCode(200);
				m.setMesssage("File Deletion Operation Committed successfully");
			} else {
				m.setStatusCode(100);
				m.setMesssage("File doesnot exists");
			}
		}
		else {
			File tmp_file =  new File(DATADIRECTORY, "."+this.resource.getFilename()+".tmp");
			if(tmp_file.exists()) {
				File file = new File(DATADIRECTORY, this.resource.getFilename());
				if(file.exists()) {
					file.delete();
				}
				tmp_file.renameTo(file);
			}
		}
			
		return m;
	}

	public OperationMethod getOperation() {
		return operation;
	}
	public void setOperation(OperationMethod operation) {
		this.operation = operation;
	}
	
	public String getArg() {
		return arg;
	}
	public void setArg(String arg) {
		this.arg = arg;
	}

	public OperationType getType() {
		return type;
	}

	public void setType(OperationType type) {
		this.type = type;
	}

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}
}
