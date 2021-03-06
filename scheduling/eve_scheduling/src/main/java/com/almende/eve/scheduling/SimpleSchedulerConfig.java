/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.scheduling;

import com.almende.eve.capabilities.Config;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class SimpleSchedulerConfig.
 */
public class SimpleSchedulerConfig extends Config {

	/**
	 * Instantiates a new simple scheduler config.
	 */
	public SimpleSchedulerConfig() {
		this(JOM.createObjectNode());
	}

	/**
	 * Instantiates a new simple scheduler config.
	 * 
	 * @param node
	 *            the node
	 */
	public SimpleSchedulerConfig(final ObjectNode node) {
		super(node);
		if (node == null || !node.has("class")) {
			setClassName(SimpleSchedulerBuilder.class.getName());
		}
	}

	/**
	 * Sets the id.
	 * 
	 * @param id
	 *            the new id
	 */
	public void setId(final String id) {
		this.put("id", id);
	}

	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public String getId() {
		if (this.has("id")) {
			return this.get("id").asText();
		}
		return null;
	}

	/**
	 * Sets the sender url.
	 * 
	 * @param senderUrl
	 *            the new sender url
	 */
	public void setSenderUrl(String senderUrl) {
		this.put("senderUrl", senderUrl);
	}

	/**
	 * Gets the sender url.
	 * 
	 * @return the sender url
	 */
	public String getSenderUrl() {
		if (this.has("senderUrl")) {
			return this.get("senderUrl").asText();
		}
		return "local:scheduler_"+getId();
	}
}
