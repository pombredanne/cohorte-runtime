/**
 * File:   HostAccess.java
 * Author: Thomas Calmant
 * Date:   12 juin 2012
 */
package org.psem2m.signals;

/**
 * A simple class to store a host address/port couple
 * 
 * @author Thomas Calmant
 */
public class HostAccess {

    /** The host address */
    private final String pAddress;

    /** The access port on the host */
    private final int pPort;

    /**
     * Sets up the host access
     * 
     * @param aAddress
     *            An host address (FQDN, IPv4, IPv6)
     * @param aPort
     *            A port on the host
     */
    public HostAccess(final String aAddress, final int aPort) {

        pAddress = aAddress;
        pPort = aPort;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object aObj) {

        if (aObj instanceof HostAccess) {
            final HostAccess other = (HostAccess) aObj;

            // Addresses must be equals
            if (pAddress != null) {
                if (!pAddress.equals(other.pAddress)) {
                    return false;
                }
            } else {
                if (other.pAddress != null) {
                    return false;
                }
            }

            return pPort == other.pPort;
        }

        // Invalid type
        return false;
    }

    /**
     * Retrieves the host address
     * 
     * @return the host address
     */
    public String getAddress() {

        return pAddress;
    }

    /**
     * Retrieves the access port on the host
     * 
     * @return the access port on the host
     */
    public int getPort() {

        return pPort;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        final StringBuilder builder = new StringBuilder();
        builder.append("('");
        builder.append(pAddress);
        builder.append("', ");
        builder.append(pPort);
        builder.append(")");

        return builder.toString();
    }
}
