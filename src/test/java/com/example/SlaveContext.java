package com.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.example.components.TestItemWriter;

/**
 * 
 * Starts an application context in a new thread
 * 
 * @author gawain
 *
 */
public class SlaveContext extends AbstractStartable<Integer> {
	
	private static final Log logger = LogFactory.getLog(SlaveContext.class);
	private final String CONTEXT_PATH;
	
	SlaveContext(final String contextPath) {
		this.CONTEXT_PATH = contextPath; 
	}
	
	
	
	@Override
	public Integer call() throws Exception {
		try {
			applicationContext = new ClassPathXmlApplicationContext(CONTEXT_PATH);
		} catch (Exception e) {
			logger.error("error initializing slave context with path " + CONTEXT_PATH, e);
			throw new RuntimeException(e);
		}
		
		return 0;
	}
	
	public int writtenCount() {
		TestItemWriter writer = (TestItemWriter) applicationContext.getBean("writer");
		return writer.getChunkCount();
	}

}