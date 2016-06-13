/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
        WaitSetMgr.destroy(zsc, zsc.getRequestedAccountId(), waitSetId);
        
        response.addAttribute(MailConstants.A_WAITSET_ID, waitSetId);
        return response;
    }
}
