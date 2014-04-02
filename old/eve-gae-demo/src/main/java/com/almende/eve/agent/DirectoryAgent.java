package com.almende.eve.agent;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.almende.eve.entity.Registration;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Optional;
import com.almende.util.TwigUtil;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.FindCommand.RootFindCommand;
import com.google.code.twig.annotation.AnnotationObjectDatastore;


@Access(AccessType.PUBLIC)
public class DirectoryAgent extends Agent {
	@Override
	public void onInit() {
		TwigUtil.register(Registration.class);
	}
	
	/**
	 * Add an agent to the registered agents
	 * @param agent    Url of the agent
	 * @param type     Class name of the agent (for example "GoogleCalendarAgent")
	 * @param username
	 * @param email
	 * @return
	 * @throws Exception
	 */
	public Registration register(
			@Name("agent") String agent, 
			@Name("type") @Optional String type, 
			@Name("username") @Optional String username, 
			@Name("email") @Optional String email) throws Exception {
		// remove any existing registration
		unregister(agent, email);
		
		// create a new registration
		Registration registration = new Registration();
		registration.setDirectoryAgent(getFirstUrl("http").toASCIIString());
		registration.setAgent(agent);
		registration.setType(type);
		registration.setUsername(username);
		registration.setEmail(email);
		
		// store the registration
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		datastore.store(registration);
		
		// load the registration again, to ensure its indexes are updated
		// TODO: does this actually work?...
		datastore.refresh(registration);
		
		return registration;
	}

	/**
	 * Remove an agent from the registered agents
	 * an agent can be removed by url or by email or both
	 * @param agent    Url of the agent. Optional
	 * @param email    Email of the agent. Optional
	 * @throws Exception
	 */
	public void unregister(
			@Name("agent") @Optional String agent, 
			@Name("email") @Optional String email) throws Exception {
		// remove registrations with this url
		if (agent != null) {
			remove("agent", agent);
		}

		// remove registrations with this email
		if (email != null) {
			remove("email", email);
		}
	}
	
	/**
	 * Find registrations
	 * @param agent    Url of the agent
	 * @param type     Class name of the agent (for example "GoogleCalendarAgent")
	 * @param username
	 * @param email
	 * @return messages
	 * @throws Exception
	 */
	public List<Registration> find (
			@Name("agent") @Optional String agent, 
			@Name("type") @Optional String type, 
			@Name("username") @Optional String username, 
			@Name("email") @Optional String email) 
			throws Exception {
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		
		RootFindCommand<Registration> command = datastore.find()
			.type(Registration.class)
			.addFilter("directoryAgent", FilterOperator.EQUAL, getFirstUrl("http"));
		if (agent != null) {
			command = command.addFilter("agent", FilterOperator.EQUAL, agent);
		}
		if (type != null) {
			command = command.addFilter("type", FilterOperator.EQUAL, type);
		}
		if (username != null) {
			command = command.addFilter("username", FilterOperator.EQUAL, username);
		}
		if (email != null) {
			command = command.addFilter("email", FilterOperator.EQUAL, email);
		}

		QueryResultIterator<Registration> it = command.now();
		List<Registration> registrations = new ArrayList<Registration>();
		while (it.hasNext()) {
			registrations.add(it.next());
		}		
		return registrations;
	}

	/**
	 * Delete all registrations filtered by given field and value.
	 * For example field="email", value="jos@almende.org", all registrations
	 * with this email will be deleted
	 * @param field
	 * @param value
	 * @throws Exception 
	 */
	private void remove (String field, String value) throws Exception {
		if (field == null || value == null) {
			return;
		}		
		
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		
		QueryResultIterator<Registration> it = datastore.find()
		.type(Registration.class)
		.addFilter("directoryAgent", FilterOperator.EQUAL, getFirstUrl("http"))
		.addFilter(field, FilterOperator.EQUAL, value)
		.now();
	
		while (it.hasNext()) {
			Registration registration = it.next();
			datastore.delete(registration);
		}	
	}
	
	/**
	 * Remove all registrations stored by this DirectoryAgent
	 */
	@Override
	public void onDelete () {
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		QueryResultIterator<Registration> it = datastore.find()
			.type(Registration.class)
			.addFilter("directoryAgent", FilterOperator.EQUAL, getFirstUrl("http"))
			.now();
		
		while (it.hasNext()) {
			Registration registration = it.next();
			datastore.delete(registration);
			// TODO: bulk delete all registrations instead of one by one
		}
		
		super.onDelete();
	}
	
	/*
	 * Get the first url filtered by a specific protocol
	 * @param protocol    For example "http"
	 * @return url        Returns url or null if not found
	 */
	protected URI getFirstUrl(String protocol) {
		final List<String> urls = getUrls();
		
		for (String url : urls) {
			if (url.startsWith(protocol + ":")) {
				return URI.create(url);
			}
		}
		
		return null;
	}
	
	@Override
	public String getDescription() {
		return "DirectoryAgent stores a list with registered agents.";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}
}
