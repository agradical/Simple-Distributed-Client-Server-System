package utd.aos.utils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
	public String filename;
	public String arg;
	
	public byte[] fileContent;
	
	//Perform the operation
	public boolean perform(String DATADIRECTORY) {
		
		if(this.operation.equals(OperationMethod.CREATE)) {
			if(this.getFileContent() != null) {
				File dir = new File(DATADIRECTORY);
				if(!dir.exists()) {
					dir.mkdirs();
				}
				
				File file =  new File(DATADIRECTORY, "."+filename+".tmp");
				try {
					file.createNewFile();
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(this.getFileContent());
					fos.close();
				} catch (IOException e) {

				}

			}
		}
		
		//Create a tmp file to perform opertion.. in order to rollback if needed
		if(!this.operation.equals(OperationMethod.READ)) {
			
		}
		return true;
	}

	//Commit and send signal to commit operation.
	public boolean commit(String DATADIRECTORY) {
		File tmp_file =  new File(DATADIRECTORY, "."+filename+".tmp");
		if(tmp_file.exists()) {
			File file = new File(DATADIRECTORY, filename);
			if(file.exists()) {
				file.delete();
			}
			tmp_file.renameTo(file);
		}
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
