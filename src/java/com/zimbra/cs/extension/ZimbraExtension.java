/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.extension;

import com.zimbra.common.service.ServiceException;

/**
 * Zimbra extension. An extension to the Zimbra server is packaged as a jar
 * file with its manifest containing the header:
 * 
 *   Zimbra-Extension-Class: <name of implementation class of this interface>
 * 
 * The extension is deployed by dropping the jar file into the 
 * <zimbra_home>/lib/ext/<ext> directory. It is loaded upon server startup.
 */
public interface ZimbraExtension {
    
    /**
     * Defines a name for the extension. It must be an identifier.
     * @return
     */
    public String getName();
    
    /**
     * Initializes the extension. Called when the extension is loaded.
     * 
     * @throws ServiceException
     */
	public void init() throws ServiceException;
	
    /**
     * Terminates the extension. Called when the server is shut down.
     *
     */
	public void destroy();
}
