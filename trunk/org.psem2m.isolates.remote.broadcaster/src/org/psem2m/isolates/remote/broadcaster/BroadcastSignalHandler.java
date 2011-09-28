/**
 * File:   BroadcastSignalHandler.java
 * Author: Thomas Calmant
 * Date:   21 sept. 2011
 */
package org.psem2m.isolates.remote.broadcaster;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogService;
import org.psem2m.isolates.base.activators.CPojoBase;
import org.psem2m.isolates.constants.ISignalsConstants;
import org.psem2m.isolates.services.remote.IRemoteServiceEventListener;
import org.psem2m.isolates.services.remote.IRemoteServiceRepository;
import org.psem2m.isolates.services.remote.beans.EndpointDescription;
import org.psem2m.isolates.services.remote.beans.RemoteServiceEvent;
import org.psem2m.isolates.services.remote.beans.RemoteServiceEvent.ServiceEventType;
import org.psem2m.isolates.services.remote.beans.RemoteServiceRegistration;
import org.psem2m.isolates.services.remote.signals.ISignalData;
import org.psem2m.isolates.services.remote.signals.ISignalBroadcaster;
import org.psem2m.isolates.services.remote.signals.ISignalListener;
import org.psem2m.isolates.services.remote.signals.ISignalReceiver;

/**
 * Broadcast signals listener
 * 
 * @author Thomas Calmant
 */
@Component(name = "remote-rsb-signal-handler-factory", publicFactory = false)
@Instantiate(name = "remote-rsb-signal-handler")
public class BroadcastSignalHandler extends CPojoBase implements
        ISignalListener {

    /** Log service, injected by iPOJO */
    @Requires
    private LogService pLogger;

    /** Remote service events listeners */
    @Requires
    private IRemoteServiceEventListener[] pRemoteEventsListeners;

    /** Remote Service Repository (RSR), injected by iPOJO */
    @Requires
    private IRemoteServiceRepository pRepository;

    /** Signal sender, injected by iPOJO */
    @Requires
    private ISignalBroadcaster pSignalEmitter;

    /** Signal receiver, injected by iPOJO */
    @Requires
    private ISignalReceiver pSignalReceiver;

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.utilities.CXObjectBase#destroy()
     */
    @Override
    public void destroy() {

        // ...
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.services.remote.signals.ISignalListener#
     * handleReceivedSignal(java.lang.String,
     * org.psem2m.isolates.services.remote.signals.ISignalData)
     */
    @Override
    public void handleReceivedSignal(final String aSignalName,
            final ISignalData aSignalData) {

        final Object signalContent = aSignalData.getSignalContent();

        if (ISignalsConstants.BROADCASTER_SIGNAL_REMOTE_EVENT
                .equals(aSignalName)) {
            // Remote event notification

            if (signalContent instanceof RemoteServiceEvent) {
                // Valid signal content, handle it
                final RemoteServiceEvent event = (RemoteServiceEvent) signalContent;
                event.setSenderHostName(aSignalData.getSenderHostName());

                handleRemoteEvent(event);
            }

        } else if (ISignalsConstants.BROADCASTER_SIGNAL_REQUEST_ENDPOINTS
                .equals(aSignalName)) {
            // End point request
            handleRequestEndpoints(aSignalData.getIsolateSender());
        }

    }

    /**
     * Notifies all listeners that a remote event occurred
     * 
     * @param aEvent
     *            A remote service event
     */
    protected void handleRemoteEvent(final RemoteServiceEvent aEvent) {

        // Notify all listeners
        for (IRemoteServiceEventListener listener : pRemoteEventsListeners) {
            listener.handleRemoteEvent(aEvent);
        }
    }

    /**
     * Handles end points listing request.
     * 
     * Uses the Signal sender to tell the requesting isolate which local end
     * points are stored in the RSR. Does nothing if the RSR is empty.
     * 
     * @param aRequestingIsolateId
     *            Isolate to answer to.
     */
    protected void handleRequestEndpoints(final String aRequestingIsolateId) {

        final RemoteServiceRegistration[] localRegistrations = pRepository
                .getLocalRegistrations();
        if (localRegistrations == null || localRegistrations.length == 0) {
            // Don't say anything if we have anything to say...
            pLogger.log(LogService.LOG_INFO,
                    "RequestEndpoints received, but the RSR is empty");
            return;
        }

        // Prepare the event list
        final List<RemoteServiceEvent> events = new ArrayList<RemoteServiceEvent>(
                localRegistrations.length);

        // For each exported interface, create an event
        for (RemoteServiceRegistration registration : localRegistrations) {

            final EndpointDescription[] endpoints = registration.getEndpoints();
            if (endpoints == null || endpoints.length == 0) {
                // Ignore empty services
                continue;
            }

            // Create the corresponding event
            final RemoteServiceEvent remoteEvent = new RemoteServiceEvent(
                    ServiceEventType.REGISTERED, registration);

            events.add(remoteEvent);
        }

        // Use the signal sender to reply to the isolate
        pSignalEmitter.sendData(aRequestingIsolateId,
                ISignalsConstants.BROADCASTER_SIGNAL_REMOTE_EVENT,
                events.toArray(new RemoteServiceEvent[0]));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.base.activators.CPojoBase#invalidatePojo()
     */
    @Override
    @Invalidate
    public void invalidatePojo() throws BundleException {

        // Unregister the listener
        pSignalReceiver.unregisterListener(ISignalsConstants.MATCH_ALL, this);
        pLogger.log(LogService.LOG_INFO, "RSB Handler Gone");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.isolates.base.activators.CPojoBase#validatePojo()
     */
    @Override
    @Validate
    public void validatePojo() throws BundleException {

        // Register to all broadcast signals
        pSignalReceiver.registerListener(
                ISignalsConstants.BROADCASTER_SIGNAL_NAME_PREFIX
                        + ISignalsConstants.MATCH_ALL, this);

        pLogger.log(LogService.LOG_INFO, "RSB Handler Ready");
    }
}