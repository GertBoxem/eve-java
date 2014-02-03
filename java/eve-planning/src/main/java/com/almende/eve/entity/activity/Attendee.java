package com.almende.eve.entity.activity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Attendee implements Serializable, Cloneable {
	private String			displayName		= null;
	private String			email			= null;
	private String			agent			= null; // eve agent url
	private Boolean			optional		= null; // if false, attendee must
													// attend
	private RESPONSE_STATUS	responseStatus	= null;
	
	public Attendee() {
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(final String email) {
		this.email = email;
	}
	
	public String getAgent() {
		return agent;
	}
	
	public void setAgent(final String agent) {
		this.agent = agent;
	}
	
	public Boolean getOptional() {
		return optional;
	}
	
	public void setOptional(final Boolean optional) {
		this.optional = optional;
	}
	
	public void setResponseStatus(final RESPONSE_STATUS responseStatus) {
		this.responseStatus = responseStatus;
	}
	
	public RESPONSE_STATUS getResponseStatus() {
		return responseStatus;
	}
	
	@Override
	public Attendee clone() {
		final Attendee clone = new Attendee();
		
		clone.displayName = displayName;
		clone.email = email;
		clone.agent = agent;
		clone.optional = optional;
		clone.responseStatus = responseStatus;
		
		return clone;
	}
	
	public enum RESPONSE_STATUS {
		needsAction, declined, tentative, accepted
	};
	
}
