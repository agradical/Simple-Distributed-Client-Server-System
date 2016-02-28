package utd.aos.utils;

import java.io.Serializable;

public class Resource implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public String filename;
	public int seek = 0;
	public int writeOffset = 0;
	public String fileContent;
	
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
	public String getFileContent() {
		return fileContent;
	}
	public void setFileContent(String fileContent) {
		this.fileContent = fileContent;
	}
	public int getWriteOffset() {
		return writeOffset;
	}
	public void setWriteOffset(int writeOffset) {
		this.writeOffset = writeOffset;
	}
	
}
