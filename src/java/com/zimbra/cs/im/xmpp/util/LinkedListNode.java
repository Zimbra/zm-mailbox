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

/**
 * Doubly linked node in a LinkedList. Most LinkedList implementations keep the
 * equivalent of this class private. We make it public so that references
 * to each node in the list can be maintained externally.
 * <p/>
 * Exposing this class lets us make remove operations very fast. Remove is
 * built into this class and only requires two reference reassignments. If
 * remove existed in the main LinkedList class, a linear scan would have to
 * be performed to find the correct node to delete.
 * <p/>
 * The linked list implementation was specifically written for the Jive
 * cache system. While it can be used as a general purpose linked list, for
 * most applications, it is more suitable to use the linked list that is part
 * of the Java Collections package.
 *
 * @author Jive Software
 * @see com.zimbra.cs.im.xmpp.util.LinkedList
 */
public class LinkedListNode {

    public LinkedListNode previous;
    public LinkedListNode next;
    public Object object;

    /**
     * This class is further customized for the CoolServlets cache system. It
     * maintains a timestamp of when a Cacheable object was first added to
     * cache. Timestamps are stored as long values and represent the number
     * of milleseconds passed since January 1, 1970 00:00:00.000 GMT.<p>
     * <p/>
     * The creation timestamp is used in the case that the cache has a
     * maximum lifetime set. In that case, when
     * [current time] - [creation time] > [max lifetime], the object will be
     * deleted from cache.
     */
    public long timestamp;

    /**
     * Constructs a new linked list node.
     *
     * @param object   the Object that the node represents.
     * @param next     a reference to the next LinkedListNode in the list.
     * @param previous a reference to the previous LinkedListNode in the list.
     */
    public LinkedListNode(Object object, LinkedListNode next,
                          LinkedListNode previous) {
        this.object = object;
        this.next = next;
        this.previous = previous;
    }

    /**
     * Removes this node from the linked list that it is a part of.
     */
    public void remove() {
        previous.next = next;
        next.previous = previous;
    }

    /**
     * Returns a String representation of the linked list node by calling the
     * toString method of the node's object.
     *
     * @return a String representation of the LinkedListNode.
     */
    public String toString() {
        return object == null ? "null" : object.toString();
    }
}
