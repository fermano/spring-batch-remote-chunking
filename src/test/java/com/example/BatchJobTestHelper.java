package com.example;

import org.springframework.batch.core.BatchStatus;

public class BatchJobTestHelper {	
	
	public static void waitForJobTopComplete(MasterBatchContext batchContext) {
		boolean running = true;
		while (running) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException("exception during Thread.sleep()", e);
			}
			System.out.println("Waiting... Batch Job Status:" + batchContext.getBatchStatus() );
			if ( isJobFinished(batchContext) ) {
				running = false;
			}
		}
	}
	

	public static boolean isJobFinished(final MasterBatchContext batchContetxt) {
		final BatchStatus batchStatus = batchContetxt.getBatchStatus();
		if (    batchStatus == BatchStatus.STARTED  || 
				batchStatus == BatchStatus.STARTING || 
				batchStatus == BatchStatus.STOPPING || 
				batchStatus == null) {
			return false;
		} else {
			return true;
		}
	}

}