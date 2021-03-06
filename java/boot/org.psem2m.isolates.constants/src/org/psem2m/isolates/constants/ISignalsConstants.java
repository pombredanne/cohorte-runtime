/**
 * Copyright 2014 isandlaTech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.psem2m.isolates.constants;

/**
 * Cohorte Signals system constants
 *
 * @author Thomas Calmant
 */
public interface ISignalsConstants {

    /**
     * Broadcaster Remote Event notification signal.
     *
     * Associated data: an instance of
     * {@link org.psem2m.isolates.services.remote.beans.RemoteServiceEvent}
     */
    String BROADCASTER_SIGNAL_REMOTE_EVENT = ISignalsConstants.PREFIX_BROADCASTER_SIGNAL_NAME
            + "/remote-event";

    /**
     * Broadcaster end points update request signal.
     *
     * Associated data: none
     */
    String BROADCASTER_SIGNAL_REQUEST_ENDPOINTS = ISignalsConstants.PREFIX_BROADCASTER_SIGNAL_NAME
            + "/request-endpoints";

    /** Signal result code: internal error */
    int CODE_INTERNAL_ERROR = 501;

    /** Signal result code: no listener found */
    int CODE_NO_LISTENER = 404;

    /** Signal result code: no error */
    int CODE_OK = 200;

    /**
     * A forker indicates that it will stop soon
     *
     * Associated data: the UID of the forker and the list of its isolates
     */
    String FORKER_LOST_SIGNAL = ISignalsConstants.PREFIX_FORKER_SIGNAL_NAME
            + "/lost";

    /**
     * A monitor indicates that it lost contact with a forker
     *
     * Associated data: the UID of the forker and the list of its isolates
     */
    String FORKER_STOPPING_SIGNAL = ISignalsConstants.PREFIX_FORKER_SIGNAL_NAME
            + "/stopping";

    /**
     * Lost contact with isolate
     *
     * Associated data: the Isolate ID (CharSequence)
     */
    String ISOLATE_LOST_SIGNAL = "/cohorte/isolate/lost";

    /**
     * The sender is now "ready"
     *
     * Associated data: none
     */
    String ISOLATE_READY_SIGNAL = ISignalsConstants.PREFIX_ISOLATE_SIGNAL_NAME
            + "/ready";

    /**
     * IsolateStatus signal
     *
     * Associated data: an IsolateStatus object
     */
    String ISOLATE_STATUS_SIGNAL = "/psem2m/isolate/status";

    /**
     * Stop isolate signal
     *
     * Associated data: none
     */
    String ISOLATE_STOP_SIGNAL = ISignalsConstants.PREFIX_ISOLATE_SIGNAL_NAME
            + "/stop";

    /**
     * An isolate is stopping
     *
     * Associated data: The UID of the stopping isolate
     */
    String ISOLATE_STOPPING_SIGNAL = ISignalsConstants.PREFIX_ISOLATE_SIGNAL_NAME
            + "/stopping";

    /** Match all sub signals */
    String MATCH_ALL = "/*";

    /**
     * Platform stop signal
     *
     * Associated data: none
     */
    String MONITOR_SIGNAL_STOP_PLATFORM = "/cohorte/platform/stop";

    /** Remote service broadcaster signals names prefix */
    String PREFIX_BROADCASTER_SIGNAL_NAME = "/cohorte/remote-service-broadcaster";

    /** Prefix of any isolate signal name **/
    String PREFIX_FORKER_SIGNAL_NAME = "/cohorte/forker";

    /** Prefix of any isolate signal name **/
    String PREFIX_ISOLATE_SIGNAL_NAME = "/cohorte/isolate";
}
