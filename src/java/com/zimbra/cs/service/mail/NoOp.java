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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;
import org.mortbay.util.ajax.WaitingContinuation;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class NoOp extends MailDocumentHandler  {
    
    private static final long NOP_TIMEOUT = 5 * 60 * 1000; // 5 minutes per Zimbra Desktop (NAT issues), TODO perhaps allow client to specify a timeout?

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        boolean wait = request.getAttributeBool("wait", false);
        if (wait) {
            HttpServletRequest servletRequest = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
            Continuation continuation = ContinuationSupport.getContinuation(servletRequest, zsc);
            if (!continuation.isResumed()) {
                if (continuation instanceof WaitingContinuation && (((WaitingContinuation)continuation).getMutex() != zsc)) {
                    // workaround for a Jetty bug: Jetty currently (in bio mode) re-uses the Continuation object, but doesn't
                    // clear the mutex...and unfortunately there's no way to re-set the mutex once it is already set.  Fortunately,
                    // in blocking mode it doesn't matter if we create a brand-new Continuation here since the Continuation
                    // object is just a wrapper around normal java wait/notify.
                    continuation = new WaitingContinuation(zsc);
                }
                if (zsc.beginWaitForNotifications(continuation)) {
                    synchronized(zsc) {
                        if (zsc.waitingForNotifications()) {
                            assert (!(continuation instanceof WaitingContinuation) || ((WaitingContinuation)continuation).getMutex()==zsc); 
                            continuation.suspend(NOP_TIMEOUT);
                        }
                    }
                }
            }
        }
        return zsc.createElement(MailConstants.NO_OP_RESPONSE);
	}
}
