package com.example;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Starts an application context in a new thread.
 * 
 * @author gawain
 *
 */
public class MasterBatchContext extends AbstractStartable<BatchStatus> {

	private final Log logger = LogFactory.getLog(MasterBatchContext.class);
	private final String JOB_NAME;
	private final String CONTEXT_PATH;
	private BatchStatus batchStatus;

	
	public MasterBatchContext(final String jobName, final String contextPath) {
		this.JOB_NAME = jobName;
		this.CONTEXT_PATH = contextPath;
	}
	
	
	@Override
	public BatchStatus call() throws Exception {
		try {
			applicationContext = new ClassPathXmlApplicationContext(CONTEXT_PATH);
			final JobLauncher jobLauncher = (JobLauncher) applicationContext.getBean("jobLauncher");
			final JobParameter jobParam = new JobParameter(UUID.randomUUID().toString());
			final Map params = Collections.singletonMap("param1", jobParam);
			final Job job = (Job) applicationContext.getBean(JOB_NAME);
			logger.debug("batch context running job " + job);
			final JobExecution jobExecution = jobLauncher.run(job, new JobParameters(params) );
			batchStatus = jobExecution.getStatus();
		} catch (Exception e) {
			logger.error("Exception thrown in batch context", e);
			throw new RuntimeException(e);
		}
		
		logger.debug("batch context finished running job: " + batchStatus);
		return batchStatus;
	}

	public BatchStatus getBatchStatus() {
		return batchStatus;
	}

}