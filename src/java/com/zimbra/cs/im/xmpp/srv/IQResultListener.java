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

import org.xmpp.packet.IQ;

/**
 * An IQResultListener will be invoked when a previously IQ packet sent by the server was answered.
 * Use {@link IQRouter#addIQResultListener(String, IQResultListener)} to add a new listener that
 * will process the answer to the IQ packet being sent. The listener will automatically be
 * removed from the {@link IQRouter} as soon as a reply for the sent IQ packet is received. The
 * reply can be of type RESULT or ERROR.
 *
 * @author Gaston Dombiak
 */
public interface IQResultListener {

    /**
     * Notification method indicating that a previously sent IQ packet has been answered.
     * The received IQ packet might be of type ERROR or RESULT.
     *
     * @param packet the IQ packet answering a previously sent IQ packet.
     */
    void receivedAnswer(IQ packet);
}
