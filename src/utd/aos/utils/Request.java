package utd.aos.utils;

import utd.aos.utils.MutexMessage.MessageType;

public class Request {
	public int id;
	public MessageType type;
	public SocketMap socketmap;
	
	public boolean mark;
	
	public Request(int id, SocketMap socketmap, MessageType type) {
		this.id = id;
		this.socketmap = socketmap;
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

	public SocketMap getSocketmap() {
		return socketmap;
	}

	public void setSocketmap(SocketMap socketmap) {
		this.socketmap = socketmap;
	}

	public boolean isMark() {
		return mark;
	}

	public void setMark(boolean mark) {
		this.mark = mark;
	}
}
