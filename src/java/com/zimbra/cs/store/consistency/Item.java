/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.store.consistency;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Item implements Serializable {

    public final int id;
    public final int mailboxId;
    public final byte type;
    public final byte volumeId;
    public final long size;
    public final String digest;
    public final int mod;
    public final int group;
    public final  List<Revision> revisions = new ArrayList<Revision>();
    private final static long serialVersionUID = 200805081714L;

    public Item(int id, int group, int mailboxId,
            byte type, byte volumeId, long size, String digest, int mod) {
        this.id = id;
        this.group = group;
        this.mailboxId = mailboxId;
        this.type = type;
        this.volumeId = volumeId;
        this.size = size;
        this.digest = digest;
        this.mod = mod;
    }

    public static class Revision implements Serializable, Comparable<Revision> {
        /**
         * Sort descending
         */
        public int compareTo(Revision o) {
            return o.version - version;
        }

        public final int version;
        public final long size;
        public final String digest;
        public final byte volumeId;
        public final int mod;
        private final static long serialVersionUID = 200805081714L;

        public Revision(int version, byte volumeId,
                long size, String digest, int mod) {
            this.version = version;
            this.volumeId = volumeId;
            this.size = size;
            this.digest = digest;
            this.mod = mod;
        }
    }

    public boolean equals(Object other) {
        if (other instanceof Item) {
            return ((Item)other).id == id;
        }
        return false;
    }

    public int hashCode() {
        return id;
    }
    
    @Override
    public String toString() {
        return String.format("Item: %d, Mailbox: %d, Group: %d, Volume: %d",
                id, mailboxId, group, volumeId);
    }
}
