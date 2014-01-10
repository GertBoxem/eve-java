/**
 * @file Agent.java
 * 
 * @brief
 *        Agent is the abstract base class for all Eve agents.
 *        It provides basic functionality such as id, url, getting methods,
 *        subscribing to events, etc.
 * 
 * @license
 *          Licensed under the Apache License, Version 2.0 (the "License"); you
 *          may not
 *          use this file except in compliance with the License. You may obtain
 *          a copy
 *          of the License at
 * 
 *          http://www.apache.org/licenses/LICENSE-2.0
 * 
 *          Unless required by applicable law or agreed to in writing, software
 *          distributed under the License is distributed on an "AS IS" BASIS,
 *          WITHOUT
 *          WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 *          the
 *          License for the specific language governing permissions and
 *          limitations under
 *          the License.
 * 
 *          Copyright © 2010-2012 Almende B.V.
 * 
 * @author Jos de Jong, <jos@almende.org>
 * @date 2012-12-12
 */

package com.almende.eve.agent;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.annotation.Namespace;
import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.agent.callback.CallbackInterface;
import com.almende.eve.agent.callback.SyncCallback;
import com.almende.eve.event.EventsInterface;
import com.almende.eve.monitor.ResultMonitorFactoryInterface;
import com.almende.eve.rpc.RequestParams;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Sender;
import com.almende.eve.rpc.jsonrpc.JSONMessage;
import com.almende.eve.rpc.jsonrpc.JSONRPC;
import com.almende.eve.rpc.jsonrpc.JSONRPCException;
import com.almende.eve.rpc.jsonrpc.JSONRPCException.CODE;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.JSONResponse;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.eve.scheduler.Scheduler;
import com.almende.eve.state.State;
import com.almende.eve.state.TypedKey;
import com.almende.eve.transport.TransportService;
import com.almende.util.AnnotationUtil;
import com.almende.util.TypeUtil;
import com.almende.util.AnnotationUtil.AnnotatedClass;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Access(AccessType.UNAVAILABLE)
public abstract class Agent implements AgentInterface {
	private static final Logger				LOG					= Logger.getLogger(Agent.class
																		.getCanonicalName());
	private AgentHost						host				= null;
	private State							state				= null;
	private Scheduler						scheduler			= null;
	private ResultMonitorFactoryInterface	monitorFactory		= null;
	private EventsInterface					eventsFactory		= null;
	private CallbackInterface<JSONResponse>	callbacks			= null;
	private static final RequestParams		EVEREQUESTPARAMS	= new RequestParams();
	static {
		EVEREQUESTPARAMS.put(Sender.class, null);
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public String getDescription() {
		return "Base agent.";
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public String getVersion() {
		return "1.0";
	}
	
	public Agent() {
	}
	
	public void constr(final AgentHost agentHost, final State state) {
		if (this.state == null) {
			host = agentHost;
			this.state = state;
			monitorFactory = agentHost.getResultMonitorFactory(this);
			eventsFactory = agentHost.getEventsFactory(this);
			callbacks = agentHost.getCallbackService(getId(),
					JSONResponse.class);
			
			// validate the Eve agent and output as warnings
			final List<String> errors = JSONRPC.validate(this.getClass(),
					EVEREQUESTPARAMS);
			for (final String error : errors) {
				LOG.warning("Validation error class: "
						+ this.getClass().getName() + ", message: " + error);
			}
		}
	}
	
	@Override
	public boolean hasPrivate() {
		try {
			final Class<?> clazz = this.getClass();
			final AnnotatedClass annotated = AnnotationUtil.get(clazz);
			for (final Annotation anno : annotated.getAnnotations()) {
				if (anno.annotationType().equals(Access.class)
						&& ((Access) anno).value() == AccessType.PRIVATE) {
					return true;
				}
				if (anno.annotationType().equals(Sender.class)) {
					return true;
				}
			}
		} catch (final Exception e) {
			LOG.log(Level.WARNING,
					" Couldn't determine private annotations of agent "
							+ getId(), e);
		}
		return false;
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public boolean onAccess(final String senderUrl, final String functionTag) {
		return true;
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public boolean onAccess(final String senderUrl) {
		return onAccess(senderUrl, null);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public boolean isSelf(final String senderUrl) {
		if (senderUrl.startsWith("web://")) {
			return true;
		}
		final List<String> urls = getUrls();
		return urls.contains(senderUrl);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	// TODO: Replace this by some form of publish/subscribe model!
	public void signalAgent(final AgentSignal<?> event) {
		if (AgentSignal.INVOKE.equals(event.getEvent())) {
			sigInvoke((Object[]) event.getData());
		} else if (AgentSignal.RESPOND.equals(event.getEvent())) {
			sigRespond((JSONResponse) event.getData());
		} else if (AgentSignal.RESPONSE.equals(event.getEvent())) {
			sigResponse((JSONResponse) event.getData());
		} else if (AgentSignal.SEND.equals(event.getEvent())) {
			sigSend((JSONMessage) event.getData());
		} else if (AgentSignal.EXCEPTION.equals(event.getEvent())) {
			sigException((JSONResponse) event.getData());
		} else if (AgentSignal.CREATE.equals(event.getEvent())) {
			sigCreate();
		} else if (AgentSignal.INIT.equals(event.getEvent())) {
			sigInit();
		} else if (AgentSignal.DELETE.equals(event.getEvent())) {
			sigDelete();
		} else if (AgentSignal.DESTROY.equals(event.getEvent())) {
			sigDestroy();
		} else if (AgentSignal.SETSCHEDULERFACTORY.equals(event.getEvent())) {
			// init scheduler tasks
			scheduler = host.getScheduler(this);
		} else if (AgentSignal.ADDTRANSPORTSERVICE.equals(event.getEvent())) {
			final TransportService service = (TransportService) event.getData();
			try {
				service.reconnect(getId());
			} catch (final IOException e) {
				LOG.log(Level.WARNING,
						"Failed to reconnect agent on new transport.", e);
			}
		}
	}
	
	/**
	 * This method is called once in the life time of an agent, at the moment
	 * the agent is being created by the AgentHost.
	 * It can be overridden and used to perform some action when the agent
	 * is create, in that case super.sigCreate() should be called in
	 * the overridden sigCreate().
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigCreate() {
		for (final TransportService service : host.getTransportServices()) {
			try {
				service.reconnect(getId());
			} catch (final Exception e) {
				LOG.log(Level.WARNING, "Couldn't reconnect transport:"
						+ service + " for agent:" + getId(), e);
			}
		}
	}
	
	/**
	 * This method is called on each incoming RPC call.
	 * It can be overridden and used to perform some action when the agent
	 * is invoked.
	 * 
	 * @param request
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigInvoke(final Object[] signalData) {
	}
	
	/**
	 * This method is called after handling each incoming RPC call.
	 * It can be overridden and used to perform some action when the agent
	 * has been invoked.
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigRespond(final JSONResponse response) {
	}
	
	/**
	 * This method is called when handling any outgoing RPC messages.
	 * It can be overridden and used to perform some action when the agent
	 * is sending a message.
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigSend(final JSONMessage message) {
	}
	
	/**
	 * This method is called when an agent encounters any exception handling RPC
	 * messages.
	 * It can be overridden and used to perform some action when the agent
	 * encounters an exception.
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigException(final JSONResponse response) {
	}
	
	/**
	 * This method is called when handling an incoming RPC response.
	 * It can be overridden and used to perform some action when the agent
	 * has received a response.
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigResponse(final JSONResponse response) {
	}
	
	/**
	 * This method is called directly after the agent and its state is
	 * initiated.
	 * It can be overridden and used to perform some action when the agent
	 * is initialized, in that case super.sigInit() should be called in
	 * the overridden sigInit().
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigInit() {
	}
	
	/**
	 * This method is called by the finalize method (GC) upon unloading of the
	 * agent from memory.
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigDestroy() {
	}
	
	/**
	 * This method is called once in the life time of an agent, at the moment
	 * the agent is being deleted by the AgentHost.
	 * It can be overridden and used to perform some action when the agent
	 * is deleted, in that case super.sigDelete() should be called in
	 * the overridden sigDelete().
	 */
	@Access(AccessType.UNAVAILABLE)
	protected void sigDelete() {
		// TODO: unsubscribe from all subscriptions
		
		// cancel all scheduled tasks.
		if (scheduler == null) {
			scheduler = host.getScheduler(this);
		}
		if (scheduler != null) {
			for (final String taskId : scheduler.getTasks()) {
				scheduler.cancelTask(taskId);
			}
		}
		// remove all keys from the state
		// Note: the state itself will be deleted by the AgentHost
		state.clear();
		
		// save the agents class again in the state
		state.put(State.KEY_AGENT_TYPE, getClass().getName());
		state = null;
		// forget local reference, as it can keep the State alive
		// even if the AgentHost removes the file.
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	protected void finalize() throws Throwable {
		// ensure the state is cleanup when the agent's method destroy is not
		// called.
		sigDestroy();
		getState().destroy();
		super.finalize();
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final State getState() {
		return state;
	}
	
	@Override
	@Namespace("scheduler")
	public final Scheduler getScheduler() {
		if (scheduler == null) {
			scheduler = host.getScheduler(this);
		}
		return scheduler;
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final AgentHost getAgentHost() {
		return host;
		
	}
	
	@Override
	@Namespace("monitor")
	public final ResultMonitorFactoryInterface getResultMonitorFactory() {
		return monitorFactory;
	}
	
	@Override
	@Namespace("event")
	public final EventsInterface getEventsFactory() {
		return eventsFactory;
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public URI getFirstUrl() {
		final List<String> urls = getUrls();
		if (urls.size() > 0) {
			return URI.create(urls.get(0));
		}
		return URI.create("local:" + getId());
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public List<Object> getMethods() {
		return JSONRPC.describe(this, EVEREQUESTPARAMS);
	}
	
	// TODO: only allow ObjectNode as params?
	private JSONResponse locSend(final URI url, final String method, final Object params)
			throws IOException, JSONRPCException {
		
		ObjectNode jsonParams;
		if (params instanceof ObjectNode) {
			jsonParams = (ObjectNode) params;
		} else {
			jsonParams = JOM.getInstance().valueToTree(params);
		}
		
		// invoke the other agent via the AgentHost, allowing the factory
		// to route the request internally or externally
		final JSONRequest request = new JSONRequest(method, jsonParams);
		final SyncCallback<JSONResponse> callback = new SyncCallback<JSONResponse>();
		send(request, url, callback);
		JSONResponse response;
		try {
			response = callback.get();
		} catch (final Exception e) {
			throw new JSONRPCException(CODE.REMOTE_EXCEPTION, "", e);
		}
		final JSONRPCException err = response.getError();
		if (err != null) {
			throw err;
		}
		return response;
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(final URI url, final String method, final Object params, final Class<T> type)
			throws IOException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, params).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(final URI url, final String method, final Object params, final Type type)
			throws IOException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, params).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(final URI url, final String method, final Object params,
			final TypeUtil<T> type) throws IOException, JSONRPCException {
		return type.inject(locSend(url, method, params).getResult());
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(final URI url, final String method, final Object params, final JavaType type)
			throws IOException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, params).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(final URI url, final String method, final Type type)
			throws IOException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, null).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(final URI url, final String method, final JavaType type)
			throws IOException, JSONRPCException {
		return TypeUtil.inject(locSend(url, method, null).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(final URI url, final String method, final Class<T> type)
			throws IOException, JSONRPCException {
		
		return TypeUtil.inject(locSend(url, method, null).getResult(), type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> T send(final URI url, final String method, final TypeUtil<T> type)
			throws IOException, JSONRPCException {
		
		return type.inject(locSend(url, method, null).getResult());
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final void send(final URI url, final String method, final Object params)
			throws IOException, JSONRPCException {
		locSend(url, method, params);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final void send(final URI url, final String method) throws IOException,
			JSONRPCException {
		locSend(url, method, null);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T extends AgentInterface> T createAgentProxy(final URI url,
			final Class<T> agentInterface) {
		return getAgentHost().createAgentProxy(this, url, agentInterface);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T extends AgentInterface> AsyncProxy<T> createAsyncAgentProxy(
			final URI url, final Class<T> agentInterface) {
		return getAgentHost().createAsyncAgentProxy(this, url, agentInterface);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final void sendAsync(final URI url, final String method,
			final ObjectNode params) throws IOException {
		sendAsync(url, method, params, null, Void.class);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final void sendAsync(final URI url, final String method)
			throws IOException {
		sendAsync(url, method, null, null, Void.class);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final String method,
			final ObjectNode params, final AsyncCallback<T> callback,
			final Class<T> type) throws IOException {
		final JSONRequest request = new JSONRequest(method, params);
		sendAsync(url, request, callback, JOM.getTypeFactory()
				.uncheckedSimpleType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final String method,
			final ObjectNode params, final AsyncCallback<T> callback,
			final Type type) throws IOException {
		final JSONRequest request = new JSONRequest(method, params);
		sendAsync(url, request, callback,
				JOM.getTypeFactory().constructType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final String method,
			final ObjectNode params, final AsyncCallback<T> callback,
			final JavaType type) throws IOException {
		final JSONRequest request = new JSONRequest(method, params);
		sendAsync(url, request, callback, type);
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, final Class<T> type)
			throws IOException {
		sendAsync(url, request, callback, JOM.getTypeFactory()
				.uncheckedSimpleType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, final Type type)
			throws IOException {
		sendAsync(url, request, callback,
				JOM.getTypeFactory().constructType(type));
	}
	
	@Override
	@Access(AccessType.UNAVAILABLE)
	public final <T> void sendAsync(final URI url, final JSONRequest request,
			final AsyncCallback<T> callback, final JavaType type)
			throws IOException {
		
		// Create a callback to retrieve a JSONResponse and extract the result
		// or error from this. This is double nested, mostly because of the type
		// conversions required on the result.
		final AsyncCallback<JSONResponse> responseCallback = new AsyncCallback<JSONResponse>() {
			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(final JSONResponse response) {
				if (callback == null) {
					final Exception err = response.getError();
					if (err != null) {
						LOG.warning("async RPC call failed, and no callback handler available:"
								+ err.getLocalizedMessage());
					}
				} else {
					final Exception err = response.getError();
					if (err != null) {
						callback.onFailure(err);
					}
					if (type != null && !type.hasRawClass(Void.class)) {
						try {
							final T res = (T) TypeUtil.inject(response.getResult(),
									type);
							callback.onSuccess(res);
						} catch (final ClassCastException cce) {
							callback.onFailure(new JSONRPCException(
									"Incorrect return type received for JSON-RPC call:"
											+ request.getMethod() + "@" + url,
									cce));
						}
						
					} else {
						callback.onSuccess(null);
					}
				}
			}
			
			@Override
			public void onFailure(final Exception exception) {
				if (callback == null) {
					LOG.warning("async RPC call failed and no callback handler available:"
							+ exception.getLocalizedMessage());
				} else {
					callback.onFailure(exception);
				}
			}
		};
		
		send(request, url, responseCallback);
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public List<String> getUrls() {
		final List<String> urls = new ArrayList<String>();
		if (host != null) {
			final String agentId = getId();
			for (final TransportService service : host.getTransportServices()) {
				final String url = service.getAgentUrl(agentId);
				if (url != null) {
					urls.add(url);
				}
			}
			urls.add("local:" + agentId);
		} else {
			LOG.severe("AgentHost not initialized?!?");
		}
		return urls;
	}
	
	@Override
	public <T> T getRef(final TypedKey<T> key) {
		return host.getRef(getId(), key);
	}
	
	@Override
	public <T> void putRef(final TypedKey<T> key, final T value) {
		host.putRef(getId(), key, value);
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public String getId() {
		return state.getAgentId();
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public String getType() {
		return getClass().getSimpleName();
	}
	
	@Override
	@Access(AccessType.PUBLIC)
	public String toString() {
		final Map<String, Object> data = new HashMap<String, Object>();
		data.put("class", this.getClass().getName());
		data.put("id", getId());
		return data.toString();
	}
	
	// TODO: This should be abstracted to a generic "Translation service"?
	@Override
	public void receive(final Object msg, final URI senderUrl) {
		receive(msg, senderUrl, null);
	}
	
	public static JSONMessage jsonConvert(final Object msg) throws IOException,
			JSONRPCException {
		JSONMessage jsonMsg = null;
		if (msg instanceof JSONMessage) {
			jsonMsg = (JSONMessage) msg;
		} else {
			ObjectNode json = null;
			if (msg instanceof String) {
				final String message = (String) msg;
				if (message.startsWith("{") || message.trim().startsWith("{")) {
					
					json = JOM.getInstance().readValue(message,
							ObjectNode.class);
				}
			} else if (msg instanceof ObjectNode) {
				json = (ObjectNode) msg;
			} else if (msg == null) {
				LOG.warning("Message null!");
			} else {
				LOG.warning("Message unknown type:" + msg.getClass());
			}
			if (json != null) {
				if (JSONRPC.isResponse(json)) {
					final JSONResponse response = new JSONResponse(json);
					jsonMsg = response;
				} else if (JSONRPC.isRequest(json)) {
					final JSONRequest request = new JSONRequest(json);
					jsonMsg = request;
				} else {
					throw new IllegalArgumentException(
							" Request does not contain a valid JSON-RPC request or response");
				}
			}
		}
		return jsonMsg;
	}
	
	@Override
	public void receive(final Object msg, final URI senderUrl, final String tag) {
		JsonNode id = null;
		try {
			final JSONMessage jsonMsg = jsonConvert(msg);
			if (jsonMsg != null) {
				if (jsonMsg.getId() != null) {
					id = jsonMsg.getId();
				}
				if (jsonMsg instanceof JSONRequest) {
					final RequestParams params = new RequestParams();
					params.put(Sender.class, senderUrl.toASCIIString());
					
					final JSONRequest request = (JSONRequest) jsonMsg;
					final AgentInterface me = this;
					host.getPool().execute(new Runnable() {
						@Override
						public void run() {
							final Object[] signalData = new Object[2];
							signalData[0] = request;
							signalData[1] = params;
							signalAgent(new AgentSignal<Object[]>(
									AgentSignal.INVOKE, signalData));
							
							final JSONResponse response = JSONRPC.invoke(me, request,
									params, me);
							
							signalAgent(new AgentSignal<JSONResponse>(
									AgentSignal.RESPOND, response));
							try {
								send(response, senderUrl, null, tag);
							} catch (final IOException e) {
								LOG.log(Level.WARNING, getId()
										+ ": Failed to send response.", e);
							}
						}
					});
					
				} else if (jsonMsg instanceof JSONResponse && callbacks != null
						&& id != null && !id.isNull()) {
					final JSONResponse response = (JSONResponse) jsonMsg;
					final AsyncCallback<JSONResponse> callback = callbacks
							.get(id);
					if (callback != null) {
						host.getPool().execute(new Runnable() {
							@Override
							public void run() {
								signalAgent(new AgentSignal<JSONResponse>(
										AgentSignal.RESPONSE, response));
								if (response.getError() != null) {
									callback.onFailure(response.getError());
								} else {
									callback.onSuccess(response);
								}
							}
						});
					}
				}
			} else {
				LOG.log(Level.WARNING, getId()
						+ ": Received non-JSON message:'" + msg + "'");
			}
		} catch (final Exception e) {
			LOG.log(Level.WARNING, "Exception in receiving message", e);
			// generate JSON error response, skipped if it was an incoming
			// notification i.s.o. request.
			final JSONRPCException jsonError = new JSONRPCException(
					JSONRPCException.CODE.INTERNAL_ERROR, e.getMessage(), e);
			final JSONResponse response = new JSONResponse(jsonError);
			response.setId(id);
			signalAgent(new AgentSignal<JSONResponse>(AgentSignal.EXCEPTION,
					response));
			try {
				send(response, senderUrl, null, tag);
			} catch (final Exception e1) {
				LOG.log(Level.WARNING,
						getId() + ": failed to send '"
								+ e.getLocalizedMessage()
								+ "' error to remote agent.", e1);
			}
		}
	}
	
	@Override
	public void send(final Object msg, final URI receiverUrl,
			final AsyncCallback<JSONResponse> callback) throws IOException {
		send(msg, receiverUrl, callback, null);
	}
	
	@Override
	public void send(final Object msg, final URI receiverUrl,
			final AsyncCallback<JSONResponse> callback, final String tag)
			throws IOException {
		if (msg instanceof JSONMessage) {
			signalAgent(new AgentSignal<JSONMessage>(AgentSignal.SEND,
					(JSONMessage) msg));
			if (callback != null && callbacks != null) {
				callbacks.store(((JSONMessage) msg).getId(), callback);
			}
		}
		// This should already been done!
		if (msg instanceof JSONRPCException) {
			LOG.log(Level.WARNING,
					"Send has been called to send an JSONRPCException i.s.o. a JSONMessage...");
			host.sendAsync(receiverUrl,
					new JSONResponse((JSONRPCException) msg), this, tag);
			return;
		}
		host.sendAsync(receiverUrl, msg, this, tag);
	}
}
