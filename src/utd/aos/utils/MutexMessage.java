package utd.aos.utils;

import java.io.Serializable;

//To be used for communication for mutual exclusion between clients
public class MutexMessage implements Serializable {
	
	private static final long serialVersionUID = 1234141L;
	
	private int id;
	private MessageType type;
	public enum MessageType {
		TEST, REQUEST, REPLY, RELEASE, FAILED, ENQUIRE, YIELD, GRANT
	}
	
	public MutexMessage() {
		
	}
	
	public MutexMessage(int id, MessageType type) {
		this.id = id;
		this.type = type;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public MessageType getType() {
		return type;
	}
	public void setType(MessageType type) {
		this.type = type;
	}
}
