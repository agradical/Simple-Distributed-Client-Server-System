package utd.aos.client;

import utd.aos.utils.Operations;

public class ClientMainThread extends Client {
	
	Operations operation;
	
	public ClientMainThread(Operations operation) {
		this.operation = operation;
	}
	
	@Override
	public void run() {
		try {
			if(getMutex()) {
				gotallReplies.acquire();

				System.out.println("--starting CS--");

				request(operation);

				System.out.println("--Exiting CS--");

				gotallReplies.release();
				gotallReleases.release();
				sendRelease();
			}
		} catch (Exception e) {

		}
	}
}
