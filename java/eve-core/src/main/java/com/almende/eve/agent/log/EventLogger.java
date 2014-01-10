package com.almende.eve.agent.log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.AgentHostDefImpl;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;

public class EventLogger {
	private static final Logger	LOG			= Logger.getLogger(EventLogger.class
													.getCanonicalName());
	private AgentHostDefImpl	agentHost	= null;
	
	protected EventLogger() {
	}
	
	public EventLogger(final AgentHostDefImpl agentHost) {
		this.agentHost = agentHost;
	}
	
	public void log(final String agentId, final String event, final Object params) {
		try {
			final String logAgentId = getLogAgentId(agentId);
			final LogAgent agent = (LogAgent) agentHost.getAgent(logAgentId);
			if (agent != null) {
				// log only if the log agent exists
				agent.log(new Log(agentId, event, params));
			}
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "", e);
		}
	}
	
	public List<Log> getLogs(final String agentId, final Long since)
			throws JSONRPCException, ClassNotFoundException,
			InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, IOException {
		final String logAgentId = getLogAgentId(agentId);
		LogAgent agent = (LogAgent) agentHost.getAgent(logAgentId);
		if (agent == null) {
			// create the log agent if it does not yet exist
			agent = agentHost
					.createAgent(LogAgent.class, logAgentId);
			agent.config(agentHost.getAgent(agentId).getFirstUrl());
		}
		return agent.getLogs(since);
	}
	
	private String getLogAgentId(final String agentId) {
		// TODO: use a naming here which cannot conflict with other agents.
		// introduce a separate namespace or something like that?
		return "_logagent_" + agentId;
	}
	
}
