/**
 * 
 */
package com.zimbra.cs.rmgmt;

import java.io.InputStream;

public interface RemoteBackgroundHandler {

    public void read(InputStream stdout, InputStream stderr);

    public void error(Throwable t);
    
}