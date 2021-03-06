/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.eve.deploy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentBuilder;
import com.almende.eve.agent.AgentConfig;
import com.almende.eve.capabilities.Config;
import com.almende.eve.config.YamlReader;
import com.almende.eve.instantiation.InstantiationService;
import com.almende.eve.instantiation.InstantiationServiceBuilder;
import com.almende.eve.instantiation.InstantiationServiceConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class Boot.
 */
public final class Boot {
	private static final Logger	LOG	= Logger.getLogger(Boot.class.getName());

	private Boot() {}

	/**
	 * The default agent booter. It takes an EVE yaml file and creates all
	 * agents mentioned in the "agents" section.
	 * 
	 * @param args
	 *            Single argument: args[0] -> Eve yaml
	 */
	public static void main(final String[] args) {
		if (args.length == 0) {
			LOG.warning("Missing argument pointing to yaml file:");
			LOG.warning("Usage: java -jar <jarfile> eve.yaml");
			return;
		}
		final ClassLoader cl = new ClassLoader() {
			@Override
			protected Class<?> findClass(final String name)
					throws ClassNotFoundException {
				Class<?> result = null;
				try {
					result = super.findClass(name);
				} catch (ClassNotFoundException cne) {}
				if (result == null) {
					FileInputStream fi = null;
					try {

						String path = name.replace('.', '/');
						fi = new FileInputStream(System.getProperty("user.dir")
								+ "/" + path + ".class");
						byte[] classBytes = new byte[fi.available()];
						fi.read(classBytes);
						fi.close();
						return defineClass(name, classBytes, 0,
								classBytes.length);
					} catch (Exception e) {
						LOG.log(Level.WARNING, "Failed to load class:", e);
					}
				}
				if (result == null) {
					throw new ClassNotFoundException(name);
				}
				return result;
			}
		};
		String configFileName = args[0];
		try {
			InputStream is = new FileInputStream(new File(configFileName));
			boot(is, cl);

		} catch (FileNotFoundException e) {
			LOG.log(Level.WARNING,
					"Couldn't find configfile:" + configFileName, e);
			return;
		}

	}

	/**
	 * Boot.
	 *
	 * @param is
	 *            the is
	 * @return the object node
	 */
	public static ObjectNode boot(final InputStream is) {
		return boot(is, null);
	}

	/**
	 * Boot.
	 *
	 * @param config
	 *            the config
	 * @return the object node
	 */
	public static ObjectNode boot(final ObjectNode config) {
		return boot(config, null);
	}

	/**
	 * Boot.
	 *
	 * @param config
	 *            the config
	 * @param cl
	 *            the cl
	 * @return the object node
	 */
	public static ObjectNode boot(final ObjectNode config, final ClassLoader cl) {
		final Config conf = new Config(config);
		return boot(conf, null);
	}

	/**
	 * Boot.
	 *
	 * @param is
	 *            the is
	 * @param cl
	 *            the cl
	 * @return the object node
	 */
	public static ObjectNode boot(final InputStream is, final ClassLoader cl) {
		final Config config = YamlReader.load(is).expand();
		return boot(config, cl);
	}

	/**
	 * Boot.
	 *
	 * @param config
	 *            the config
	 * @param cl
	 *            the cl
	 * @return the object node
	 */
	public static ObjectNode boot(final Config config, final ClassLoader cl) {
		loadInstantiationServices(config, cl);
		loadAgents(config, cl);
		return config;
	}

	/**
	 * Load instantiation services.
	 *
	 * @param config
	 *            the config
	 * @param cl
	 *            the cl
	 */
	public static void loadInstantiationServices(final Config config,
			final ClassLoader cl) {
		if (!config.has("instantiationServices")) {
			return;
		}
		final ArrayNode iss = (ArrayNode) config.get("instantiationServices");
		for (final JsonNode service : iss) {
			final InstantiationServiceConfig isconfig = new InstantiationServiceConfig(
					(ObjectNode) service);
			final InstantiationService is = new InstantiationServiceBuilder()
					.withClassLoader(cl).withConfig(isconfig).build();
			is.boot();
		}
	}

	/**
	 * Load agents.
	 *
	 * @param config
	 *            the config
	 * @param cl
	 *            the custom classloader
	 */
	public static void loadAgents(final Config config, final ClassLoader cl) {
		if (!config.has("agents")) {
			return;
		}

		final ArrayNode agents = (ArrayNode) config.get("agents");

		for (final JsonNode agent : agents) {
			final AgentConfig agentConfig = new AgentConfig((ObjectNode) agent);
			final Agent newAgent = new AgentBuilder().withClassLoader(cl)
					.with(agentConfig).build();
			LOG.info("Created agent:" + newAgent.getId());
		}
	}
}
