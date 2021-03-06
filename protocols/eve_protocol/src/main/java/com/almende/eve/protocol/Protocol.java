/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.protocol;

import java.net.URI;

import com.almende.eve.capabilities.Capability;

/**
 * The Interface Protocol.
 */
public interface Protocol extends Capability {

	/**
	 * The Class Meta.
	 */
	class Meta {
		public Object	result	= null;
		public boolean	doNext	= true;
		public boolean	valid	= true;

		public Meta() {}

		public Meta(final Object result, final boolean doNext,
				final boolean valid) {
			this.result = result;
			this.doNext = doNext;
			this.valid = valid;
		}

		public Meta(final Object result) {
			this.result = result;
		}

		public String toString() {
			return result.toString();
		}

		public Object getResult() {
			return result;
		}

		public void setResult(Object result) {
			this.result = result;
		}

		public boolean isDoNext() {
			return doNext;
		}

		public void setDoNext(boolean doNext) {
			this.doNext = doNext;
		}

		public boolean isValid() {
			return valid;
		}

		public void setValid(boolean valid) {
			this.valid = valid;
		}

	}

	/**
	 * Handle inbound messages, converting them from the right protocols.
	 *
	 * @param msg
	 *            the msg
	 * @param senderUrl
	 *            the sender url
	 * @return the modified msg or null if no chaining is allowed
	 */
	Meta inbound(final Object msg, final URI senderUrl);

	/**
	 * Handle outbound messages, converting them into the right protocols.
	 *
	 * @param msg
	 *            the msg or null if this protocol creates the content (is a
	 *            source)
	 * @param recipientUrl
	 *            the recipient url
	 * @return the modified msg
	 */
	Meta outbound(final Object msg, final URI recipientUrl);

}
