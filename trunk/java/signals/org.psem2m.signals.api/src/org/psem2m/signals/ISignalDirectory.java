/**
 * File:   ISignalDirectory.java
 * Author: Thomas Calmant
 * Date:   19 déc. 2011
 */
package org.psem2m.signals;

import java.util.Collection;
import java.util.Map;

/**
 * Defines a directory for the PSEM2M Signals services. Provides access strings
 * to each isolates or to a group of isolates.
 * 
 * @author Thomas Calmant
 */
public interface ISignalDirectory {

    /**
     * Constants to handle base groups, computed by the directory itself
     * 
     * @author Thomas Calmant
     */
    public enum EBaseGroup {
        /** All isolates, including the current one */
        ALL,

        /** Current isolate */
        CURRENT,

        /** All forkers, including the current isolate if it is a forker */
        FORKERS,

        /**
         * All isolates, including monitors and the current one, excluding
         * forkers. If the current isolate is a forker, it is excluded.
         */
        ISOLATES,

        /** All monitors, including the current isolate if it is a monitor */
        MONITORS,

        /** All isolates on the current node, excluding the current one */
        NEIGHBOURS,

        /** All isolates, with monitors and forkers, but this one */
        OTHERS,

        /**
         * All isolates, with monitors and forkers, but this one, even if they
         * are not validated
         */
        STORED,
    }

    /**
     * Represents the access to the local isolate
     */
    HostAccess LOCAL_ACCESS = new HostAccess(null, -1);

    /**
     * Returns a snapshot of the directory.
     * 
     * The result is a map with 4 entries :
     * <ul>
     * <li>'accesses': Isolate ID -&gt; {'node' -&gt; Node Name, 'port' -&gt;
     * Port}</li>
     * <li>'groups': Group Name -&gt; [Isolates IDs]</li>
     * <li>'nodes_host': Node Name -&gt; Host Address</li>
     * </ul>
     * 
     * @return A snapshot of the directory
     */
    Map<String, Object> dump();

    /**
     * Retrieves all known isolates which ID begins with the given prefix. If
     * the prefix is null or empty, returns all known isolates.
     * 
     * Returns null if no isolate matched the prefix.
     * 
     * @param aPrefix
     *            An optional prefix filter
     * @param aIncludeCurrent
     *            If true, include the current isolate in the result
     * @param aOnlyValidated
     *            If true, only return isolates that have been validated
     * @return All known isolates beginning with prefix, or null
     * 
     * @see #validateIsolatePresence(String)
     */
    String[] getAllIsolates(String aPrefix, boolean aIncludeCurrent,
            boolean aOnlyValidated);

    /**
     * Retrieves all known nodes. Returns null if no node is known.
     * 
     * @return All known nodes, or null
     */
    String[] getAllNodes();

    /**
     * Retrieves an Isolate Id -&gt; (host, port) map, containing all known
     * isolates that belong to given group.
     * 
     * @param aGroup
     *            A base group
     * @return An ID -&gt; (host, port) map, null if the group is unknown
     */
    Map<String, HostAccess> getGroupAccesses(EBaseGroup aGroup);

    /**
     * Retrieves an Isolate Id -&gt; (host, port) map, containing all known
     * isolates that belong to given group.
     * 
     * @param aGroupName
     *            A group name
     * @return An ID -&gt; (host, port) map, null if the group is unknown
     */
    Map<String, HostAccess> getGroupAccesses(String aGroupName);

    /**
     * Retrieves the host name to access the node
     * 
     * @param aNodeName
     *            A node name
     * @return A host name or address
     */
    String getHostForNode(String aNodeName);

    /**
     * Retrieves the (host, port) tuple to access the given isolate, or null
     * 
     * @param aIsolateId
     *            The ID of an isolate
     * @return A (host, port) object, or null if the isolate is unknown
     */
    HostAccess getIsolateAccess(String aIsolateId);

    /**
     * Retrieves the current isolate ID
     * 
     * @return The current isolate ID
     */
    String getIsolateId();

    /**
     * Retrieves the node of the given isolate
     * 
     * @param aIsolateId
     *            An isolate ID
     * @return The node hosting the given isolate, or null
     */
    String getIsolateNode(String aIsolateId);

    /**
     * Retrieves the IDs of the isolates on the given node
     * 
     * @param aNodeName
     *            A node name
     * @return The list of isolates on the given node, null if there is no
     *         isolate
     */
    String[] getIsolatesOnNode(String aNodeName);

    /**
     * Retrieves the name of the current node
     * 
     * @return the name of the current node
     */
    String getLocalNode();

    /**
     * Tests if the given isolate ID is registered in the directory
     * 
     * @param aIsolateId
     *            An isolate ID
     * @return True if the ID is known, else false
     */
    boolean isRegistered(String aIsolateId);

    /**
     * Tests if the given isolate ID is registered and has been validated
     * 
     * @param aIsolateId
     *            An isolate ID
     * @return True if the ID is known and validated, else false
     */
    boolean isValidated(String aIsolateId);

    /**
     * Notifies the isolate presence listeners of the validated presence of the
     * given isolate. This method must be called after
     * {@link #validateIsolatePresence(String)}.
     * 
     * @param aIsolateId
     *            The validated isolate ID.
     * @return true on success, false if the isolate wasn't in the validated
     *         state.
     */
    boolean notifyIsolatePresence(String aIsolateId);

    /**
     * Registers an isolate in the directory if it is not yet known
     * 
     * @param aIsolateId
     *            The ID of the isolate to register
     * @param aNode
     *            The node hosting the isolate
     * @param aPort
     *            The port to access the isolate
     * @param aGroups
     *            All groups of the isolate
     * @return True if the isolate has been registered, False on error or if the
     *         isolate was already known for this access.
     * @throws IllegalArgumentException
     *             An argument is invalid
     */
    boolean registerIsolate(String aIsolateId, String aNode, int aPort,
            String... aGroups) throws IllegalArgumentException;

    /**
     * Registers the local isolate in the registry
     * 
     * @param aPort
     *            The port to access the signals
     * @param aGroups
     *            The local isolate groups
     */
    void registerLocal(int aPort, String... aGroups);

    /**
     * Sets up the address to access the given node. Overrides the previous
     * address and returns it.
     * 
     * If the given address is null or empty, only returns the current node
     * address.
     * 
     * @param aNodeName
     *            The node name
     * @param aHostAddress
     *            The address to the node host
     * @return The previous address
     */
    String setNodeAddress(String aNodeName, String aHostAddress);

    /**
     * Stores the content of the given directory dump
     * 
     * If aIgnoredNode is not null, the address corresponding to it in the
     * dumped directory won't be stored.
     * 
     * @param aDumpedDirectory
     *            A directory dump
     * @param aIgnoredNodes
     *            The name of the nodes to ignore
     * @param aIgnoredIds
     *            The isolate IDs to ignore
     * @return The list of the newly registered isolates
     */
    String[] storeDump(Map<?, ?> aDumpedDirectory,
            Collection<String> aIgnoredNodes, Collection<String> aIgnoredIds);

    /**
     * Notifies the directory we are synchronizing with the given isolate. This
     * method must be called when sending or receiving a SYN-ACK signal.
     * 
     * @param aIsolateId
     *            An isolate ID
     * @return True if the state change succeeded
     */
    boolean synchronizingIsolatePresence(String aIsolateId);

    /**
     * Unregisters the given isolate of the directory
     * 
     * @param aIsolateId
     *            The ID of the isolate to unregister
     * @return True if the isolate has been unregistered
     */
    boolean unregisterIsolate(String aIsolateId);

    /**
     * Notifies the directory that an isolate has acknowledged the registration
     * of the current isolate. This method must be called when an ACK signal has
     * been received.
     * 
     * @param aIsolateId
     *            An isolate ID
     * @return True if the state change succeeded
     */
    boolean validateIsolatePresence(String aIsolateId);
}