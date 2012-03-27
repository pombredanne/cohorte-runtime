/**
 * File:   IForker.java
 * Author: Thomas Calmant
 * Date:   17 juin 2011
 */
package org.psem2m.isolates.services.forker;

import java.util.Map;

/**
 * Description of the Forker service
 * 
 * @author Thomas Calmant
 */
public interface IForker {

    /** Isolate is alive */
    int ALIVE = 0;

    /** The isolate is already running */
    int ALREADY_RUNNING = 1;

    /** Process is dead (not running) */
    int DEAD = 1;

    /** No reference to the isolate process, unknown state */
    int NO_PROCESS_REF = 2;

    /** No isolate watcher could be started (active isolate waiter) */
    int NO_WATCHER = 3;

    /** An error occurred calling the runner */
    int RUNNER_EXCEPTION = 4;

    /** Process is stuck (running, but not responding) */
    int STUCK = 2;

    /** Successful operation */
    int SUCCESS = 0;

    /** Unknown kind of isolate */
    int UNKNOWN_KIND = 5;

    /**
     * Retrieves the name of the host machine of this forker
     * 
     * @return The name of the forker host
     */
    String getHostName();

    /**
     * Tests the given isolate state
     * 
     * @param aIsolateId
     *            The isolate ID
     * @return The isolate process state
     */
    int ping(String aIsolateId);

    /**
     * Starts a process according to the given configuration
     * 
     * @param aIsolateConfiguration
     *            The configuration of the isolate to start
     * 
     * @return The starter error code
     */
    int startIsolate(Map<String, Object> aIsolateConfiguration);

    /**
     * Kills the process with the given isolate ID
     * 
     * @param aIsolateId
     *            The ID of the isolate to kill
     */
    void stopIsolate(String aIsolateId);
}
