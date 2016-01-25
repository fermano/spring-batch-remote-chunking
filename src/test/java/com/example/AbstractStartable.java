package com.example;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.context.ApplicationContext;

/**
 * super class with common methods for starting a new thread
 * 
 * @author gawain
 *
 * @param <V>
 */
public abstract class AbstractStartable<V> implements Callable<V> {

	Future<V> future;
	private final ExecutorService executorService = Executors.newFixedThreadPool(1);
	protected ApplicationContext applicationContext;
	
	public void shutdown() {
		executorService.shutdown();
		executorService.shutdownNow();
		if (! executorService.isTerminated() && ! executorService.isShutdown() ) {
			throw new RuntimeException("applciation context is not shutdown correctly");
		}
	}
	
	public AbstractStartable start() {
		future = executorService.submit((Callable) this);
		return this;
	}

	public Future<V> getFuture() {
		return future;
	}
	
}