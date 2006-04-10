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

package com.zimbra.cs.im.xmpp.srv.disco;

/**
 * An item is associated with an XMPP Entity, usually thought of a children of the parent
 * entity and normally are addressable as a JID.<p>
 * <p/>
 * An item associated with an entity may not be addressable as a JID. In order to handle
 * such items, Service Discovery uses an optional 'node' attribute that supplements the
 * 'jid' attribute.
 *
 * @author Gaston Dombiak
 */
public interface DiscoItem {

    /**
     * Returns the entity's ID.
     *
     * @return the entity's ID.
     */
    public abstract String getJID();

    /**
     * Returns the node attribute that supplements the 'jid' attribute. A node is merely
     * something that is associated with a JID and for which the JID can provide information.<p>
     * <p/>
     * Node attributes SHOULD be used only when trying to provide or query information which
     * is not directly addressable.
     *
     * @return the node attribute that supplements the 'jid' attribute
     */
    public abstract String getNode();

    /**
     * Returns the entity's name. The entity's name specifies in natural-language the name for the
     * item.
     *
     * @return the entity's name.
     */
    public abstract String getName();

    /**
     * Returns the action (i.e. update or remove) that indicates what must be done with this item or
     * null if none. An "update" action requests the server to create or update the item. Whilst a
     * "remove" action requests to remove the item.
     *
     * @return the action (i.e. update or remove) that indicates what must be done with this item or
     *         null if none.
     */
    public abstract String getAction();
}
