/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * The Class ClassUtil.
 */
public final class ClassUtil {
	private static final Map<Class<?>, Class<?>>	PRIMITIVES_TO_WRAPPERS	= new HashMap<Class<?>, Class<?>>();
	static {
		PRIMITIVES_TO_WRAPPERS.put(boolean.class, Boolean.class);
		PRIMITIVES_TO_WRAPPERS.put(byte.class, Byte.class);
		PRIMITIVES_TO_WRAPPERS.put(char.class, Character.class);
		PRIMITIVES_TO_WRAPPERS.put(double.class, Double.class);
		PRIMITIVES_TO_WRAPPERS.put(float.class, Float.class);
		PRIMITIVES_TO_WRAPPERS.put(int.class, Integer.class);
		PRIMITIVES_TO_WRAPPERS.put(long.class, Long.class);
		PRIMITIVES_TO_WRAPPERS.put(short.class, Short.class);
		PRIMITIVES_TO_WRAPPERS.put(void.class, Void.class);
	}
	private static final Map<Class<?>, Class<?>>	WRAPPERS_TO_PRIMITIVES	= new HashMap<Class<?>, Class<?>>();
	static {
		WRAPPERS_TO_PRIMITIVES.put(Boolean.class, boolean.class);
		WRAPPERS_TO_PRIMITIVES.put(Byte.class, byte.class);
		WRAPPERS_TO_PRIMITIVES.put(Character.class, char.class);
		WRAPPERS_TO_PRIMITIVES.put(Double.class, double.class);
		WRAPPERS_TO_PRIMITIVES.put(Float.class, float.class);
		WRAPPERS_TO_PRIMITIVES.put(Integer.class, int.class);
		WRAPPERS_TO_PRIMITIVES.put(Long.class, long.class);
		WRAPPERS_TO_PRIMITIVES.put(Short.class, short.class);
		WRAPPERS_TO_PRIMITIVES.put(Void.class, void.class);
	}
	
	/**
	 * Instantiates a new class util.
	 */
	private ClassUtil() {
	};
	
	/**
	 * Check if checkClass has implemented interfaceClass.
	 * 
	 * @param checkClass
	 *            the check class
	 * @param interfaceClass
	 *            the interface class
	 * @return true, if successful
	 */
	public static boolean hasInterface(final Class<?> checkClass,
			final Class<?> interfaceClass) {
		final String name = interfaceClass.getName();
		Class<?> s = checkClass;
		while (s != null) {
			final Class<?>[] interfaces = s.getInterfaces();
			for (final Class<?> i : interfaces) {
				if (i.getName().equals(name)) {
					return true;
				}
				if (hasInterface(s, i)) {
					return true;
				}
			}
			
			s = s.getSuperclass();
		}
		
		return false;
	}
	
	/**
	 * Check if checkClass extends superClass.
	 * 
	 * @param checkClass
	 *            the check class
	 * @param superClass
	 *            the super class
	 * @return true, if successful
	 */
	public static boolean hasSuperClass(final Class<?> checkClass,
			final Class<?> superClass) {
		// TODO: replace with return (checkClass instanceof superClass); ?
		final String name = superClass.getName();
		Class<?> s = (checkClass != null) ? checkClass.getSuperclass() : null;
		while (s != null) {
			if (s.getName().equals(name)) {
				return true;
			}
			s = s.getSuperclass();
		}
		
		return false;
	}
	
	/**
	 * Wraps any primitive type in it's boxed version
	 * returns other types unmodified.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param c
	 *            the c
	 * @return class type
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> wrap(final Class<T> c) {
		return c.isPrimitive() ? (Class<T>) PRIMITIVES_TO_WRAPPERS.get(c) : c;
	}
	
	/**
	 * Unwraps any boxed type in it's primitive version
	 * returns other types unmodified.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param c
	 *            the c
	 * @return class type
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<T> unWrap(final Class<T> c) {
		return WRAPPERS_TO_PRIMITIVES.containsKey(c) ? (Class<T>) WRAPPERS_TO_PRIMITIVES
				.get(c) : c;
	}
	
	/**
	 * Search for method (reflection) which fits the given argument types. Works
	 * for any combination of
	 * primitive types, boxed types and normal objects.
	 * 
	 * @param type
	 *            Class in which the method is searched
	 * @param name
	 *            Method name to search for
	 * @param parms
	 *            Class types of the requested arguments
	 * @return Method
	 * @author PSpeed
	 *         http://stackoverflow.com/questions/1894740/any-solution-for
	 *         -class-getmethod-reflection-and-autoboxing
	 */
	public static Method searchForMethod(final Class<?> type,
			final String name, final Class<?>[] parms) {
		final Method[] methods = type.getMethods();
		for (int i = 0; i < methods.length; i++) {
			// Has to be named the same of course.
			if (!methods[i].getName().equals(name)) {
				continue;
			}
			
			final Class<?>[] types = methods[i].getParameterTypes();
			
			// Does it have the same number of arguments that we're looking for.
			if (types.length != parms.length) {
				continue;
			}
			
			// Check for type compatibility
			if (areTypesCompatible(types, parms)) {
				return methods[i];
			}
		}
		return null;
	}
	
	/**
	 * Are types compatible.
	 * 
	 * @param targets
	 *            the targets
	 * @param sources
	 *            the sources
	 * @return true, if successful
	 */
	public static boolean areTypesCompatible(final Class<?>[] targets,
			final Class<?>[] sources) {
		
		if (targets.length != sources.length) {
			return false;
		}
		
		for (int i = 0; i < targets.length; i++) {
			if (sources[i] == null) {
				continue;
			}
			
			if (!wrap(targets[i]).isAssignableFrom(sources[i])) {
				return false;
			}
		}
		return (true);
	}
	
	/**
	 * Get the underlying class for a type, or null if the type is a variable
	 * type. See <a
	 * href="http://www.artima.com/weblogs/viewpost.jsp?thread=208860"
	 * >description</a>
	 * 
	 * @param type
	 *            the type
	 * @return the underlying class
	 */
	public static Class<?> getClass(final Type type) {
		if (type instanceof Class) {
			return (Class<?>) type;
		}
		
		if (type instanceof ParameterizedType) {
			return getClass(((ParameterizedType) type).getRawType());
		}
		
		if (type instanceof GenericArrayType) {
			final Type componentType = ((GenericArrayType) type)
					.getGenericComponentType();
			final Class<?> componentClass = getClass(componentType);
			if (componentClass != null) {
				return Array.newInstance(componentClass, 0).getClass();
			}
			
		}
		return null;
	}
	
	// These methods were found at:
	// http://www.javacodegeeks.com/2011/12/cloning-of-serializable-and-non.html
	// @Author Craig Flichel
	
	/**
	 * Clone through serialize.
	 * 
	 * @param <T>
	 *            the generic type
	 * @param t
	 *            the t
	 * @return the t
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T cloneThroughSerialize(final T t)
			throws IOException, ClassNotFoundException {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		serializeToOutputStream(t, bos);
		final byte[] bytes = bos.toByteArray();
		final ObjectInputStream ois = new ObjectInputStream(
				new ByteArrayInputStream(bytes));
		return (T) ois.readObject();
	}
	
	/**
	 * Serialize to output stream.
	 * 
	 * @param ser
	 *            the ser
	 * @param os
	 *            the os
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private static void serializeToOutputStream(final Serializable ser,
			final OutputStream os) throws IOException {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(os);
			oos.writeObject(ser);
			oos.flush();
		} finally {
			oos.close();
		}
	}
}
