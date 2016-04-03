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
				System.out.println("--WAIT allreply sema ( Main thread)--");
				
				gotallReplies.acquire();
				pendingReleaseToReceive = id;
				
				System.out.println("--starting CS--");
				request(operation);
				System.out.println("--Exiting CS--");

				System.out.println("--RELEASE allreply sema (Main thread)--");
				
				gotallReplies.release();
				pendingReleaseToReceive = 0;
				gotallReleases.release();
				
				sendRelease();
			}
		} catch (Exception e) {

		}
	}
}
