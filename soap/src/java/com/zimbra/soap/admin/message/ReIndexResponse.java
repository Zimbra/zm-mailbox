/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ReindexProgressInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_REINDEX_RESPONSE)
public class ReIndexResponse {

    /**
     * @zm-api-field-tag status
     * @zm-api-field-description Status - one of
     *                           <b>started|running|cancelled|idle</b>
     */
    @XmlAttribute(name = AdminConstants.A_STATUS, required = true)
    private final String status;

    /**
     * @zm-api-field-description Aggregate re-indexing progress for legacy SOAP clients
     */
    @XmlElement(name = AdminConstants.E_PROGRESS, required = false)
    private final ReindexProgressInfo progress;

    /**
     * @zm-api-field-tag mbox
     * @zm-api-field-description Per mailbox re-indexing progress
     */
    @XmlElement(name = AdminConstants.E_MAILBOX, required = false)
    private List<ReindexProgressInfo> mbox;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ReIndexResponse() {
        this((String) null, (ReindexProgressInfo) null);
    }

    public ReIndexResponse(String status) {
        this(status, (ReindexProgressInfo) null);
    }

    public ReIndexResponse(String status, ReindexProgressInfo progress) {
        this.status = status;
        this.progress = progress;
        this.mbox = new ArrayList<ReindexProgressInfo>();
    }

    public ReIndexResponse(String status, ReindexProgressInfo progress, List<ReindexProgressInfo> mailboxes) {
        this.status = status;
        this.progress = progress;
        this.mbox = mailboxes;
    }

    public String getStatus() {
        return status;
    }

    public ReindexProgressInfo getProgress() {
        return progress;
    }

    public void addMbox(ReindexProgressInfo mailboxProgress) {
        this.mbox.add(mailboxProgress);
    }

    public void setMbox(Iterable<ReindexProgressInfo> mailboxes) {
        this.mbox.clear();
        if(mailboxes != null) {
            if(mailboxes != null) {
                Iterables.addAll(this.mbox, mailboxes);
            }
        }
    }

    public List<ReindexProgressInfo> getMbox() {
        return this.mbox;
    }
}
