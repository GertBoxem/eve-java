/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.algorithms;

import java.util.concurrent.Executor;

import org.joda.time.DateTime;

import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Namespace;
import com.almende.eve.protocol.jsonrpc.formats.JSONRequest;
import com.almende.eve.scheduling.Scheduler;
import com.almende.util.threads.ThreadPool;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class TrickleRPC.
 */
@Namespace("trickle")
public class TrickleRPC {
	private Trickle		trickle		= null;
	private String		intTaskId	= null;
	private String		sendTaskId	= null;
	private Scheduler	scheduler	= null;
	private Runnable	onInterval	= null;
	private Runnable	onSend		= null;
	private Executor	executer	= ThreadPool.getPool();

	/**
	 * Instantiates a new trickle rpc.
	 *
	 * @param config
	 *            the config
	 * @param scheduler
	 *            the scheduler
	 * @param onInterval
	 *            the on interval
	 * @param onSend
	 *            the on send
	 */
	public TrickleRPC(final ObjectNode config, Scheduler scheduler,
			Runnable onInterval, Runnable onSend) {
		this.scheduler = scheduler;
		this.onInterval = onInterval;
		this.onSend = onSend;

		int intervalMin = 100;
		int intervalFactor = 16;
		int redundancyFactor = 3;

		trickle = new Trickle(intervalMin, intervalFactor, redundancyFactor);
		reschedule(trickle.next());
	}

	private void reschedule(final long[] intervals) {
		sendTaskId = scheduler.schedule(new JSONRequest("trickle.send", null),
				DateTime.now().plus(intervals[0]));
		intTaskId = scheduler.schedule(new JSONRequest("trickle.nextInterval",
				null), DateTime.now().plus(intervals[1]));
	}

	/**
	 * Send value.
	 */
	@Access(AccessType.SELF)
	public void send() {
		if (trickle.check()) {
			executer.execute(onSend);
		}
	}

	/**
	 * Next interval.
	 */
	@Access(AccessType.SELF)
	public void nextInterval() {
		executer.execute(onInterval);
		reschedule(trickle.next());
	}

	/**
	 * Reset.
	 */
	public void reset() {
		scheduler.cancel(sendTaskId);
		scheduler.cancel(intTaskId);
		reschedule(trickle.reset());
	}

	/**
	 * Incr.
	 */
	public void incr() {
		trickle.incr();
	}

}
