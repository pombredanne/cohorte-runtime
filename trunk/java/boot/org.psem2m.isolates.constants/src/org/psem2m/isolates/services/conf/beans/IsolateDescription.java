/**
 * File:   IsolateDescription.java
 * Author: Thomas Calmant
 * Date:   2 sept. 2011
 */
package org.psem2m.isolates.services.conf.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.psem2m.isolates.services.conf.IIsolateDescr;

/**
 * Description of an isolate
 * 
 * @author Thomas Calmant
 */
public class IsolateDescription implements Serializable {

    /** Serializable version */
    private static final long serialVersionUID = 1L;

    /** The isolate access URL */
    private String pAccessUrl;

    /** Application arguments */
    private List<String> pAppArgs = new ArrayList<String>();

    /** Isolate bundles (can't be null) */
    private final Set<BundleDescription> pBundles = new LinkedHashSet<BundleDescription>();

    /** The isolate VM class path (mainly for Java) */
    private List<String> pClasspath = new ArrayList<String>();

    /** The isolate process environment variables */
    private Map<String, String> pEnvironment = new HashMap<String, String>();

    /** The isolate host name */
    private String pHostName;

    /** Isolate ID */
    private String pIsolateId;

    /** Isolate kind, must never be null */
    private String pIsolateKind = "";

    /** The OSGi framework JAR file path, if any */
    private String pOsgiFramework;

    /** The isolate signals port */
    private int pPort;

    /** Isolate Java VM arguments (can't be null, must be ordered) */
    private final List<String> pVmArguments = new ArrayList<String>();

    /**
     * Default constructor
     */
    public IsolateDescription() {

        // Do nothing
    }

    /**
     * Sets up the description according to the given map
     * 
     * @param aDescriptionMap
     *            An isolate description map
     */
    public IsolateDescription(final Map<String, Object> aDescriptionMap) {

        // Standard fields
        pIsolateId = (String) aDescriptionMap.get(IIsolateDescr.ISOLATE_ID);
        pPort = (Integer) aDescriptionMap
                .get(IIsolateDescr.ISOLATE_SIGNALS_PORT);
        pOsgiFramework = (String) aDescriptionMap
                .get(IIsolateDescr.ISOLATE_OSGI_FRAMEWORK);

        // "Special" fields
        setHostName((String) aDescriptionMap
                .get(IIsolateDescr.ISOLATE_SIGNALS_HOST));
        setKind((String) aDescriptionMap.get(IIsolateDescr.ISOLATE_KIND));

        // Application arguments
        fillStringCollection(pAppArgs,
                aDescriptionMap.get(IIsolateDescr.ISOLATE_APP_ARGS), true);

        // Class path
        fillStringCollection(pClasspath,
                aDescriptionMap.get(IIsolateDescr.ISOLATE_CLASSPATH), true);

        // VM Arguments
        fillStringCollection(pVmArguments,
                aDescriptionMap.get(IIsolateDescr.ISOLATE_VM_ARGS), true);

        // Bundles
        if (aDescriptionMap.get(IIsolateDescr.ISOLATE_BUNDLES) instanceof Collection) {

            final Collection<?> args = (Collection<?>) aDescriptionMap
                    .get(IIsolateDescr.ISOLATE_VM_ARGS);

            for (final Object arg : args) {
                if (arg instanceof Map) {
                    @SuppressWarnings("unchecked")
                    final Map<String, Object> mapArg = (Map<String, Object>) arg;
                    pBundles.add(new BundleDescription(mapArg));
                }
            }
        }

        // Environment
        if (aDescriptionMap.get(IIsolateDescr.ISOLATE_ENVIRONMENT) instanceof Map) {

            final Map<?, ?> mapEnv = (Map<?, ?>) aDescriptionMap
                    .get(IIsolateDescr.ISOLATE_ENVIRONMENT);

            for (final Entry<?, ?> entry : mapEnv.entrySet()) {

                final String key = (String) entry.getKey();
                if (key == null || key.isEmpty()) {
                    // Ignore empty keys
                    continue;
                }

                final String value = (String) entry.getValue();
                if (value == null) {
                    // Ignore null values
                    continue;
                }

                pEnvironment.put(key, value);
            }
        }
    }

    /**
     * Sets up the isolate description
     * 
     * @param aIsolateId
     */
    public IsolateDescription(final String aIsolateId) {

        pIsolateId = aIsolateId;
    }

    /**
     * Fills the given collection with the given object, if it is a collection
     * 
     * @param aFilledCollection
     *            The collection to fill
     * @param aSourceObject
     *            The source collection (untyped)
     * @param aAvoidNull
     *            If True, null values are ignored
     */
    protected void fillStringCollection(
            final Collection<String> aFilledCollection,
            final Object aSourceObject, final boolean aAvoidNull) {

        if (aFilledCollection == null) {
            // Nothing to fill
            return;
        }

        if (!(aSourceObject instanceof Collection)) {
            // No content
            aFilledCollection.clear();
            return;
        }

        final Collection<?> collection = (Collection<?>) aSourceObject;
        for (final Object obj : collection) {

            if (obj == null && aAvoidNull) {
                continue;
            }

            aFilledCollection.add((String) obj);
        }
    }

    /**
     * Retrieves the forged access URL to the isolate signal receiver
     * 
     * @return The URL to the isolate signal receiver
     */
    public String getAccessUrl() {

        return pAccessUrl;
    }

    /**
     * Retrieves the application argumens
     * 
     * @return the application arguments
     */
    public List<String> getAppArgs() {

        return pAppArgs;
    }

    /**
     * Retrieves the list of bundles described in the isolate configuration
     * 
     * @return The isolate bundles
     */
    public Set<BundleDescription> getBundles() {

        return pBundles;
    }

    /**
     * Retrieves the isolate VM class path (Java)
     * 
     * @return the isolate class path
     */
    public List<String> getClasspath() {

        return pClasspath;
    }

    /**
     * Retrieves the isolate process environment
     * 
     * @return the isolate process environment
     */
    public Map<String, String> getEnvironment() {

        return pEnvironment;
    }

    /**
     * Retrieves the isolate host name
     * 
     * @return the host name
     */
    public String getHostName() {

        return pHostName;
    }

    /**
     * Retrieves the ID of this isolate
     * 
     * @return The isolate ID
     */
    public String getId() {

        return pIsolateId;
    }

    /**
     * Retrieves the kind of this isolate
     * 
     * @return the isolate kind
     */
    public String getKind() {

        return pIsolateKind;
    }

    /**
     * The path to the OSGi framework JAR file
     * 
     * @return the path to the OSGi framework
     */
    public String getOsgiFramework() {

        return pOsgiFramework;
    }

    /**
     * The port to access the isolate signal receiver
     * 
     * @return the signal receiver port
     */
    public int getPort() {

        return pPort;
    }

    /**
     * Retrieves the list of VM / interpreter arguments for this isolate
     * 
     * @return The list of VM arguments
     */
    public List<String> getVmArgs() {

        return pVmArguments;
    }

    /**
     * Sets the bundle access URL. <strong>This method should only be used by
     * libraries based on beans (Jabsorb, ...)</strong>.
     * 
     * The access URL is automatically updated when calling
     * {@link #setHostName(String)} or {@link #setPort(int)}.
     * 
     * @param aUrl
     *            The access URL
     */
    public void setAccessUrl(final String aUrl) {

        pAccessUrl = aUrl;
    }

    /**
     * @param aAppArgs
     *            the appArgs to set
     */
    public void setAppArgs(final List<String> aAppArgs) {

        pAppArgs = aAppArgs;
    }

    /**
     * Sets the isolate bundles
     * 
     * @param aBundles
     *            the isolate bundles
     */
    public void setBundles(final Set<BundleDescription> aBundles) {

        pBundles.clear();

        if (aBundles != null) {
            pBundles.addAll(aBundles);
        }
    }

    /**
     * @param aClasspath
     *            the classpath to set
     */
    public void setClasspath(final List<String> aClasspath) {

        pClasspath.clear();

        if (aClasspath != null) {
            pClasspath.addAll(aClasspath);
        }
    }

    /**
     * @param aEnvironment
     *            the environment to set
     */
    public void setEnvironment(final Map<String, String> aEnvironment) {

        pEnvironment.clear();
        if (aEnvironment != null) {
            pEnvironment.putAll(aEnvironment);
        }
    }

    /**
     * Sets the isolate host name
     * 
     * @param aHostName
     *            the host name
     */
    public void setHostName(final String aHostName) {

        if (aHostName != null) {
            pHostName = aHostName;

        } else {
            pHostName = "localhost";
        }

        // Update the access URL
        updateAccessUrl();
    }

    /**
     * Sets the isolate ID
     * 
     * @param aIsolateId
     *            the isolate ID
     */
    public void setId(final String aIsolateId) {

        pIsolateId = aIsolateId;
    }

    /**
     * Sets the kind of isolate
     * 
     * @param aKind
     *            The kind of the isolate
     * 
     * @see IIsolateDescr#getKind()
     */
    public void setKind(final String aKind) {

        if (aKind != null) {
            pIsolateKind = aKind;

        } else {
            pIsolateKind = "";
        }
    }

    /**
     * @param aOsgiFramework
     *            the osgiFramework to set
     */
    public void setOsgiFramework(final String aOsgiFramework) {

        pOsgiFramework = aOsgiFramework;
    }

    /**
     * @param aPort
     *            the port to set
     */
    public void setPort(final int aPort) {

        pPort = aPort;

        // Update the access URL
        updateAccessUrl();
    }

    /**
     * Sets the Java virtual machine arguments
     * 
     * @param aVmArgs
     *            the VM arguments
     */
    public void setVMArgs(final List<String> aVmArgs) {

        pVmArguments.clear();

        if (aVmArgs != null) {
            pVmArguments.addAll(aVmArgs);
        }
    }

    /**
     * Converts this description into its map representation
     * 
     * @return The map representation of this object
     */
    public Map<String, Object> toMap() {

        final Map<String, Object> map = new HashMap<String, Object>();

        map.put(IIsolateDescr.ISOLATE_APP_ARGS, pAppArgs);
        map.put(IIsolateDescr.ISOLATE_CLASSPATH, pClasspath);
        map.put(IIsolateDescr.ISOLATE_ENVIRONMENT, pEnvironment);
        map.put(IIsolateDescr.ISOLATE_ID, pIsolateId);
        map.put(IIsolateDescr.ISOLATE_KIND, pIsolateKind);
        map.put(IIsolateDescr.ISOLATE_OSGI_FRAMEWORK, pOsgiFramework);
        map.put(IIsolateDescr.ISOLATE_SIGNALS_HOST, pHostName);
        map.put(IIsolateDescr.ISOLATE_SIGNALS_PORT, pPort);
        map.put(IIsolateDescr.ISOLATE_VM_ARGS, pVmArguments);

        // Bundles, converted to maps
        final List<Map<String, Object>> bundlesMap = new ArrayList<Map<String, Object>>();
        for (final BundleDescription bundle : pBundles) {
            bundlesMap.add(bundle.toMap());
        }

        map.put(IIsolateDescr.ISOLATE_BUNDLES, bundlesMap);

        return map;
    }

    /**
     * Sets up the access URL, according to the current host name and port
     */
    protected void updateAccessUrl() {

        final StringBuilder builder = new StringBuilder();
        builder.append("http://");
        builder.append(getHostName());
        builder.append(":");
        builder.append(pPort);

        pAccessUrl = builder.toString();
    }
}