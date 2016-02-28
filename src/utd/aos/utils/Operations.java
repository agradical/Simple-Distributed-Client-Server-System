package utd.aos.utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
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
	
	public Resource inputResource;
	
	//Perform the operation
	public Message perform(String DATADIRECTORY, Resource resource) {
		
		Message m = new Message();
		
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
					m.setMesssage(resource.getFilename()+" created successfully");
				} catch (IOException e) {
					m.setStatusCode(100);
					m.setMesssage("ERROR --"+resource.getFilename()+" file creation failed");
				}
			}
		}
		else if(this.operation.equals(OperationMethod.SEEK)) {
			int seek = Integer.parseInt(this.getArg());
			resource.setSeek(seek);
			resource.setWriteOffset(seek);
			m.setStatusCode(200);
			m.setMesssage("Cursor moved to "+seek+" index in "+resource.getFilename());
		}
		else if(this.operation.equals(OperationMethod.READ)) {
			File file = new File(DATADIRECTORY, resource.getFilename());
			try {
				RandomAccessFile file_r = new RandomAccessFile(file, "r");
				int count = Integer.parseInt(this.getArg());
				int seek = resource.getSeek();
				file_r.seek(seek);
				String output = "";
				while(count > 0) {
					output += (char)file_r.read();
					count--;
				}
				file_r.close();
				m.setStatusCode(200);
				m.setMesssage(output);
			} catch (FileNotFoundException f) {
				m.setStatusCode(100);
				m.setMesssage("ERROR -- "+resource.getFilename()+" does not exist");
			} catch (IOException i) {
				m.setStatusCode(100);
				m.setMesssage("ERROR -- "+resource.getFilename()+" IO Exception");
			}
		}
		
		else if(this.operation.equals(OperationMethod.WRITE)) {
			File originalFile = new File(DATADIRECTORY, resource.getFilename());
			File file =  new File(DATADIRECTORY, "."+resource.getFilename()+".tmp");
			if(!file.exists()) {
				InputStream inStream = null;
				OutputStream outStream = null;
				try {
					file.createNewFile();
					
		    	    inStream = new FileInputStream(originalFile);
		    	    outStream = new FileOutputStream(file);
		        	
		    	    byte[] buffer = new byte[1024];
		    		
		    	    int length;
		    	    //copy the file content in bytes 
		    	    while ((length = inStream.read(buffer)) > 0){    	  
		    	    	outStream.write(buffer, 0, length);
		    	    }
		    	    inStream.close();
		    	    outStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					m.setStatusCode(100);
					m.setMesssage("ERROR writing in file "+resource.getFilename());
					e.printStackTrace();
				}
			}
			
			if(originalFile.exists() && file.exists()) {
				try{
					RandomAccessFile file_r = new RandomAccessFile(file, "rw");
					int seek = resource.getWriteOffset();
					file_r.seek(seek);
					byte[] ip_bytes = this.getArg().getBytes();
					int nextWriteLocation = seek+this.getArg().length();
					
					file_r.write(ip_bytes);
					file_r.close();
					
					resource.setWriteOffset(nextWriteLocation);
					m.setStatusCode(200);
					m.setMesssage("A string is written in "+resource.getFilename());
				} catch (FileNotFoundException e) {
					m.setStatusCode(100);
					m.setMesssage("ERROR -- "+resource.getFilename()+" does not exist");
					e.printStackTrace();
				} catch (IOException i) {
					m.setStatusCode(100);
					m.setMesssage("ERROR writing in file "+resource.getFilename());
					i.printStackTrace();
				}
			}
		}
		
		else if(this.operation.equals(OperationMethod.DELETE)) {
			File file = new File(DATADIRECTORY, resource.getFilename() );
			if (file.exists()) {
				m.setStatusCode(200);
				m.setMesssage(resource.getFilename() + " deleted successfully");
			}
		}
		
		return m;
	}

	//Commit and send signal to commit operation.
	public Message commit(String DATADIRECTORY, Resource resource) {
		
		Message m = new Message();
		
		if(this.operation.equals(OperationMethod.DELETE)) {
			File file = new File(DATADIRECTORY, resource.getFilename());
			if(file.exists()) {
				file.delete();
				m.setStatusCode(200);
				m.setMesssage("File Deletion Operation Committed successfully");
			} else {
				m.setStatusCode(100);
				m.setMesssage("File does not exists");
			}
		}
		else {
			if(this.operation.equals(OperationMethod.SEEK)) {
				m.setStatusCode(200);
				m.setMesssage("Commited successully");
			} else {
				File tmp_file =  new File(DATADIRECTORY, "."+resource.getFilename()+".tmp");
				if(tmp_file.exists()) {
					File file = new File(DATADIRECTORY, resource.getFilename());
					if(file.exists()) {				
						file.delete();		
					}
					tmp_file.renameTo(file);
					m.setStatusCode(200);
					m.setMesssage("Commited successully");
				}
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

	public Resource getInputResource() {
		return this.inputResource;
	}

	public void setInputResource(Resource resource) {
		this.inputResource = resource;
	}
}
