/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.transport.zmq;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.zeromq.ZMQ.Socket;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.transport.TransportService;
import com.almende.util.tokens.TokenStore;

/**
 * The Class ZmqService.
 */
public class ZmqService implements TransportService {
	private static final Logger				LOG				= Logger.getLogger(ZmqService.class
																	.getCanonicalName());
	private AgentHost						host			= null;
	private String							baseUrl			= "";
	private final Map<String, ZmqConnection>	inboundSockets	= new HashMap<String, ZmqConnection>();
	protected ZmqService() {
	}
	
	/**
	 * Construct an ZmqService
	 * This constructor is called when the TransportService is constructed
	 * by the AgentHost.
	 *
	 * @param agentHost the agent host
	 * @param params Available parameters:
	 * {String} baseUrl
	 * {Integer} basePort
	 */
	public ZmqService(final AgentHost agentHost, final Map<String, Object> params) {
		host = agentHost;
		
		if (params != null) {
			baseUrl = (String) params.get("baseUrl");
			baseUrl = baseUrl.replaceAll("(.*:[0-9]+).*$", "$1");
		}
		
	}
	
	
	/* (non-Javadoc)
	 * @see com.almende.eve.transport.TransportService#getAgentUrl(java.lang.String)
	 */
	@Override
	public URI getAgentUrl(final String agentId) {
		if (inboundSockets.containsKey(agentId)) {
			return inboundSockets.get(agentId).getAgentUrl();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.transport.TransportService#getAgentId(java.lang.String)
	 */
	@Override
	public String getAgentId(final URI agentUrl) {
		for (final Entry<String, ZmqConnection> entry : inboundSockets.entrySet()) {
			if (entry.getValue().getAgentUrl().equals(agentUrl)) {
				return entry.getKey();
			}
		}
		return null;
	}
	
	/**
	 * Send async.
	 *
	 * @param zmqType the zmq type
	 * @param token the token
	 * @param senderUrl the sender url
	 * @param receiverUrl the receiver url
	 * @param message the message
	 * @param tag the tag
	 */
	public void sendAsync(final byte[] zmqType, final String token,
			final URI senderUrl, final URI receiverUrl,
			final byte[] message, final String tag) {
		host.getPool().execute(new Runnable() {
			@Override
			public void run() {
				final String addr = receiverUrl.toString().replaceFirst("zmq:/?/?", "");
				final Socket socket = ZMQ.getSocket(org.zeromq.ZMQ.PUSH);
				try {
					socket.connect(addr);
					socket.send(zmqType, org.zeromq.ZMQ.SNDMORE);
					socket.send(senderUrl.toString(), org.zeromq.ZMQ.SNDMORE);
					socket.send(token, org.zeromq.ZMQ.SNDMORE);
					socket.send(message, 0);
					
				} catch (final Exception e) {
					LOG.log(Level.WARNING, "Failed to send JSON through ZMQ", e);
				}
				socket.setTCPKeepAlive(-1);
				socket.setLinger(-1);
				socket.close();
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.transport.TransportService#sendAsync(java.lang.String, java.lang.String, java.lang.Object, java.lang.String)
	 */
	@Override
	public void sendAsync(final URI senderUrl, final URI receiverUrl,
			final String message, final String tag) {
		sendAsync(ZMQ.NORMAL, TokenStore.create().toString(), senderUrl,
				receiverUrl, message.getBytes(), tag);
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.transport.TransportService#sendAsync(java.lang.String, java.lang.String, java.lang.Object, java.lang.String)
	 */
	@Override
	public void sendAsync(final URI senderUrl, final URI receiverUrl,
			final byte[] message, final String tag) {
		sendAsync(ZMQ.NORMAL, TokenStore.create().toString(), senderUrl,
				receiverUrl, message, tag);
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.transport.TransportService#getProtocols()
	 */
	@Override
	public List<String> getProtocols() {
		return Arrays.asList("zmq");
	}
	
	/**
	 * Gen url.
	 *
	 * @param agentId the agent id
	 * @return the string
	 */
	private URI genUrl(final String agentId) {
		String res = null;
		if (baseUrl.startsWith("tcp://")) {
			final int basePort = Integer.parseInt(baseUrl.replaceAll(".*:", ""));
			// TODO: this is not nice. Agents might change address at server
			// restart.... How to handle this?
			res = baseUrl.replaceFirst(":[0-9]*$", "") + ":"
					+ (basePort + inboundSockets.size());
		} else if (baseUrl.startsWith("inproc://")) {
			res = baseUrl + agentId;
		} else if (baseUrl.startsWith("ipc://")) {
			res = baseUrl + agentId;
		} else {
			throw new IllegalStateException("ZMQ baseUrl not valid! (baseUrl:'"
					+ baseUrl + "')");
		}
		if (res != null){
			try {
				return new URI(res);
			} catch (URISyntaxException e) {
				LOG.warning("Strange, couldn't generate zmq url:"+res);
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.transport.TransportService#reconnect(java.lang.String)
	 */
	@Override
	public synchronized void reconnect(final String agentId) throws IOException {
		try {
			if (inboundSockets.containsKey(agentId)) {
				final ZmqConnection conn = inboundSockets.get(agentId);
				final Socket socket = conn.getSocket();
				socket.disconnect(conn.getZmqUrl().toString());
				socket.bind(conn.getZmqUrl().toString());
				conn.listen();
			} else {
				final ZmqConnection socket = new ZmqConnection(
						ZMQ.getSocket(org.zeromq.ZMQ.PULL), this);
				inboundSockets.put(agentId, socket);
				
				final URI url = genUrl(agentId);
				socket.getSocket().bind(url.toString());
				socket.setAgentUrl(url);
				socket.setAgentId(agentId);
				socket.setHost(host);
				socket.listen();
			}
		} catch (final Exception e) {
			LOG.log(Level.SEVERE, "Caught error:", e);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.transport.TransportService#getKey()
	 */
	@Override
	public String getKey() {
		return "zmq:" + baseUrl;
	}
	
}
