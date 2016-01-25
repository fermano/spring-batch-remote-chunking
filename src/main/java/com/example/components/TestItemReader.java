package com.example.components;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

public class TestItemReader implements ItemReader<String> {

	private static final Log logger = LogFactory.getLog(TestItemReader.class);
	private final List<String> items;
	public volatile int count = 0;
	
	TestItemReader() {
		items = new ArrayList<String>();
	}
	
	@Override
	public String read() throws Exception, UnexpectedInputException, ParseException,
			NonTransientResourceException {
		
		if (items.size() > count) {
			final String item = items.get(count++);
			logger.info("********** for count " + count + ", reading item: " + item);
			return item;
		}else {
			logger.info("********** returning null for count " + count);
			return null;
		}
		
	}

	public List<String> getItems() {
		return items;
	}

	public void setItems(List<String> items) {
		this.items.clear();
		this.items.addAll(items);
	}
}