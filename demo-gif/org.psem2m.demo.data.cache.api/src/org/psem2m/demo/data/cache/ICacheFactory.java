/**
 * File:   ICacheFactory.java
 * Author: Thomas Calmant
 * Date:   11 oct. 2011
 */
package org.psem2m.demo.data.cache;

/**
 * Defines a cache channel
 * 
 * @author Thomas Calmant
 */
public interface ICacheFactory {

    /**
     * Closes the channel with the given name
     * 
     * @param aName
     *            Name of the channel
     */
    void closeChannel(String aName);

    /**
     * Opens a standard cache channel. Creates a new one if needed.
     * 
     * Standard and Dequeue are indexed in different maps. Therefore, it is
     * possible to have a standard and a dequeue channel with the same name.
     * 
     * @param aName
     *            Name of the channel
     * @return The cache channel
     */
    <K, V> ICacheChannel<K, V> openChannel(String aName);

    /**
     * Opens a dequeue cache channel. Creates a new one if needed.
     * 
     * Standard and Dequeue are indexed in different maps. Therefore, it is
     * possible to have a standard and a dequeue channel with the same name.
     * 
     * @param aName
     *            Name of the channel
     * @return The cache channel
     */
    <K, V> ICacheDequeueChannel<K, V> openDequeueChannel(String aName);
}