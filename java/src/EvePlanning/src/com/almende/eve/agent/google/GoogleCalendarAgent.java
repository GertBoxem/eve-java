/**
 * @file GoogleCalendarAgent.java
 * 
 * @brief 
 * TODO: brief
 *
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Copyright © 2012 Almende B.V.
 *
 * @author 	Jos de Jong, <jos@almende.org>
 * @date	2012-05-29
 */


/**
 * 
 * DOCUMENTATION:
 *   https://developers.google.com/google-apps/calendar/v3/reference/
 *   https://developers.google.com/google-apps/calendar/v3/reference/events#resource
 */

package com.almende.eve.agent.google;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.CalendarAgent;
import com.almende.eve.context.Context;
import com.almende.eve.entity.calendar.Authorization;

import com.almende.eve.json.JSONRPCException;
import com.almende.eve.json.JSONRPCException.CODE;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.almende.eve.json.jackson.JOM;
import com.almende.eve.config.Config;
import com.almende.util.HttpUtil;
import com.almende.util.IntervalsUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class GoogleCalendarAgent extends Agent implements CalendarAgent {
	private Logger logger = Logger.getLogger(this.getClass().getName());

	// note: config parameters google.client_id and google.client_secret
	//       are loaded from the eve configuration
	private String OAUTH_URI = "https://accounts.google.com/o/oauth2";
	private String CALENDAR_URI = "https://www.googleapis.com/calendar/v3/calendars/";
	
	/**
	 * Set access token and refresh token, used to authorize the calendar agent. 
	 * These tokens must be retrieved via Oauth 2.0 authorization.
	 * @param access_token
	 * @param token_type
	 * @param expires_in
	 * @param refresh_token
	 * @throws IOException 
	 */
	public void setAuthorization (
			@Name("access_token") String access_token,
			@Name("token_type") String token_type,
			@Name("expires_in") Integer expires_in,
			@Name("refresh_token") String refresh_token) throws IOException {
		logger.info("setAuthorization");
		
		Context context = getContext();
		
		// retrieve user information
		String url = "https://www.googleapis.com/oauth2/v1/userinfo";
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", token_type + " " + access_token);
		String resp = HttpUtil.get(url, headers);
		
		ObjectNode info = JOM.getInstance().readValue(resp, ObjectNode.class);
		String email = info.has("email") ? info.get("email").asText() : null;
		String name = info.has("name") ? info.get("name").asText() : null;
		
		DateTime expires_at = calculateExpiresAt(expires_in);
		Authorization auth = new Authorization(access_token, token_type, 
				expires_at, refresh_token);
		
		// store the tokens in the context
		context.put("auth", auth);
		context.put("email", email);
		context.put("name", name);		
	}
	
	/**
	 * Calculate the expiration time from a life time
	 * @param expires_in      Expiration time in seconds
	 * @return
	 */
	private DateTime calculateExpiresAt(Integer expires_in) {
		DateTime expires_at = null;
		if (expires_in != null && expires_in != 0) {
			// calculate expiration time, and subtract 5 minutes for safety
			expires_at = DateTime.now().plusSeconds(expires_in).minusMinutes(5);
		}
		return expires_at;
	}
		
	/**
	 * Refresh the access token using the refresh token
	 * the tokens in provided authorization object will be updated
	 * @param auth
	 * @throws Exception 
	 */
	private void refreshAuthorization (Authorization auth) throws Exception {
		String refresh_token = (auth != null) ? auth.getRefreshToken() : null;
		if (refresh_token == null) {
			throw new Exception("No refresh token available");
		}
		
		Config config = getContext().getConfig();
		String client_id = config.get("google", "client_id");
		String client_secret = config.get("google", "client_secret");
		
		// retrieve new access_token using the refresh_token
		Map<String, String> params = new HashMap<String, String>();
		params.put("client_id", client_id);
		params.put("client_secret", client_secret);
		params.put("refresh_token", refresh_token);
		params.put("grant_type", "refresh_token");
		String resp = HttpUtil.postForm(OAUTH_URI + "/token", params);
		ObjectNode json = JOM.getInstance().readValue(resp, ObjectNode.class);
		if (!json.has("access_token")) {
			// TODO: give more specific error message
			throw new Exception("Retrieving new access token failed");
		}
		
		// update authorization
		if (json.has("access_token")) {
			auth.setAccessToken(json.get("access_token").asText());
		}
		if (json.has("expires_in")) {
			Integer expires_in = json.get("expires_in").asInt();
			DateTime expires_at = calculateExpiresAt(expires_in);
			auth.setExpiresAt(expires_at);
		}
	}
	
	/**
	 * Remove all stored data from this agent
	 */
	public void clear() {
		Context context = getContext();
		context.remove("auth");
		context.remove("email");
		context.remove("name");			
	}
	
	/**
	 * Get the username associated with the calendar
	 * @return name
	 */
	@Override
	public String getUsername() {
		return (String) getContext().get("name");
	}
	
	/**
	 * Get the email associated with the calendar
	 * @return email
	 */
	@Override
	public String getEmail() {
		return (String) getContext().get("email");
	}
	
	/**
	 * Get ready-made HTTP request headers containing the authorization token
	 * Example usage: HttpUtil.get(url, getAuthorizationHeaders());
	 * @return
	 * @throws Exception 
	 */
	private Map<String, String> getAuthorizationHeaders () throws Exception {
		Authorization auth = getAuthorization();
		
		String access_token = (auth != null) ? auth.getAccessToken() : null;
		if (access_token == null) {
			throw new Exception("No authorization token available");
		}
		String token_type = (auth != null) ? auth.getTokenType() : null;
		if (token_type == null) {
			throw new Exception("No token type available");
		}
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Authorization", token_type + " " + access_token);
		return headers;
	}
	
	/**
	 * Retrieve authorization tokens
	 * @return
	 * @throws Exception
	 */
	private Authorization getAuthorization() throws Exception {
		Authorization auth = (Authorization) getContext().get("auth");

		// check if access_token is expired
		DateTime expires_at = (auth != null) ? auth.getExpiresAt() : null;
		if (expires_at != null && expires_at.isBeforeNow()) {
			// TODO: remove this logging
			logger.info("access token is expired. refreshing now...");
			refreshAuthorization(auth);
			getContext().put("auth", auth);
		}
		
		return auth;
	}
	
	@Override
	public String getVersion() {
		return "0.4";
	}
	
	@Override
	public String getDescription() {
		return "This agent gives access to a Google Calendar. " +
				"It allows to search events, find free timeslots, " +
				"and add, edit, or remove events.";
	}

	/**
	 * Convert the event from a Eve event to a Google event 
	 * @param event
	 */
	private void toGoogleEvent(ObjectNode event) {
		if (event.has("agent")) {
			// move agent url from event.agent to extendedProperties
			String agent = event.get("agent").asText();
			event.with("extendedProperties").with("shared").put("agent", agent);
		}
	}

	/**
	 * Convert the event from a Google event to a Eve event 
	 * @param event
	 */
	private void toEveEvent(ObjectNode event) {
		ObjectNode extendedProperties = (ObjectNode) event.get("extendedProperties");
		if (extendedProperties != null) {
			ObjectNode shared = (ObjectNode) extendedProperties.get("shared");
			if (shared != null && shared.has("agent")) {
				// move agent url from extended properties to event.agent
				String agent = shared.get("agent").asText();
				event.put("agent", agent);
				
				/* TODO: remove agent from extended properties
				shared.remove("agent");
				if (shared.size() == 0) {
					extendedProperties.remove("shared");
					if (extendedProperties.size() == 0) {
						event.remove("extendedProperties");
					}
				}
				*/
			}
		}
	}
	
	/**
	 * Retrieve a list with all calendars in this google calendar
	 */
	@Override
	public ArrayNode getCalendarList() throws Exception {
		logger.info("getCalendarList");
		
		String url = CALENDAR_URI + "users/me/calendarList";
		String resp = HttpUtil.get(url, getAuthorizationHeaders());
		ObjectNode calendars = JOM.getInstance().readValue(resp, ObjectNode.class);

		// check for errors
		if (calendars.has("error")) {
			ObjectNode error = (ObjectNode)calendars.get("error");
			throw new JSONRPCException(error);
		}

		// get items from response
		ArrayNode items = null;
		if (calendars.has("items")) {
			items = (ArrayNode)calendars.get("items");
		}
		else {
			items = JOM.createArrayNode();
		}
		
		return items;
	}

	/**
	 * Get todays events. A convenience method for easy testing
	 * @param calendarId
	 * @return
	 * @throws Exception
	 */
	public ArrayNode getEventsToday(
			@Required(false) @Name("calendarId") String calendarId) throws Exception {
		DateTime now = DateTime.now();
		DateTime timeMin = now.minusMillis(now.getMillisOfDay());
		DateTime timeMax = timeMin.plusDays(1);

		logger.info("getEventsToday timeMin=" + timeMin + ", end=" + timeMax + 
				", calendarId=" + calendarId);

		return getEvents(timeMin.toString(), timeMax.toString(), calendarId);
	}
	
	/**
	 * Get all events in given interval
	 * @param timeMin 		start of the interval
	 * @param timeMax 		end of the interval
	 * @param calendarId   	optional calendar id. If not provided, the default
	 *                      calendar is used
	 */
	@Override
	public ArrayNode getEvents(
			@Required(false) @Name("timeMin") String timeMin, 
			@Required(false) @Name("timeMax") String timeMax, 
			@Required(false) @Name("calendarId") String calendarId) 
			throws Exception {
		logger.info("getEvents timeMin=" + timeMin + ", timeMax=" + timeMax + 
				", calendarId=" + calendarId);
		
		// initialize optional parameters
		if (calendarId == null) {
			calendarId = (String) getContext().get("email");
		}
		
		// built url with query parameters
		String url = CALENDAR_URI + calendarId + "/events";
		Map<String, String> params = new HashMap<String, String>();
		if (timeMin != null) {
			params.put("timeMin", new DateTime(timeMin).toString());
		}
		if (timeMax != null) {
			params.put("timeMax", new DateTime(timeMax).toString());
		}
		// Set singleEvents=true to expand recurring events into instances
		params.put("singleEvents", "true");
		url = HttpUtil.appendQueryParams(url, params);
		
		// perform GET request
		Map<String, String> headers = getAuthorizationHeaders();
		String resp = HttpUtil.get(url, headers);
		ObjectMapper mapper = JOM.getInstance();
		ObjectNode json = mapper.readValue(resp, ObjectNode.class);
		
		// check for errors
		if (json.has("error")) {
			ObjectNode error = (ObjectNode)json.get("error");
			throw new JSONRPCException(error);
		}

		// get items from the response
		ArrayNode items = null;
		if (json.has("items")){
			items = (ArrayNode) json.get("items");
			
			// convert from Google to Eve event
			for (int i = 0; i < items.size(); i++) {
				ObjectNode item = (ObjectNode) items.get(i);
				toEveEvent(item);
			}
		}
		else {
			items = JOM.createArrayNode();
		}
		
		return items;
	}

	/**
	 * Get busy intervals of today. A convenience method for easy testing
	 * @param calendarId
	 * @return
	 * @throws Exception
	 */
	public ArrayNode getBusyToday(
			@Required(false) @Name("calendarId") String calendarId,
			@Required(false) @Name("excludeEventIds") Set<String> excludeEventIds) 
			throws Exception {
		DateTime now = DateTime.now();
		DateTime timeMin = now.minusMillis(now.getMillisOfDay());
		DateTime timeMax = timeMin.plusDays(1);

		return getBusy(timeMin.toString(), timeMax.toString(), calendarId, 
				excludeEventIds);
	}
	
	/**
	 * Retrieve the busy intervals in the calendar
	 * @throws Exception 
	 */
	@Override
	public ArrayNode getBusy(
			@Name("timeMin") String timeMin, 
			@Name("timeMax") String timeMax,
			@Required(false) @Name("calendarId") String calendarId,
			@Required(false) @Name("excludeEventIds") Set<String> excludeEventIds) 
			throws Exception {
		
		ArrayNode events = getEvents(timeMin, timeMax, calendarId);
		
		List<Interval> busy = new ArrayList<Interval>();
        for (int i = 0; i < events.size(); i++) {
        	ObjectNode event = (ObjectNode) events.get(i);
        	
        	// filter excludes
        	String eventId = event.has("id") ? event.get("id").asText() : null;
        	boolean exclude = (eventId != null && excludeEventIds != null &&
        			excludeEventIds.contains(eventId));
        	if (!exclude) {
	        	// TODO: deal with day-events too!!
	        	// TODO: check for null
	        	DateTime start = new DateTime(event.with("start").get("dateTime").asText());
	        	DateTime end = new DateTime(event.with("end").get("dateTime").asText());
	        	Interval interval = new Interval(start, end);
	        	busy.add(interval);
        	}
        }

        // merge the intervals
        List<Interval> merged = IntervalsUtil.merge(busy);
        
        // convert to JSON array
        ArrayNode array = JOM.createArrayNode();
        for (Interval interval : merged) {
        	ObjectNode obj = JOM.createObjectNode();
        	obj.put("start", interval.getStart().toString());
        	obj.put("end", interval.getEnd().toString());
        	array.add(obj);
        }
        return array;
	}

	@Override
	public ObjectNode getEvent (
			@Name("eventId") String eventId,
			@Required(false) @Name("calendarId") String calendarId) 
			throws Exception {
		// initialize optional parameters
		if (calendarId == null) {
			calendarId = (String) getContext().get("email");
		}

		// built url
		String url = CALENDAR_URI + calendarId + "/events/" + eventId;
		
		// perform GET request
		Map<String, String> headers = getAuthorizationHeaders();
		String resp = HttpUtil.get(url, headers);
		ObjectMapper mapper = JOM.getInstance();
		ObjectNode event = mapper.readValue(resp, ObjectNode.class);
		
		// convert from Google to Eve event
		toEveEvent(event);
		
		// check for errors
		if (event.has("error")) {
			ObjectNode error = (ObjectNode)event.get("error");
			Integer code = error.has("code") ? error.get("code").asInt() : null;
			if (code != null && code.equals(404)) {
				throw new JSONRPCException(CODE.NOT_FOUND);				
			}
			
			throw new JSONRPCException(error);
		}
		
		// check if canceled. If so, return null
		// TODO: be able to retrieve canceled events?
		if (event.has("status") && event.get("status").asText().equals("cancelled")) {
			throw new JSONRPCException(CODE.NOT_FOUND);
		}
		
		return event;
	}

	@Override
	public ObjectNode createEvent (@Name("event") ObjectNode event,
			@Required(false) @Name("calendarId") String calendarId) 
			throws Exception {
		// initialize optional parameters
		if (calendarId == null) {
			calendarId = (String) getContext().get("email");
		}

		// built url
		String url = CALENDAR_URI + calendarId + "/events";
		
		logger.info("createEvent event=" + 
				JOM.getInstance().writeValueAsString(event) +
				", calendarId=" + calendarId);

		// perform POST request
		ObjectMapper mapper = JOM.getInstance();
		String body = mapper.writeValueAsString(event);
		Map<String, String> headers = getAuthorizationHeaders();
		headers.put("Content-Type", "application/json");
		String resp = HttpUtil.post(url, body, headers);
		ObjectNode createdEvent = mapper.readValue(resp, ObjectNode.class);
		
		// convert from Google to Eve event
		toEveEvent(event);
		
		// check for errors
		if (createdEvent.has("error")) {
			ObjectNode error = (ObjectNode)createdEvent.get("error");
			throw new JSONRPCException(error);
		}
		
		logger.info("createdEvent=" + JOM.getInstance().writeValueAsString(createdEvent));

		return createdEvent;
	}

	public ObjectNode createEventQuick (
			@Required(false) @Name("start") String start,
			@Required(false) @Name("end") String end,
			@Required(false) @Name("summary") String summary,
			@Required(false) @Name("location") String location,
			@Required(false) @Name("calendarId") String calendarId) throws Exception {
		ObjectNode event = JOM.createObjectNode();
		
		if (start == null) {
			// set start to current time, rounded to hours
			DateTime startDate = DateTime.now();
			startDate = startDate.plusHours(1);
			startDate = startDate.minusMinutes(startDate.getMinuteOfHour());
			startDate = startDate.minusSeconds(startDate.getSecondOfMinute());
			startDate = startDate.minusMillis(startDate.getMillisOfSecond());
			start = startDate.toString();
		}
		ObjectNode startObj = JOM.createObjectNode();
		startObj.put("dateTime", start);
		event.put("start", startObj);
		if (end == null) {
			// set end to start +1 hour
			DateTime startDate = new DateTime(start);
			DateTime endDate = startDate.plusHours(1);
			end = endDate.toString();
		}
		ObjectNode endObj = JOM.createObjectNode();
		endObj.put("dateTime", end);
		event.put("end", endObj);
		if (summary != null) {
			event.put("summary", summary);
		}
		if (location != null) {
			event.put("location", location);
		}
		
		return createEvent(event, calendarId);
	}
	
	@Override
	public ObjectNode updateEvent (@Name("event") ObjectNode event,
			@Required(false) @Name("calendarId") String calendarId) 
			throws Exception {
		// initialize optional parameters
		if (calendarId == null) {
			calendarId = (String) getContext().get("email");
		}

		// convert from Eve to Google event
		toGoogleEvent(event);

		logger.info("updateEvent event=" + 
				JOM.getInstance().writeValueAsString(event) +
				", calendarId=" + calendarId); // TODO: cleanup

		// read id from event
		String id = event.get("id").asText();
		if (id == null) {
			throw new Exception("Parameter 'id' missing in event");
		}
		
		// built url
		String url = CALENDAR_URI + calendarId + "/events/" + id;
		
		// perform POST request
		ObjectMapper mapper = JOM.getInstance();
		String body = mapper.writeValueAsString(event);
		Map<String, String> headers = getAuthorizationHeaders();
		headers.put("Content-Type", "application/json");
		String resp = HttpUtil.put(url, body, headers);
		ObjectNode updatedEvent = mapper.readValue(resp, ObjectNode.class);
		
		// check for errors
		if (updatedEvent.has("error")) {
			ObjectNode error = (ObjectNode)updatedEvent.get("error");
			throw new JSONRPCException(error);
		}

		// convert from Google to Eve event
		toEveEvent(event);
		
		logger.info("updateEvent=" + JOM.getInstance().writeValueAsString(updatedEvent)); // TODO: cleanup

		return updatedEvent;
	}
	
	@Override
	public void deleteEvent (@Name("eventId") String eventId,
			@Required(false) @Name("calendarId") String calendarId) 
			throws Exception {
		// initialize optional parameters
		if (calendarId == null) {
			calendarId = (String) getContext().get("email");
		}

		logger.info("deleteEvent eventId=" + eventId + ", calendarId=" + calendarId); // TODO: cleanup

		// built url
		String url = CALENDAR_URI + calendarId + "/events/" + eventId;
		
		// perform POST request
		Map<String, String> headers = getAuthorizationHeaders();
		String resp = HttpUtil.delete(url, headers);
		if (!resp.isEmpty()) {
			throw new Exception(resp);
		}
		
		logger.info("event deleted"); // TODO: cleanup
	}
}


