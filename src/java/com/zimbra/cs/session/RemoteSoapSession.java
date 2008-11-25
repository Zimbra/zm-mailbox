/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.session;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.ZimbraSoapContext;

public class RemoteSoapSession extends SoapSession {
    /** Creates a <tt>SoapSession</tt> owned by the given account homed on
     *  a different server.  It thus cannot listen on its own {@link Mailbox}.
     * @see Session#register() */
    public RemoteSoapSession(String authenticatedId) {
        super(authenticatedId);
    }

    @Override protected boolean isMailboxListener() {
        return false;
    }

    @Override protected boolean isIMListener() {
        return false;
    }

    @Override public String getRemoteSessionId(Server server) {
        return null;
    }

    @Override public void putRefresh(Element ctxt, ZimbraSoapContext zsc) {
        ctxt.addUniqueElement(ZimbraNamespace.E_REFRESH);
        return;
    }

    @Override public Element putNotifications(Element ctxt, ZimbraSoapContext zsc, int lastSequence) {
        if (ctxt == null)
            return null;

        QueuedNotifications ntfn;
        synchronized (mSentChanges) {
            if (!mChanges.hasNotifications())
                return null;
            ntfn = mChanges;
            mChanges = new QueuedNotifications(ntfn.getSequence() + 1);
        }

        putQueuedNotifications(null, ntfn, ctxt, zsc);
        return ctxt;
    }
}
