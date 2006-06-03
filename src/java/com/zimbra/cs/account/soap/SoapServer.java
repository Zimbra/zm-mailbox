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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.soap;

import java.util.Map;

import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;

public class SoapServer extends SoapNamedEntry implements Server {

    public SoapServer(String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs);
    }

    public SoapServer(Element e) throws ServiceException {
        super(e);
    }

    @Override
    public void modifyAttrs(Map<String, ? extends Object> attrs) throws ServiceException {
        // TODO Auto-generated method stub
    }

    @Override
    public void modifyAttrs(Map<String, ? extends Object> attrs, boolean checkImmutable) throws ServiceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void reload() throws ServiceException {
        // TODO Auto-generated method stub
        
    }

    public Map<String, Object> getAttrs(boolean applyConfig) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }
}
