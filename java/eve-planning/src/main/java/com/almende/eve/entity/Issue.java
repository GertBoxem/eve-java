package com.almende.eve.entity;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class Issue implements Serializable {
	public Issue() {
	}
	
	public void setCode(final Integer code) {
		this.code = code;
	}
	
	public Integer getCode() {
		return code;
	}
	
	public void setType(final TYPE type) {
		this.type = type;
	}
	
	public TYPE getType() {
		return type;
	}
	
	public void setMessage(final String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setTimestamp(final String timestamp) {
		this.timestamp = timestamp;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public void setHints(final List<Hint> hints) {
		this.hints = hints;
	}
	
	public List<Hint> getHints() {
		return hints;
	}
	
	public boolean hasHints() {
		return (hints != null && hints.size() > 0);
	}
	
	public static enum TYPE {
		error, warning, weakWarning, info
	};
	
	// error codes
	// TODO: better implement error codes
	public static Integer	NO_PLANNING			= 1000;
	public static Integer	EXCEPTION			= 2000;
	public static Integer	JSONRPCEXCEPTION	= 2001;
	
	private Integer			code				= null;
	private TYPE			type				= null;
	private String			message				= null;
	private String			timestamp			= null;
	private List<Hint>		hints				= null;
}
