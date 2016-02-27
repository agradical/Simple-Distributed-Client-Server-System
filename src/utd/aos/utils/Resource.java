package utd.aos.utils;

import java.io.File;

public class Resource {
	
	public File file;
	public String filename;
	public int seek;
	public byte[] fileContent;
	
	
	public File getFile() {
		return file;
	}	
	public void setFile(File file) {
		this.file = file;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public int getSeek() {
		return seek;
	}
	public void setSeek(int seek) {
		this.seek = seek;
	}
	
	public byte[] getFileContent() {
		return fileContent;
	}
	public void setFileContent(byte[] fileContent) {
		this.fileContent = fileContent;
	}
	
}
