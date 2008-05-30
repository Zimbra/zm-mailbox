/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.session.WaitSetMgr;
import com.zimbra.soap.ZimbraSoapContext;

/*
  *************************************
  DestroyWaitSet: Use this to close out the wait set.  Note that the
  server will automatically time out a wait set if there is no reference
  to it for (default of) 10 minutes.
  *************************************
  <DestroyWaitSetRequest waitSet="setId"/>

  <DestroyWaitSetResponse waitSet="setId"/>
*/

/**
 * 
 */
public class DestroyWaitSet extends MailDocumentHandler {

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(com.zimbra.common.soap.Element, java.util.Map)
     */
    
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Element response = zsc.createElement(MailConstants.DESTROY_WAIT_SET_RESPONSE);
        return staticHandle(request, context, response);
    }
    
    static public Element staticHandle(Element request, Map<String, Object> context, Element response) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        String waitSetId = request.getAttribute(MailConstants.A_WAITSET_ID);
        WaitSetMgr.destroy(zsc.getAuthtokenAccountId(), waitSetId);
        
        response.addAttribute(MailConstants.A_WAITSET_ID, waitSetId);
        return response;
    }
}
