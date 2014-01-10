package com.almende.test;

import java.io.Serializable;

import junit.framework.TestCase;

import org.junit.Test;

import com.almende.eve.state.MemoryState;

public class TestMemoryState extends TestCase {
	
	@Test
	public void testMemoryState() throws Exception {
		final MemoryState fc = new MemoryState();
		
		final MyObject testObject = new MyObject();
		testObject.setaField("bye");
		
		fc.put("test1", testObject);
		
		final MyObject resObject = fc.get("test1", MyObject.class);
		
		assertNotSame(testObject, resObject);
		assertEquals(testObject.getaField(), resObject.getaField());
		
	}
}

class MyObject implements Serializable {
	private static final long	serialVersionUID	= -7643312816937130652L;
	
	private String				aField				= "hi";
	
	public String getaField() {
		return aField;
	}
	
	public void setaField(final String aField) {
		this.aField = aField;
	}
}
