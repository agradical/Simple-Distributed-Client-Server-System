package utd.aos.utils;

import java.util.Comparator;

public class RequestComparator implements Comparator<Request> {

	@Override
	public int compare(Request r1, Request r2) {
		if (r1.getId() > r2.getId()) {
			return 1; 
		} else if (r1.getId() < r2.getId()) {
			return -1;
		} else {
			return 0;
		}
	}
}