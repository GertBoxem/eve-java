/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.test;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.state.MemoryStateFactory;
import com.almende.eve.test.agents.Test2Agent;
import com.almende.eve.transport.zmq.ZmqService;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class TestZmq.
 */
public class TestZmq extends TestCase {
	private static final Logger	LOG	= Logger.getLogger(TestZmq.class
											.getCanonicalName());
	
	/**
	 * Gets the url.
	 * 
	 * @param type
	 *            the type
	 * @param agentId
	 *            the agent id
	 * @return the url
	 */
	private URI getUrl(final String type, final String agentId) {
		if ("tcp".equals(type)) {
			final int port = agentId.equals("test") ? 5556 : 5557;
			return URI.create("zmq:tcp://127.0.0.1:" + port);
		} else if ("inproc".equals(type)) {
			return URI.create("zmq:inproc://" + agentId);
		} else if ("ipc".equals(type)) {
			return URI.create("zmq:ipc:///tmp/" + agentId);
		}
		return null;
	}
	
	/**
	 * Run test.
	 * 
	 * @param test
	 *            the test
	 * @param test2
	 *            the test2
	 * @param type
	 *            the type
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws JSONRPCException
	 *             the jSONRPC exception
	 * @throws InterruptedException
	 *             the interrupted exception
	 */
	private void runTest(final Test2Agent test, final Test2Agent test2,
			final String type) throws IOException, JSONRPCException,
			InterruptedException {
		final Set<String> results = new ConcurrentSkipListSet<String>();
		
		final AsyncCallback<String> callback = new AsyncCallback<String>() {
			
			@Override
			public void onSuccess(final String result) {
				results.add(result);
			}
			
			@Override
			public void onFailure(final Exception e) {
				LOG.log(Level.SEVERE, "Oeps, exception:", e);
			}
			
		};
		final URI uri1 = getUrl(type, test.getId());
		final URI uri2 = getUrl(type, test2.getId());
		
		LOG.warning("uri1:" + uri1 + " uri2:" + uri2);
		
		ObjectNode parms = JOM.createObjectNode();
		parms.put("message", "1");
		test.sendAsync(uri2, "slowPing", parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "2");
		test.sendAsync(uri2, "slowPing", parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "3");
		test.sendAsync(uri1, "slowPing", parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "4");
		test.sendAsync(uri1, "slowPing", parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "5");
		test2.sendAsync(uri1, "slowPing", parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "6");
		test2.sendAsync(uri1, "slowPing", parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "7");
		test2.sendAsync(uri2, "slowPing", parms, callback, String.class);
		
		parms = JOM.createObjectNode();
		parms.put("message", "8");
		test2.sendAsync(uri2, "slowPing", parms, callback, String.class);
		Thread.sleep(2000);
		
		System.err.println("results:" + results);
		assertEquals(8, results.size());
		
	}
	
	/**
	 * Test zmq.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testZmq() throws Exception {
		final AgentHost host = AgentHost.getInstance();
		host.setDoesShortcut(false);
		host.setStateFactory(new MemoryStateFactory());
		
		if (host.hasAgent("test")) {
			host.deleteAgent("test");
		}
		if (host.hasAgent("test2")) {
			host.deleteAgent("test2");
		}
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("baseUrl", "tcp://127.0.0.1:5555");
		host.addTransportService(new ZmqService(host, params));
		
		params = new HashMap<String, Object>();
		params.put("baseUrl", "inproc://");
		host.addTransportService(new ZmqService(host, params));
		
		params = new HashMap<String, Object>();
		params.put("baseUrl", "ipc:///tmp/");
		host.addTransportService(new ZmqService(host, params));
		
		final Test2Agent test = host.createAgent(Test2Agent.class, "test");
		final Test2Agent test2 = host.createAgent(Test2Agent.class, "test2");
		
		runTest(test, test2, "inproc");
		runTest(test, test2, "ipc");
		runTest(test, test2, "tcp");
		
	}
	
}
