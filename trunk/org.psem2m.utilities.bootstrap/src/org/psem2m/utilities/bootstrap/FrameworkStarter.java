/**
 * File:   FrameworkStarter.java
 * Author: Thomas Calmant
 * Date:   6 juil. 2011
 */
package org.psem2m.utilities.bootstrap;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.psem2m.isolates.base.boot.IBootstrapMessageSender;
import org.psem2m.isolates.constants.IPlatformProperties;

/**
 * Bootstrap for OSGi frameworks
 * 
 * @author Thomas Calmant
 */
public class FrameworkStarter {

    /** Name to use in logs */
    private static final String CLASS_LOG_NAME = FrameworkStarter.class
	    .getSimpleName();

    /** Default OSGi pFramework : Felix */
    public static final String DEFAULT_FRAMEWORK = "org.apache.felix.framework.FrameworkFactory";

    /** Framework factories content */
    public static final Map<String, String> FRAMEWORK_FACTORIES = new TreeMap<String, String>();

    static {
	// Initialize the static map
	FRAMEWORK_FACTORIES.put("equinox",
		"org.eclipse.osgi.launch.EquinoxFactory");

	FRAMEWORK_FACTORIES.put("felix",
		"org.apache.felix.framework.FrameworkFactory");
    }

    /** The bootstrap configuration */
    private Map<String, String> pBootstrapConfiguration = new TreeMap<String, String>();

    /** Bootstrap message sender service */
    private final BootstrapMessageSender pBootstrapService;

    /** The launched framework */
    private Framework pFramework;

    /** The framework/system configuration */
    private Map<String, String> pFrameworkConfiguration = new TreeMap<String, String>();

    /** The installed bundles list */
    private List<Bundle> pInstalledBundles = new ArrayList<Bundle>();

    /** Log message sender */
    private IMessageSender pMessageSender;

    /**
     * Prepares the bootstrap members
     * 
     * @param aMessageSender
     *            Log message sender
     * 
     * @param aBootstrapConfiguration
     *            Bootstrap configuration map
     * 
     * @param aFrameworkConfiguration
     *            OSGi framework configuration map
     */
    public FrameworkStarter(final IMessageSender aMessageSender,
	    final Map<String, String> aBootstrapConfiguration,
	    final Map<String, String> aFrameworkConfiguration) {

	// Set up the message sender
	pMessageSender = aMessageSender;

	// Create the bootstrap message sender service instance
	pBootstrapService = new BootstrapMessageSender(pMessageSender);

	// Prepare the configurations
	if (aBootstrapConfiguration != null) {
	    pBootstrapConfiguration.putAll(aBootstrapConfiguration);
	}

	if (aFrameworkConfiguration != null) {
	    pFrameworkConfiguration.putAll(aFrameworkConfiguration);
	}

	// Flush the cache by default
	if (!pFrameworkConfiguration.containsKey(Constants.FRAMEWORK_STORAGE)) {

	    // Find an isolate ID
	    String isolateId = pFrameworkConfiguration
		    .get(IPlatformProperties.PROP_PLATFORM_ISOLATE_ID);
	    if (isolateId == null) {
		System.getProperty(
			IPlatformProperties.PROP_PLATFORM_ISOLATE_ID,
			"unknown-isolate");
	    }

	    // Prepare the directory
	    File frameworkCacheDirectory = new File(System.getProperty(
		    IPlatformProperties.PROP_PLATFORM_BASE, "."), "var/work/"
		    + isolateId);

	    if (frameworkCacheDirectory.exists()) {
		// Dump it ...
		removeDirectory(frameworkCacheDirectory);

	    } else {
		// ... or create it if necessary
		frameworkCacheDirectory.mkdirs();
	    }

	    // Set up the property
	    pFrameworkConfiguration.put(Constants.FRAMEWORK_STORAGE,
		    frameworkCacheDirectory.getAbsolutePath());
	}

	/*
	 * Extra system package, to allow a dialog between bundles and the
	 * bootstrap.
	 * 
	 * IMPORTANT: Use a package version greater than the one exported by
	 * isolates.base, else it won't be imported when needed.
	 * 
	 * IMPORTANT 2: As isolates.base contains this package, it won't the one
	 * we export here.
	 */
	pFrameworkConfiguration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
		"org.psem2m.isolates.base.boot; version=1.0.1");

	// Force the system properties
	for (Entry<String, String> property : pFrameworkConfiguration
		.entrySet()) {
	    System.setProperty(property.getKey(), property.getValue());
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.utilities.bootstrap.impl.ISvcBootstrap#createFramework()
     */
    public Framework createFramework() {

	final FrameworkFactory factory = getFrameworkFactory();
	if (factory == null) {
	    // Could not find the factory
	    return null;
	}

	// Create the framework object
	pFramework = factory.newFramework(pFrameworkConfiguration);

	try {
	    // Initializes the framework
	    pFramework.init();

	} catch (BundleException e) {
	    pMessageSender.sendMessage(Level.SEVERE, CLASS_LOG_NAME,
		    "createFramework", "Framework inialization error", e);

	    e.printStackTrace();
	    pFramework = null;
	}

	return pFramework;
    }

    /**
     * Retrieves the OSGi pFramework factory
     * 
     * @return A FrameworkFactory pSingleton, null on error
     */
    protected FrameworkFactory getFrameworkFactory() {

	String osgiFramework = pBootstrapConfiguration
		.get(IBootstrapConstants.CONFIG_FRAMEWORK);
	if (osgiFramework == null) {
	    osgiFramework = DEFAULT_FRAMEWORK;
	}

	// Try to find the factory
	String factoryName = FRAMEWORK_FACTORIES.get(osgiFramework);

	// If none found, consider the configuration as the factory
	if (factoryName == null) {
	    factoryName = osgiFramework;
	}

	// Find and instantiate the class
	try {
	    return (FrameworkFactory) Class.forName(factoryName).newInstance();

	} catch (InstantiationException e) {
	    pMessageSender.sendMessage(Level.SEVERE, CLASS_LOG_NAME,
		    "getFrameworkFactory", "Can't create a new instance", e);

	} catch (IllegalAccessException e) {
	    pMessageSender.sendMessage(Level.SEVERE, CLASS_LOG_NAME,
		    "getFrameworkFactory", "Illegal class access", e);

	} catch (ClassNotFoundException e) {
	    pMessageSender.sendMessage(Level.SEVERE, CLASS_LOG_NAME,
		    "getFrameworkFactory", "Factory class not found", e);

	}

	return null;
    }

    /**
     * Installs the bootstrap message sender service
     */
    protected void installBootstrapService() {

	pFramework.getBundleContext().registerService(
		IBootstrapMessageSender.class.getName(), pBootstrapService,
		null);

	try {
	    pFramework.getBundleContext().getClass().getClassLoader()
		    .loadClass(IBootstrapMessageSender.class.getName());
	} catch (ClassNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.psem2m.utilities.bootstrap.impl.ISvcBootstrap#installBundles(java
     * .net.URL[])
     */
    public boolean installBundles(final URL[] aBundlesConfiguration) {

	boolean success = true;
	for (URL url : aBundlesConfiguration) {
	    try {
		pInstalledBundles.add(pFramework.getBundleContext()
			.installBundle(url.toString()));

	    } catch (BundleException e) {
		pMessageSender.sendMessage(Level.SEVERE, CLASS_LOG_NAME,
			"installBundles", "Error installing bundle '" + url
				+ "'", e);

		e.printStackTrace();
		success = false;
	    }
	}

	return success;
    }

    /**
     * Empties a directory and deletes it
     * 
     * @param aDirectory
     *            Directory to delete
     */
    protected void removeDirectory(final File aDirectory) {

	File[] files = aDirectory.listFiles();

	for (File file : files) {
	    if (file.isDirectory()) {
		removeDirectory(file);

	    }

	    file.delete();
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.psem2m.utilities.bootstrap.impl.ISvcBootstrap#setMessageSender(org
     * .psem2m.utilities.bootstrap.IMessageSender)
     */
    public void setMessageSender(final IMessageSender aMessageSender) {

	if (aMessageSender != null) {
	    pMessageSender = aMessageSender;
	    pBootstrapService.setMessageSender(pMessageSender);
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.utilities.bootstrap.impl.ISvcBootstrap#startBundles()
     */
    public boolean startBundles() {

	boolean success = true;
	for (Bundle bundle : pInstalledBundles) {
	    try {
		pMessageSender.sendMessage(Level.INFO, CLASS_LOG_NAME,
			"startBundles",
			"Starting : " + bundle.getSymbolicName());
		bundle.start();

	    } catch (BundleException e) {
		pMessageSender.sendMessage(Level.SEVERE, CLASS_LOG_NAME,
			"startBundles",
			"Error starting bundle : '" + bundle.getSymbolicName()
				+ "'", e);

		e.printStackTrace();
		success = false;
	    }
	}

	return success;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.utilities.bootstrap.impl.ISvcBootstrap#startFramework()
     */
    public boolean startFramework() {

	try {
	    pFramework.start();
	    return true;

	} catch (BundleException e) {
	    pMessageSender.sendMessage(Level.SEVERE, CLASS_LOG_NAME,
		    "startFramework", "Error starting the framework", e);
	    e.printStackTrace();
	}

	return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.utilities.bootstrap.impl.ISvcBootstrap#stopFramework()
     */
    public boolean stopFramework() {

	if (pFramework.getState() != Bundle.ACTIVE) {
	    // The framework is already in a non-active state
	    return true;
	}

	try {
	    pFramework.stop();
	    return true;

	} catch (BundleException e) {
	    pMessageSender.sendMessage(Level.SEVERE, CLASS_LOG_NAME,
		    "stopFramework", "Error stopping the framework", e);

	    e.printStackTrace();
	}

	return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.utilities.bootstrap.impl.ISvcBootstrap#waitForStop(int)
     */
    public boolean waitForStop(final int aTimeout) throws InterruptedException {

	FrameworkEvent event = pFramework.waitForStop(aTimeout);
	return event.getType() != FrameworkEvent.WAIT_TIMEDOUT;
    }
}
