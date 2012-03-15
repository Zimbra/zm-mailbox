/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class MailboxStats {

    /**
     * @zm-api-field-tag num-mailboxes
     * @zm-api-field-description Total number of mailboxes
     */
    @XmlAttribute(name=AdminConstants.A_NUM_MBOXES /* numMboxes */, required=true)
    private final long numMboxes;

    /**
     * @zm-api-field-tag total-size
     * @zm-api-field-description Total size of all mailboxes
     */
    @XmlAttribute(name=AdminConstants.A_TOTAL_SIZE /* totalSize */, required=true)
    private final long totalSize;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MailboxStats() {
        this(0L, 0L);
    }

    public MailboxStats(long numMboxes, long totalSize) {
        this.numMboxes = numMboxes;
        this.totalSize = totalSize;
    }

    public long getNumMboxes() { return numMboxes; }
    public long getTotalSize() { return totalSize; }
}
