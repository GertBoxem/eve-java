package com.almende.eve.entity.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class Time implements Serializable, Cloneable {
	public Time() {
	}
	
	public Long getDuration() {
		return duration;
	}
	
	public void setDuration(final Long duration) {
		this.duration = duration;
	}
	
	public Long getDurationMin() {
		return durationMin;
	}
	
	public void setDurationMin(final Long durationMin) {
		this.durationMin = durationMin;
	}
	
	public Long getDurationMax() {
		return durationMax;
	}
	
	public void setDurationMax(final Long durationMax) {
		this.durationMax = durationMax;
	}
	
	public String getPeriodStart() {
		return periodStart;
	}
	
	public void setPeriodStart(final String periodStart) {
		this.periodStart = periodStart;
	}
	
	public String getPeriodEnd() {
		return periodEnd;
	}
	
	public void setPeriodEnd(final String periodEnd) {
		this.periodEnd = periodEnd;
	}
	
	public List<Preference> withPreferences() {
		if (preferences == null) {
			preferences = new ArrayList<Preference>();
		}
		return preferences;
	}
	
	public List<Preference> getPreferences() {
		return preferences;
	}
	
	public void setPreferences(final List<Preference> preferences) {
		this.preferences = preferences;
	}
	
	public void addPreference(final Preference preference) {
		final List<Preference> preferences = withPreferences();
		preferences.add(preference);
	}
	
	public void merge(final Time other) {
		if (other.duration != null) {
			duration = other.duration;
		}
		if (other.durationMin != null) {
			durationMin = other.durationMin;
		}
		if (other.durationMax != null) {
			durationMax = other.durationMax;
		}
		if (other.periodStart != null) {
			periodStart = other.periodStart;
		}
		if (other.periodEnd != null) {
			periodEnd = other.periodEnd;
		}
		
		if (other.preferences != null) {
			preferences = new ArrayList<Preference>();
			for (final Preference preference : other.preferences) {
				preferences.add(preference != null ? preference.clone() : null);
			}
		}
	}
	
	@Override
	public Time clone() {
		final Time clone = new Time();
		
		clone.duration = duration;
		clone.durationMin = durationMin;
		clone.durationMax = durationMax;
		clone.periodStart = periodStart;
		clone.periodEnd = periodEnd;
		
		if (preferences != null) {
			clone.preferences = new ArrayList<Preference>();
			for (final Preference preference : preferences) {
				clone.preferences.add(preference != null ? preference.clone()
						: null);
			}
		}
		
		return clone;
	}
	
	private Long				duration	= null; // milliseconds
	private Long				durationMin	= null; // milliseconds
	private Long				durationMax	= null; // milliseconds
	private String				periodStart	= null;
	private String				periodEnd	= null;
	private List<Preference>	preferences	= null;
}
