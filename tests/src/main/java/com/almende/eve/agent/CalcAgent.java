/**
 * @file CalcAgent.java
 * @brief
 *        CalcAgent can evaluate mathematical expressions.
 *        It uses the math.js RESTful API.
 * @license
 *          Licensed under the Apache License, Version 2.0 (the "License"); you
 *          may not
 *          use this file except in compliance with the License. You may obtain
 *          a copy
 *          of the License at
 *          http://www.apache.org/licenses/LICENSE-2.0
 *          Unless required by applicable law or agreed to in writing, software
 *          distributed under the License is distributed on an "AS IS" BASIS,
 *          WITHOUT
 *          WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 *          the
 *          License for the specific language governing permissions and
 *          limitations under
 *          the License.
 *          Copyright © 2012-2013 Almende B.V.
 * @author Jos de Jong, <jos@almende.org>
 * @date 2013-11-29
 */
package com.almende.eve.agent;

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.util.StringUtil;

/**
 * The Class CalcAgent.
 */
@Access(AccessType.PUBLIC)
public class CalcAgent extends Agent {
	private static final String	CALC_API_URL	= "http://api.mathjs.org/v1/";

	/**
	 * Evaluate given expression For example expr="2.5 + 3 / sqrt(16)" will
	 * return "3.25"
	 * 
	 * @param expr
	 *            the expr
	 * @return result
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String eval(@Name("expr") final String expr) throws IOException {
		final String url = CALC_API_URL + "?expr="
				+ URLEncoder.encode(expr, "UTF-8");

		final HttpClient client = HttpClients.createDefault();
		final HttpGet request = new HttpGet(url);
		final HttpResponse response = client.execute(request);

		return StringUtil.streamToString(response.getEntity().getContent());
	}

}
