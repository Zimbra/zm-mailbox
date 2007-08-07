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
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;
import org.mortbay.util.ajax.WaitingContinuation;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * Do nothing. The main intent of this Soap call is for the client
 * to fetch new notifications (which are automatically added onto the empty 
 * response). 
 * 
 * The caller may set wait=1 for this request in which case the request will
 * block until there are new notifications.
 */
public class NoOp extends MailDocumentHandler  {
    
    private static final long DEFAULT_TIMEOUT;
    private static final long MIN_TIMEOUT;
    private static final long MAX_TIMEOUT;
    
    static {
        DEFAULT_TIMEOUT = LC.zimbra_noop_default_timeout.longValue() * 1000;
        MIN_TIMEOUT = LC.zimbra_noop_min_timeout.longValue() * 1000;
        MAX_TIMEOUT = LC.zimbra_noop_max_timeout.longValue() * 1000;
    }
    
    private static long parseTimeout(Element request) throws ServiceException {
        long timeout = request.getAttributeLong(MailConstants.A_TIMEOUT, DEFAULT_TIMEOUT);
        if (timeout < MIN_TIMEOUT)
            timeout = MIN_TIMEOUT;
        if (timeout > MAX_TIMEOUT)
            timeout = MAX_TIMEOUT;
        return timeout;
    }
    
    ConcurrentHashMap<String /*AccountId*/, ZimbraSoapContext> sBlockedNops = 
        new ConcurrentHashMap<String /*AccountId*/, ZimbraSoapContext>(5000, 0.75f, 50);
    
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
	    ZimbraSoapContext zsc = getZimbraSoapContext(context);
        boolean wait = request.getAttributeBool(MailConstants.A_WAIT, false);
        HttpServletRequest servletRequest = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
        
        boolean blockingUnsupported = false;
        
        // See bug 16494 - if a session is new, we should return from the NoOp immediately so the client
        // gets the <refresh> block 
        if (zsc.hasCreatedSession())
            wait = false;
        
        if (wait) {
            if (!context.containsKey(SoapServlet.IS_RESUMED_REQUEST)) {
                Continuation continuation = ContinuationSupport.getContinuation(servletRequest, zsc);
                
                servletRequest.setAttribute("nop_origcontext", zsc);
                
                // NOT a resumed request -- block if necessary
                if (zsc.beginWaitForNotifications(continuation)) {
                    ZimbraSoapContext otherContext = sBlockedNops.put(zsc.getAuthtokenAccountId(), zsc);
                    if (otherContext!= null) {
                        otherContext.signalNotification(true);
                    }
                    
                    synchronized(zsc) {
                        if (zsc.waitingForNotifications()) {
                            assert (!(continuation instanceof WaitingContinuation) || ((WaitingContinuation)continuation).getMutex()==zsc); 
                            continuation.suspend(parseTimeout(request));
                        }
                        assert(continuation instanceof WaitingContinuation); // this part of code only reached if we're using WaitingContinuations
                        
                        if (zsc.isCanceledWaitForNotifications())
                            blockingUnsupported = true;
                    }
                }
            } else {
                ZimbraSoapContext origContext = (ZimbraSoapContext)(servletRequest.getAttribute("nop_origcontext"));
                if (origContext.isCanceledWaitForNotifications())
                    blockingUnsupported = true;
            }
            //
            // at this point, we know we're done waiting -- we either blocked on a BlockingContinuation, or
            // we've resumed a RetryContinuation...either way our wait is up, time to execute.
            //
//            blockingUnsupported = zsc.isCanceledWaitForNotifications();
            
            // remove this soap context from the blocked-conext hash, but only
            // if it hasn't already been removed by someone else...
            sBlockedNops.remove(zsc.getAuthtokenAccountId(), zsc);
        }
        Element toRet = zsc.createElement(MailConstants.NO_OP_RESPONSE);
        if (blockingUnsupported) {
            toRet.addAttribute(MailConstants.A_WAIT_DISALLOWED, true);
        }
        
        return toRet;
	}
}
