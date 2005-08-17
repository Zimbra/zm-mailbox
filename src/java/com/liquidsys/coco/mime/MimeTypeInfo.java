/*
 * Created on Apr 14, 2005
 *
 */
package com.liquidsys.coco.mime;

/**
 * @author kchen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface MimeTypeInfo {
    
    public String getType();
    
    public String getHandlerClass();
    
    public boolean isIndexingEnabled();
    
    public String getDescription();
    
    public String[] getFileExtensions();

}
