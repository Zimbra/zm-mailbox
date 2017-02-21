/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
