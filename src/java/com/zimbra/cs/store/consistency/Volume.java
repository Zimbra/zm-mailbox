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

import java.io.File;

public class Volume implements java.io.Serializable {
    public final byte id;
    public final String path;
    private final short fileBits;
    private final short mailboxBits;
    private final int mailboxGroupBitMask;
    private final int fileGroupBitMask;
    private final static long serialVersionUID = 200805081714L;
    public final boolean compressed;


    public Volume(byte    id,          String path,
                  short   fileBits,    short  fileGroupBits,
                  short   mailboxBits, short  mailboxGroupBits,
                  boolean compressed) {
        this.id               = id;
        this.path             = path;
        this.fileBits         = fileBits;
        this.mailboxBits      = mailboxBits;
        this.compressed       = compressed;

        long mask;
        mask = (long) Math.pow(2, mailboxGroupBits) - 1L;
        mailboxGroupBitMask = (int) mask;

        mask = (long) Math.pow(2, fileGroupBits) - 1L;
        fileGroupBitMask = (int) mask;
    }

    private StringBuilder getItemBase(Item item) {
        StringBuilder sb = new StringBuilder();

        long dir;

        dir = item.mailboxId >> mailboxBits;
        dir &= mailboxGroupBitMask;
        sb.append(path);
        sb.append(File.separator).append(dir);
        sb.append(File.separator).append(item.mailboxId);
        sb.append(File.separator).append("msg");

        dir = item.id >> fileBits;
        dir &= fileGroupBitMask;

        sb.append(File.separator).append(dir);
        sb.append(File.separator);
        sb.append(item.id);
        sb.append("-");
        return sb;
    }
    public File getItemFile(Item item) {
        StringBuilder sb = getItemBase(item);
        sb.append(item.mod);
        sb.append(".msg");

        return new File(sb.toString());
    }

    public File getItemRevisionFile(Item item, Item.Revision revision) {
        StringBuilder sb = getItemBase(item);
        sb.append(revision.mod);
        sb.append(".msg");

        return new File(sb.toString());
    }
}
