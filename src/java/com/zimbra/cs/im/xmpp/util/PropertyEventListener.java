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

package com.zimbra.cs.im.xmpp.util;

import java.util.Map;

/**
 * Interface to listen for property events. Use the
 * {@link com.zimbra.cs.im.xmpp.util.PropertyEventDispatcher#addListener(PropertyEventListener)}
 * method to register for events.
 *
 * @author Matt Tucker
 */
public interface PropertyEventListener {

    /**
     * A property was set.
     *
     * @param property the property.
     * @param params event parameters.
     */
    public void propertySet(String property, Map params);

    /**
     * A property was deleted.
     *
     * @param property the deleted.
     * @param params event parameters.
     */
    public void propertyDeleted(String property, Map params);

    /**
     * An XML property was set.
     *
     * @param property the property.
     * @param params event parameters.
     */
    public void xmlPropertySet(String property, Map params);

    /**
     * An XML property was deleted.
     *
     * @param property the property.
     * @param params event parameters.
     */
    public void xmlPropertyDeleted(String property, Map params);

}