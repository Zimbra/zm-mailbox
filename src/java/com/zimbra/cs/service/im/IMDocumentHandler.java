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
package com.zimbra.cs.service.im;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.im.IMRouter;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.BlockingOperation;
import com.zimbra.cs.operation.Requester;
import com.zimbra.cs.operation.Scheduler.Priority;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class IMDocumentHandler extends DocumentHandler {
    
    @Override
    public Object preHandle(Element request, Map<String, Object> context) throws ServiceException { 
        Session session = getSession(context);
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox.OperationContext octxt = zsc.getOperationContext();
        Mailbox mbox = getRequestedMailbox(zsc);
        return BlockingOperation.schedule(request.getName(), session, octxt, mbox, Requester.SOAP, getSchedulerPriority(), 1);   
    }
    
    @Override
    public void postHandle(Object userObj) { 
        ((BlockingOperation)userObj).finish();
    }
    
    protected Priority getSchedulerPriority() {
        return Priority.INTERACTIVE_HIGH;
    }
    

    Object getLock(ZimbraSoapContext zc) throws ServiceException {
        return super.getRequestedMailbox(zc);
    }
    
    static IMPersona getRequestedPersona(ZimbraSoapContext zc, Object lock) throws ServiceException {
        return IMRouter.getInstance().findPersona(zc.getOperationContext(), (Mailbox)lock);
    }
    
    static IMPersona getRequestedPersona(OperationContext oc, Object lock) throws ServiceException {
        return IMRouter.getInstance().findPersona(oc, (Mailbox)lock);
    }
    
}
