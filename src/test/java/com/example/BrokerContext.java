package com.example;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BrokerContext extends AbstractStartable<Integer> {
	
	private static final Log logger = LogFactory.getLog(BrokerContext.class);
	private final String CONTEXT_PATH;
	
	public BrokerContext(String contextPath) {
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
	
}