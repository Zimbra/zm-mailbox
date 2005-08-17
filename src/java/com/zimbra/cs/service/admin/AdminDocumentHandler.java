/*
 * Created on Oct 4, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.soap.DocumentHandler;


/**
 * @author schemers
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class AdminDocumentHandler extends DocumentHandler {

    public boolean needsAuth(Map context) {
        return true;
    }
    
    public boolean needsAdminAuth(Map context) {
        return true;
    }

}
