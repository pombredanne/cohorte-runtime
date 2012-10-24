/**
 * File:   SignalData.java
 * Author: Thomas Calmant
 * Date:   12 juin 2012
 */
package org.psem2m.signals;

/**
 * Basic implementation of a signal data bean
 * 
 * @author Thomas Calmant
 */
public class SignalData implements ISignalData {

    /** The source isolate ID */
    private String pIsolate;

    /** The source isolate node */
    private String pNode;

    /** The sender host/address */
    private String pSender;

    /** The data associated to the signal */
    private Object pSignalData;

    /** Signal time stamp */
    private long pTimestamp;

    /**
     * Default constructor (sets up the time stamp to now)
     */
    public SignalData() {

        pTimestamp = System.currentTimeMillis();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.signals.ISignalData#getSenderAddress()
     */
    @Override
    public String getSenderAddress() {

        return pSender;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.signals.ISignalData#getSenderId()
     */
    @Override
    public String getSenderId() {

        return pIsolate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.signals.ISignalData#getSenderNode()
     */
    @Override
    public String getSenderNode() {

        return pNode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.signals.impl.ISignalData#getSignalContent()
     */
    @Override
    public Object getSignalContent() {

        return pSignalData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.psem2m.signals.impl.ISignalData#getTimestamp()
     */
    @Override
    public long getTimestamp() {

        return pTimestamp;
    }

    /**
     * Sets up the sender address
     * 
     * @param aSender
     *            the sender address
     */
    public void setSenderAddress(final String aSender) {

        pSender = aSender;
    }

    /**
     * Sets up the sender ID
     * 
     * @param aIsolateId
     *            An isolate Id
     */
    public void setSenderId(final String aIsolateId) {

        pIsolate = aIsolateId;
    }

    /**
     * Sets up the sending node
     * 
     * @param aNode
     *            An isolate node
     */
    public void setSenderNode(final String aNode) {

        pNode = aNode;
    }

    /**
     * Sets up the signal content
     * 
     * @param aSignalData
     *            the signal content
     */
    public void setSignalContent(final Object aSignalData) {

        pSignalData = aSignalData;
    }

    /**
     * Sets up the signal time stamp
     * 
     * @param aTimestamp
     *            the signal time stamp
     */
    public void setTimestamp(final long aTimestamp) {

        pTimestamp = aTimestamp;
    }
}