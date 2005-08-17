/*
 * Created on Apr 14, 2005
 *
 */
package com.zimbra.cs.object;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface ObjectType {
    
    public String getType();
    
    public String getDescription();
    
    public boolean isIndexingEnabled();
    
    public boolean isStoreMatched();
    
    public String getHandlerClass();
    
    public String getHandlerConfig();

}
