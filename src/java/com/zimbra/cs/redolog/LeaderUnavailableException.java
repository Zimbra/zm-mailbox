package com.zimbra.cs.redolog;

import java.io.IOException;

/**
 * Exception thrown when there is no leader available to handle a request
 *
 */
public class LeaderUnavailableException extends IOException {

    public LeaderUnavailableException(IOException ioe) {
        super(ioe);
    }

    private static final long serialVersionUID = 7029345556025681264L;

}
