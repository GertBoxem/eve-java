/**
 * @file GoogleDirectionsAgent.java
 * 
 * @brief
 *        TODO: brief
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
 *          Copyright © 2010-2011 Almende B.V.
 * 
 * @author Jos de Jong, <jos@almende.org>
 * @date 2011-04-13
 */

package com.almende.eve.agent.google;

import java.io.IOException;
import java.net.URISyntaxException;
// import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.HttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class GoogleDirectionsAgent.
 */
@Access(AccessType.PUBLIC)
public class GoogleDirectionsAgent extends Agent {
	
	/**
	 * The Constant DIRECTIONS_SERVICE_URL.
	 */
	static final String	DIRECTIONS_SERVICE_URL	= "http://maps.googleapis.com/maps/api/directions/json";
	
	// TODO: get https working - requires SSL certificate
	
	// Documentation:
	// http://code.google.com/apis/maps/documentation/directions/
	// http://code.google.com/apis/maps/documentation/webservices/index.html
	// http://code.google.com/apis/loader/signup.html
	// http://code.google.com/p/gwt-google-apis/source/browse/trunk/maps/samples/hellomaps/src/com/google/gwt/maps/sample/hellomaps/client/SimpleDirectionsDemo.java?r=1875
	//
	// key for
	// http://agentplatform.appspot.com/
	// on account
	// wjosdejong@gmail.com
	// is:
	// ABQIAAAAQOJzPEiBDTDlB2oHxRVmTxRSrjmNg-hdT5E1_a3uQ7J2AKkR7hTFenoJvK-F_h8dho7B4VXJZx1pdg
	//
	// TODO: if signing url is not needed, remove classes Base64 and UrlSigner
	// again
	
	// private static String keyString =
	// "ABQIAAAAQOJzPEiBDTDlB2oHxRVmTxRSrjmNg-hdT5E1_a3uQ7J2AKkR7hTFenoJvK-F_h8dho7B4VXJZx1pdg";
	
	/**
	 * Gets the directions.
	 * 
	 * @param origin
	 *            the origin
	 * @param destination
	 *            the destination
	 * @return the directions
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws InvalidKeyException
	 *             the invalid key exception
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws URISyntaxException
	 *             the uRI syntax exception
	 */
	public ObjectNode getDirections(@Name("origin") final String origin,
			@Name("destination") final String destination) throws IOException,
			InvalidKeyException, NoSuchAlgorithmException, URISyntaxException {
		
		// TODO: use java API instead of URL fetch? -> I get OVER_QUERY_LIMIT
		// issues
		// when deployed.
		final String url = DIRECTIONS_SERVICE_URL + "?origin="
				+ URLEncoder.encode(origin, "UTF-8") + "&destination="
				+ URLEncoder.encode(destination, "UTF-8")
				// + "&mode=driving" // driving, walking, or bicycling
				// + "&language=nl" // nl, en, ...
				+ "&sensor=false"
		// + "&key=" + keyString // TODO: check if adding this key solves the
		// issue...
		;
		
		// * Does not work when deployed on google app engine, we need to sign
		// with key
		final String response = HttpUtil.get(url);
		// */
		
		/*
		 * TODO: use url signing
		 * // Convert the string to a URL so we can parse it
		 * System.out.println("key: " + keyString);
		 * 
		 * URL u = new URL(url);
		 * String clientID = ...
		 * UrlSigner signer = new UrlSigner(cientId);
		 * String request = u.getProtocol() + "://" + u.getHost() +
		 * signer.signRequest(u.getPath(), u.getQuery());
		 * 
		 * System.out.println("url: " + url);
		 * System.out.println("request: " + request);
		 * 
		 * String response = fetch(request);
		 * 
		 * System.out.println("response: " + response);
		 * //
		 */
		
		final ObjectMapper mapper = JOM.getInstance();
		final ObjectNode directions = mapper.readValue(response,
				ObjectNode.class);
		
		// Check if status is "OK". Error status can for example be "NOT_FOUND"
		String status = null;
		if (directions.has("status")) {
			status = directions.get("status").asText();
		}
		if (!status.equals("OK")) {
			throw new RuntimeException(status);
		}
		
		return directions;
	}
	
	/**
	 * Retrieve the duration of the directions from origin to destination
	 * in seconds.
	 * 
	 * @param origin
	 *            the origin
	 * @param destination
	 *            the destination
	 * @return duration Duration in seconds
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws InvalidKeyException
	 *             the invalid key exception
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws URISyntaxException
	 *             the uRI syntax exception
	 */
	public Integer getDuration(@Name("origin") final String origin,
			@Name("destination") final String destination) throws IOException,
			InvalidKeyException, NoSuchAlgorithmException, URISyntaxException {
		final ObjectNode directions = getDirections(origin, destination);
		
		// TODO: check fields for being null
		final JsonNode routes = directions.get("routes");
		final JsonNode route = routes.get(0);
		final JsonNode legs = route.get("legs");
		final JsonNode leg = legs.get(0);
		final JsonNode jsonDuration = leg.get("duration");
		
		final Integer duration = jsonDuration.get("value").asInt();
		return duration;
	}
	
	/**
	 * Retrieve the duration of the directions from origin to destination
	 * in readable text, for example "59 mins".
	 * 
	 * @param origin
	 *            the origin
	 * @param destination
	 *            the destination
	 * @return duration Duration in text
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws InvalidKeyException
	 *             the invalid key exception
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws URISyntaxException
	 *             the uRI syntax exception
	 */
	public String getDurationHuman(@Name("origin") final String origin,
			@Name("destination") final String destination) throws IOException,
			InvalidKeyException, NoSuchAlgorithmException, URISyntaxException {
		final ObjectNode directions = getDirections(origin, destination);
		
		// TODO: check fields for being null
		final JsonNode routes = directions.get("routes");
		final JsonNode route = routes.get(0);
		final JsonNode legs = route.get("legs");
		final JsonNode leg = legs.get(0);
		final JsonNode jsonDuration = leg.get("duration");
		
		final String duration = jsonDuration.get("text").asText();
		return duration;
	}
	
	/**
	 * Retrieve the distance between origin to destination in meters.
	 * 
	 * @param origin
	 *            the origin
	 * @param destination
	 *            the destination
	 * @return duration Distance in meters
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws InvalidKeyException
	 *             the invalid key exception
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws URISyntaxException
	 *             the uRI syntax exception
	 */
	public Integer getDistance(@Name("origin") final String origin,
			@Name("destination") final String destination) throws IOException,
			InvalidKeyException, NoSuchAlgorithmException, URISyntaxException {
		final ObjectNode directions = getDirections(origin, destination);
		
		// TODO: check fields for being null
		final JsonNode routes = directions.get("routes");
		final JsonNode route = routes.get(0);
		final JsonNode legs = route.get("legs");
		final JsonNode leg = legs.get(0);
		final JsonNode jsonDistance = leg.get("distance");
		
		final Integer distance = jsonDistance.get("value").asInt();
		return distance;
	}
	
	/**
	 * Retrieve the distance between origin to destination in readable text,
	 * for example "74.2 km"
	 * 
	 * @param origin
	 *            the origin
	 * @param destination
	 *            the destination
	 * @return duration Distance in meters
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws InvalidKeyException
	 *             the invalid key exception
	 * @throws NoSuchAlgorithmException
	 *             the no such algorithm exception
	 * @throws URISyntaxException
	 *             the uRI syntax exception
	 */
	public String getDistanceHuman(@Name("origin") final String origin,
			@Name("destination") final String destination) throws IOException,
			InvalidKeyException, NoSuchAlgorithmException, URISyntaxException {
		final ObjectNode directions = getDirections(origin, destination);
		
		// TODO: check fields for being null
		final JsonNode routes = directions.get("routes");
		final JsonNode route = routes.get(0);
		final JsonNode legs = route.get("legs");
		final JsonNode leg = legs.get(0);
		final JsonNode jsonDistance = leg.get("distance");
		
		final String distance = jsonDistance.get("text").asText();
		return distance;
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#getVersion()
	 */
	@Override
	public String getVersion() {
		return "0.1";
	}
	
	/* (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#getDescription()
	 */
	@Override
	public String getDescription() {
		return "This agent is capable of providing directions information "
				+ "using the Google Maps Web Services.";
	}
}
