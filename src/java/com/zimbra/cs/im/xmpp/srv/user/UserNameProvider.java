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

package com.zimbra.cs.im.xmpp.srv.user;

import org.xmpp.packet.JID;

/**
 * Interface to be implemented by components that are capable of returning the name of entities
 * when running as internal components.
 *
 * @author Gaston Dombiak
 */
public interface UserNameProvider {

    /**
     * Returns the name of the entity specified by the following JID.
     *
     * @param entity JID of the entity to return its name.
     * @return the name of the entity specified by the following JID.
     */
    abstract String getUserName(JID entity);
}
