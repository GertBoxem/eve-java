/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.agent;

import java.net.URI;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.transport.http.HttpTransportConfig;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class TestRpc.
 */
public class TestProxy extends TestCase {
	private static final Logger	LOG	= Logger.getLogger(TestProxy.class
											.getName());
	
	/**
	 * Test me.
	 */
	@Test
	public void testProxy() {
		final HttpTransportConfig transportConfig = new HttpTransportConfig();
		transportConfig.setServletUrl("http://localhost:8081/agents/");
		
		transportConfig.setServletLauncher("JettyLauncher");
		final ObjectNode jettyParms = JOM.createObjectNode();
		jettyParms.put("port", 8081);
		transportConfig.put("jetty", jettyParms);
		
		final AgentConfig config = new AgentConfig("example");
		config.setTransport(transportConfig);
		
		final ExampleAgent agent = new ExampleAgent();
		agent.setConfig(config);
		
		final ExampleAgentInterface proxy = AgentProxyFactory.genProxy(agent,
				URI.create("http://localhost:8081/agents/example"),
				ExampleAgentInterface.class);
		LOG.warning("Proxy got reply:" + proxy.helloWorld("Hi there"));
	}
}
