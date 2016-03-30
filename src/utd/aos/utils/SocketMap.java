package utd.aos.utils;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketMap {
	
	Socket socket;
	InetSocketAddress addr;
	ObjectOutputStream o_out;
	ObjectInputStream o_in;
	
	public SocketMap(Socket socket, ObjectOutputStream o_out, ObjectInputStream o_in) {
		this.socket = socket;
		this.o_in = o_in;
		this.o_out = o_out;
	}

	public SocketMap(Socket socket, ObjectOutputStream o_out, ObjectInputStream o_in, InetSocketAddress addr) {
		this.socket = socket;
		this.o_in = o_in;
		this.o_out = o_out;
		this.addr = addr;
	}
	
	public SocketMap(InetSocketAddress addr) {
		this.addr = addr;
	}
	
	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public ObjectOutputStream getO_out() {
		return o_out;
	}

	public void setO_out(ObjectOutputStream o_out) {
		this.o_out = o_out;
	}

	public ObjectInputStream getO_in() {
		return o_in;
	}

	public void setO_in(ObjectInputStream o_in) {
		this.o_in = o_in;
	}

	public InetSocketAddress getAddr() {
		return addr;
	}

	public void setAddr(InetSocketAddress addr) {
		this.addr = addr;
	}
}