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
			
			while(pendingReleaseToReceive != 0 ) {
			
				Thread.sleep(2);

			}

			pendingReleaseToReceive = id;

			long endtime = System.currentTimeMillis();
			
			record.time = endtime-starttime;
			System.out.println("--starting CS--");
			request(operation);
			System.out.println("--Exiting CS--");
			
			pendingReleaseToReceive = 0;
			gotFailed = 0;
			sentYield = 0;
			
			sendRelease();

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
