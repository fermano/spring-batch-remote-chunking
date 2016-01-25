package com.example.components;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;

public class TestItemWriter implements ItemWriter<String> {

	private static final Log logger = LogFactory.getLog(TestItemWriter.class);
	public volatile static int count = 0;
	private int chunkCount = 0;
	private boolean exceptionThrown = false;
	private String exceptionText;
	private String writerName ="unknown-writer";
	
	@Override
	public void write(final List<? extends String> items) throws Exception {
		for (String item : items) {
			count++;
			if (! exceptionThrown && exceptionText != null && item.contains(exceptionText) ) {
				exceptionThrown = true;
				// throw checked exception so master can deal with it
				throw new RuntimeException("A contrived exception thrown on the remote writer to demonstrate error handling over message queues for item " + item);
			}
			logger.info("********** [" + writerName + "] writing item: " + item);
			
		}
		chunkCount++;
	}
	
	public void setExceptionText(final String exceptionText) {
		this.exceptionText = exceptionText;
	}

	public void setWriterName(final String writerName) {
		this.writerName = writerName;
	}

	public int getChunkCount() {
		return chunkCount;
	}
}
