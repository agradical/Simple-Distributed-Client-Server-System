package utd.aos.utils;

import java.io.Serializable;

public class Message implements Serializable {
	
	private static final long serialVersionUID = 1123123L;
	
	public int statusCode;
	public String status;
	public String messsage;	
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	public String getMesssage() {
		return messsage;
	}
	public void setMesssage(String messsage) {
		this.messsage = messsage;
	}

}
