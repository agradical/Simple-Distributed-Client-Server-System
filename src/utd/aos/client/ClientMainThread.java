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
			
			
			long starttime = System.currentTimeMillis();

			getMutex();
			//System.out.println("--WAIT allreply sema ( Main thread)--");			
			//gotallReplies.acquire();

			//System.out.println("--WAIT release sema ( Main thread)--");			
			//gotallReleases.acquire();

			while(pendingRepliesToReceive.size() != 0 || pendingReleaseToReceive != 0) {
				/*
					if(pendingReleaseToReceive != 0)
						System.out.println("---WAITING for all RELEASE");
					else
						System.out.println("---WAITING for all REPLIES");
				 */
				Thread.sleep(2);

			}

			pendingReleaseToReceive = id;

			long endtime = System.currentTimeMillis();
			
			record.time = endtime-starttime;
			System.out.println("--starting CS--");
			request(operation);
			System.out.println("--Exiting CS--");

			//System.out.println("--RELEASE release sema (Main thread)--");
			//gotallReleases.release();

			//System.out.println("--RELEASE allreply sema (Main thread)--");
			//gotallReplies.release();
			pendingReleaseToReceive = 0;
			gotFailed = 0;
			sentYield = 0;
			
			sendRelease();
			//request_fifo.remove();

			inprocess = false;
			
		} catch (Exception e) {
			try {
				shutdown();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}
}
