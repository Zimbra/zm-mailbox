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
 * Part of the Zimbra Collaboration Suite Server.
 *
 * The Original Code is Copyright (C) Jive Software. Used with permission
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv.audit;

import com.zimbra.cs.im.xmpp.srv.StreamID;
import com.zimbra.cs.im.xmpp.srv.StreamIDFactory;
import com.zimbra.cs.im.xmpp.srv.spi.BasicStreamIDFactory;

/**
 * Factory for producing audit stream IDs. We use a factory so that
 * audit information can be identified using an appropriate storage
 * key (typically a long for RDBMS).
 *
 * @author Iain Shigeoka
 */
public class AuditStreamIDFactory implements StreamIDFactory {

    private BasicStreamIDFactory factory = new BasicStreamIDFactory();

    public AuditStreamIDFactory() {
    }

    public StreamID createStreamID() {
        return factory.createStreamID();
    }
}
