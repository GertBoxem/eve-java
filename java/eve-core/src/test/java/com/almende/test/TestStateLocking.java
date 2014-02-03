/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.test;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.state.ConcurrentSerializableFileState;
import com.almende.eve.state.FileStateFactory;
import com.almende.eve.state.State;
import com.almende.eve.state.TypedKey;

/**
 * The Class TestStateLocking.
 */
public class TestStateLocking extends TestCase {
	// TODO: prove that a collision occurs, possibly by measuring the starttime
	// and runtime of each run.
	// TODO: alternatively: implement a non-locking, non-thread-safe version of
	// the state and see it break:)
	private static final Logger	LOG	= Logger.getLogger(TestStateLocking.class
											.getName());
	
	/**
	 * Test run.
	 * 
	 * @param state
	 *            the state
	 */
	private void testRun(final State state) {
		
		// state.clear();
		state.put("test", "test");
		state.put("test2", "test2");
		
		final ScheduledExecutorService scheduler = Executors
				.newScheduledThreadPool(10);
		final ScheduledFuture<?> thread1 = scheduler.scheduleAtFixedRate(
				new Runnable() {
					@Override
					public void run() {
						state.put("test", "test1");
						state.put("test1", "test");
						state.put("test", "test1");
						state.get("test", String.class);
						state.put("test1", "test");
						state.get("test1", String.class);
					}
					
				}, 0, 100, TimeUnit.MILLISECONDS);
		final ScheduledFuture<?> thread2 = scheduler.scheduleWithFixedDelay(
				new Runnable() {
					@Override
					public void run() {
						state.put("test", "test2");
						state.put("test", "test2");
						state.get("test", String.class);
						state.put("test", "test2");
						state.put("test1", "test");
						state.put("test1", "test");
						state.get("test1", String.class);
					}
					
				}, 110, 95, TimeUnit.MILLISECONDS);
		final ScheduledFuture<?> thread3 = scheduler.scheduleWithFixedDelay(
				new Runnable() {
					@Override
					public void run() {
						state.put("test", "test3");
						state.put("test", "test3");
						state.get("test", String.class);
						state.put("test1", "test");
						state.put("test", "test3");
						state.put("test1", "test");
						state.get("test1", String.class);
					}
					
				}, 105, 97, TimeUnit.MILLISECONDS);
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				thread1.cancel(false);
				thread2.cancel(false);
				thread3.cancel(false);
			}
		}, 1450, TimeUnit.MILLISECONDS);
		final long start = System.currentTimeMillis();
		try {
			Thread.sleep(1500);
		} catch (final InterruptedException e) {
			System.out.println("Sleep interrupted after:"
					+ (System.currentTimeMillis() - start) + " ms.");
		}
		assertEquals("test", state.get(new TypedKey<String>("test1") {
		}));
		assertEquals("test2", state.get("test2", String.class));
		assertTrue(state.get("test", String.class).startsWith("test"));
		
		LOG.info("Done test!");
		
	}
	
	/**
	 * Test file state.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testFileState() throws Exception {
		final File dir = new File(".eveagents_testStates");
		if ((!dir.exists() && !dir.mkdir()) || !dir.isDirectory()) {
			fail("Couldn't create .eveagents_testStates folder");
		}
		final State fc = new ConcurrentSerializableFileState("test",
				".eveagents_testStates/FileStateRun");
		testRun(fc);
	}
	
	/**
	 * Test concurrent file state.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testConcurrentFileState() throws Exception {
		final File dir = new File(".eveagents_testStates");
		if ((!dir.exists() && !dir.mkdir()) || !dir.isDirectory()) {
			fail("Couldn't create .eveagents_testStates folder");
		}
		final FileStateFactory sf = new FileStateFactory(".eveagents_testStates");
		final String agentId = "ConcurrentFileStateRun";
		if (sf.exists(agentId)) {
			sf.delete(agentId);
		}
		final State fc = sf.create(agentId);
		testRun(fc);
	}
	
	/**
	 * Test concurrent json file state.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testConcurrentJsonFileState() throws Exception {
		final File dir = new File(".eveagents_testStates");
		if ((!dir.exists() && !dir.mkdir()) || !dir.isDirectory()) {
			fail("Couldn't create .eveagents_testStates folder");
		}
		final FileStateFactory sf = new FileStateFactory(".eveagents_testStates", true);
		final String agentId = "ConcurrentJsonFileStateRun";
		if (sf.exists(agentId)) {
			sf.delete(agentId);
		}
		final State fc = sf.create(agentId);
		testRun(fc);
	}
	
}
