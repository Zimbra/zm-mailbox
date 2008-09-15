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

public class ItemFault implements java.io.Serializable {
    public final Item item;
    public final Item faultItem;
    public final Item.Revision faultRevision;
    public final Code faultCode;
    public final byte volumeId;
    public final long size;
    public final File faultFile;
    
    private final static long serialVersionUID = 200805091631L;

    public ItemFault(Item item,
            Item faultItem, Item.Revision faultRevision, Code faultCode,
            byte volumeId, long size, File faultFile) {
        this.item = item;
        this.faultItem = faultItem;
        this.faultRevision = faultRevision;
        this.faultCode = faultCode;
        this.faultFile = faultFile;
        this.volumeId = volumeId;
        this.size = size;
    }

    public static enum Code { NOT_FOUND, WRONG_VOLUME, WRONG_SIZE, NO_METADATA,
        GZIP_CORRUPT, IO_EXCEPTION }
}
