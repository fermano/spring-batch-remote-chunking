package com.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.test.annotation.DirtiesContext;


@DirtiesContext
public class DemoApplicationTests {

	private static final Log logger = LogFactory.getLog(DemoApplicationTests.class);
	
	@Test
	public void testRemoteChunkingOnMultipleSlavesShouldLoadBalanceAndComplete() throws Exception {

//		final BrokerContext broker = new BrokerContext("classpath:/broker/broker-context.xml");
//		broker.call();
		final MasterBatchContext masterBatchContext = new MasterBatchContext
				("testjob", "classpath:/master/master-batch-context.xml");
		final SlaveContext slaveContext1 = new SlaveContext(
				"classpath:/slave/slave1-batch-context.xml");
		final SlaveContext slaveContext2 = new SlaveContext(
				"classpath:/slave/slave2-batch-context.xml");

		masterBatchContext.start();
		slaveContext1.start();
		slaveContext2.start();
		
		BatchJobTestHelper.waitForJobTopComplete(masterBatchContext);

		final BatchStatus batchStatus = masterBatchContext.getBatchStatus();
		logger.info("job finished with status: " + batchStatus);
		Assert.assertEquals("Batch Job Status", BatchStatus.COMPLETED, batchStatus);
		logger.info("slave 1 chunks written: " + slaveContext1.writtenCount() );
		logger.info("slave 2 chunks written: " + slaveContext2.writtenCount() );
		Assert.assertEquals("slave chunks written", 5, slaveContext1.writtenCount() ); 
		Assert.assertEquals("slave chunks written", 5, slaveContext2.writtenCount());

	}	
}