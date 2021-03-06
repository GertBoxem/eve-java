/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.state;

import com.almende.eve.capabilities.Config;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class StateConfig.
 */
public class StateConfig extends Config {
	
	/**
	 * Instantiates a new state config.
	 */
	public StateConfig() {
		this(JOM.createObjectNode());
	}
	
	/**
	 * Instantiates a new state config.
	 * 
	 * @param node
	 *            the node
	 */
	public StateConfig(final ObjectNode node) {
		super(node);
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
	
}
