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

			System.out.println("THREAD MAIN - Reply acquire");
			gotallReplies.acquire();
			
			getMutex();
			
			System.out.println("THREAD MAIN - Reply acquire");
			gotallReplies.acquire();
			
			pendingReleaseToReceive = id;
		
			System.out.println("THREAD MAIN - Release acquire");
			gotallReleases.acquire();
			
//			while(pendingRepliesToReceive.size() != 0) {
//			
//				Thread.sleep(2);
//				System.out.println("WAIT for pending replyies");
//			}

			
			long endtime = System.currentTimeMillis();
			
			record.time = endtime-starttime;
			System.out.println("--starting CS--");
			
			request(operation);
			System.out.println("--Exiting CS--");
			
			pendingReleaseToReceive = 0;
			gotFailed = 0;
			sentYield = 0;
			
			sendRelease();
			
			System.out.println("THREAD MAIN - Release release");
			gotallReleases.release();
			
			System.out.println("THREAD MAIN - Reply release");
			gotallReplies.release();
			
			printreport();
			
			inprocess = false;
			curr_req_done = true;
			
		} catch (Exception e) {
			try {
				shutdown();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}
}
