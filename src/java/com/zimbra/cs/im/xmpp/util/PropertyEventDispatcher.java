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

import com.zimbra.cs.im.xmpp.util.Log;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dispatches property events. Each event has a {@link EventType type}
 * and optional parameters, as follows:<p>
 *
 * <table border="1">
 * <tr><th>Event Type</th><th>Extra Params</th></tr>
 * <tr><td>{@link EventType#property_set property_set}</td><td>A param named <tt>value</tt> that
 *      has the value of the property set.</td></tr>
 * <tr><td>{@link EventType#property_deleted property_deleted}</td><td><i>None</i></td></tr>
 * <tr><td>{@link EventType#xml_property_set xml_property_set}</td><td>A param named <tt>value</tt> that
 *      has the value of the property set.</td></tr>
 * <tr><td>{@link EventType#xml_property_deleted xml_property_deleted}</td><td><i>None</i></td></tr>
 * </table>
 *
 * @author Matt Tucker
 */
public class PropertyEventDispatcher {

    private static List<PropertyEventListener> listeners =
            new CopyOnWriteArrayList<PropertyEventListener>();

    private PropertyEventDispatcher() {
        // Not instantiable.
    }

    /**
     * Registers a listener to receive events.
     *
     * @param listener the listener.
     */
    public static void addListener(PropertyEventListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        listeners.add(listener);
    }

    /**
     * Unregisters a listener to receive events.
     *
     * @param listener the listener.
     */
    public static void removeListener(PropertyEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Dispatches an event to all listeners.
     *
     * @param property the property.
     * @param eventType the event type.
     * @param params event parameters.
     */
    public static void dispatchEvent(String property, EventType eventType, Map params) {
        for (PropertyEventListener listener : listeners) {
            try {
                switch (eventType) {
                    case property_set: {
                        listener.propertySet(property, params);
                        break;
                    }
                    case property_deleted: {
                        listener.propertyDeleted(property, params);
                        break;
                    }
                    case xml_property_set: {
                        listener.xmlPropertySet(property, params);
                        break;
                    }
                    case xml_property_deleted: {
                        listener.xmlPropertyDeleted(property, params);
                        break;
                    }
                    default:
                        break;
                }
            }
            catch (Exception e) {
                Log.error(e);
            }
        }
    }

    /**
     * Represents valid event types.
     */
    public enum EventType {

        /**
         * A property was set.
         */
        property_set,

        /**
         * A property was deleted.
         */
        property_deleted,

        /**
         * An XML property was set.
         */
        xml_property_set,

        /**
         * An XML property was deleted.
         */
        xml_property_deleted;
    }
}