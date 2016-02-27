package utd.aos.utils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

import utd.aos.server.ServerCore;

public class Operations implements Serializable{

	private static final long serialVersionUID = 1L;
	
	public static enum OperationMethod {
		CREATE, SEEK, READ, WRITE, DELETE
	}
	
	public static enum OperationType {
		PERFORM, COMMIT
	}
	
	public OperationType type;
	public OperationMethod operation;
	public String filename;
	public String arg;
	
	public byte[] fileContent;
	
	//Perform the operation
	public boolean perform() {
		
		if(!this.operation.equals(OperationMethod.CREATE)) {
			if(this.getFileContent() != null) {
				File dir = new File(ServerCore.DATA_DIRECTORY);
				if(!dir.exists()) {
					dir.mkdir();
				}
				
				File file =  new File(ServerCore.DATA_DIRECTORY+"/"+filename);
				if(!file.exists()) {
					try {
					file.createNewFile();
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(this.getFileContent());
					fos.close();
					} catch (IOException e) {
						
					}
				} else {
					return false;
				}
			}
		}
		
		//Create a tmp file to perform opertion.. in order to rollback if needed
		if(!this.operation.equals(OperationMethod.READ)) {
			//Create lock
			
			//Delete lock
		}
		return true;
	}

	//Commit and send signal to commit operation.
	public boolean commit() {	
		return true;
	}

	public OperationMethod getOperation() {
		return operation;
	}
	public void setOperation(OperationMethod operation) {
		this.operation = operation;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
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

	public byte[] getFileContent() {
		return fileContent;
	}

	public void setFileContent(byte[] fileContent) {
		this.fileContent = fileContent;
	}
	
}
