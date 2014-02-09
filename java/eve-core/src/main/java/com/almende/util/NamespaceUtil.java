/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.almende.eve.agent.annotation.Namespace;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.AnnotationUtil.AnnotatedClass;
import com.almende.util.AnnotationUtil.AnnotatedMethod;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * The Class NamespaceUtil.
 */
public final class NamespaceUtil {
	
	private static Map<String, Method[]>	cache		= new HashMap<String, Method[]>();
	private static NamespaceUtil			instance	= new NamespaceUtil();
	private static final Pattern			PATTERN		= Pattern
																.compile("\\.[^.]+$");
	
	/**
	 * Instantiates a new namespace util.
	 */
	private NamespaceUtil() {
	};
	
	/**
	 * Gets the.
	 * 
	 * @param destination
	 *            the destination
	 * @param path
	 *            the path
	 * @return the call tuple
	 * @throws IllegalAccessException
	 *             the illegal access exception
	 * @throws InvocationTargetException
	 *             the invocation target exception
	 * @throws NoSuchMethodException
	 *             the no such method exception
	 */
	public static CallTuple get(final Object destination, final String path)
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		
		return instance._get(destination, path);
	}
	
	/**
	 * Populate cache.
	 * 
	 * @param destination
	 *            the destination
	 * @param steps
	 *            the steps
	 * @param methods
	 *            the methods
	 * @throws IllegalAccessException
	 *             the illegal access exception
	 * @throws InvocationTargetException
	 *             the invocation target exception
	 */
	private void populateCache(final Object destination, final String steps,
			final Method[] methods) throws IllegalAccessException,
			InvocationTargetException {
		final AnnotatedClass clazz = AnnotationUtil.get(destination.getClass());
		for (final AnnotatedMethod method : clazz
				.getAnnotatedMethods(Namespace.class)) {
			final String path = steps + "."
					+ method.getAnnotation(Namespace.class).value();
			methods[methods.length - 1] = method.getActualMethod();
			cache.put(path, Arrays.copyOf(methods, methods.length));
			
			final Object newDest = method.getActualMethod().invoke(destination,
					(Object[]) null);
			// recurse:
			if (newDest != null) {
				populateCache(newDest, path,
						Arrays.copyOf(methods, methods.length + 1));
			}
		}
	}
	
	/**
	 * _get.
	 * 
	 * @param destination
	 *            the destination
	 * @param path
	 *            the path
	 * @return the call tuple
	 * @throws IllegalAccessException
	 *             the illegal access exception
	 * @throws InvocationTargetException
	 *             the invocation target exception
	 * @throws NoSuchMethodException
	 *             the no such method exception
	 */
	private CallTuple _get(Object destination, String path)
			throws IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		final CallTuple result = new CallTuple();
		
		if (!path.contains(".")) {
			// Quick shortcut back
			result.setDestination(destination);
			result.setMethodName(path);
			return result;
		}
		
		//If destination has a cache, use it.
		if (destination instanceof RPCCallCache){
			 final CallTuple res = ((RPCCallCache)destination).getCallTuple(path);
			 if (res != null){
				 return res;
			 }
		}
		
		path = destination.getClass().getName() + "." + path;
		String[] steps = path.split("\\.");
		final String reducedMethod = steps[steps.length - 1];
		steps = Arrays.copyOf(steps, steps.length - 1);
		path = PATTERN.matcher(path).replaceFirst("");
		
		if (!cache.containsKey(path)) {
			final Method[] methods = new Method[1];
			final String newSteps = destination.getClass().getName();
			populateCache(destination, newSteps, methods);
		}
		if (!cache.containsKey(path)) {
			try {
				throw new IllegalStateException("Non resolveable path given:'"
						+ path + "' \n checked:"
						+ JOM.getInstance().writeValueAsString(cache));
			} catch (final JsonProcessingException e) {
				throw new IllegalStateException("Non resolveable path given:'"
						+ path + "' \n checked:" + cache);
			}
		}
		final Method[] methods = cache.get(path);
		Object newDestination = destination;
		for (final Method method : methods) {
			if (method != null) {
				newDestination = method.invoke(destination, (Object[]) null);
			}
		}
		result.setDestination(newDestination);
		result.setMethodName(reducedMethod);
		
		if (destination instanceof RPCCallCache){
			((RPCCallCache)destination).putCallTuple(path, result);
		} 
		return result;
	}
	
	/**
	 * The Class CallTuple.
	 */
	public class CallTuple {
		
		/** The destination. */
		private Object	destination;
		
		/** The method name. */
		private String	methodName;
		
		/**
		 * Gets the destination.
		 * 
		 * @return the destination
		 */
		public Object getDestination() {
			return destination;
		}
		
		/**
		 * Sets the destination.
		 * 
		 * @param destination
		 *            the new destination
		 */
		public void setDestination(final Object destination) {
			this.destination = destination;
		}
		
		/**
		 * Gets the method name.
		 * 
		 * @return the method name
		 */
		public String getMethodName() {
			return methodName;
		}
		
		/**
		 * Sets the method name.
		 * 
		 * @param methodName
		 *            the new method name
		 */
		public void setMethodName(final String methodName) {
			this.methodName = methodName;
		}
	}
}
