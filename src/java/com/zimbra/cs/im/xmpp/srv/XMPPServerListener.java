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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv;

/**
 * Interface that let observers be notified when the server has been started or is
 * about to be stopped. Use {@link XMPPServer#addServerListener(XMPPServerListener)} to
 * add new listeners.
 *
 * @author Gaston Dombiak
 */
public interface XMPPServerListener {

    /**
     * Notification message indicating that the server has been started. At this point
     * all server modules have been initialized and started. Message sending and receiving
     * is now possible. However, some plugins may still be pending to be loaded.
     */
    void serverStarted();

    /**
     * Notification message indication that the server is about to be stopped. At this point
     * all modules are still running so all services are still available.
     */
    void serverStopping();
}
