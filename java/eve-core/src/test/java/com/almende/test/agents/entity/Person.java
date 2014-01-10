package com.almende.test.agents.entity;

import java.util.ArrayList;
import java.util.List;

public class Person {
	private String			name;
	private String			firstName;
	private String			lastName;
	private List<Double>	marks	= new ArrayList<Double>();
	
	public void setName(final String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}
	
	public String getFirstName() {
		return firstName;
	}
	
	public void setMarks(final List<Double> marks) {
		this.marks = marks;
	}
	
	public List<Double> getMarks() {
		return marks;
	}
}
