package utd.aos.utils;

import java.io.Serializable;

public class Message implements Serializable {
	
	private static final long serialVersionUID = 1123123L;
	
	public int statusCode;
	public String messsage;	
	public String serverId;
	
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
	public String getServerId() {
		return serverId;
	}
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

}
