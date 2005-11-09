/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.extension;

import com.zimbra.cs.service.ServiceException;

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
