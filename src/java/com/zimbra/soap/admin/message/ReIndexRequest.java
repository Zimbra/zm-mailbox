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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ReindexMailboxInfo;

/**
 * @zm-api-command-description ReIndex
 * <br />
 * <b>Access</b>: domain admin sufficient
 * <br />
 * note: this request is by default proxied to the account's home server
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_REINDEX_REQUEST)
public class ReIndexRequest {

    /**
     * @zm-api-field-tag "start|status|cancel"
     * @zm-api-field-description Action to perform
     * <table>
     * <tr> <td> <b>start</b> </td> <td> start reindexing </td> </tr>
     * <tr> <td> <b>status</b> </td> <td> show reindexing progress </td> </tr>
     * <tr> <td> <b>cancel</b> </td> <td> cancel reindexing </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.E_ACTION, required=false)
    private final String action;

    /**
     * @zm-api-field-description Specify reindexing to perform
     * <br />
     * Note: Only one of <b>{ids-comma-sep}</b> and <b>{types-comma-sep}</b> may be specified.
     */
    @XmlElement(name=AdminConstants.E_MAILBOX, required=true)
    private final ReindexMailboxInfo mbox;

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private ReIndexRequest() {
        this((String)null, (ReindexMailboxInfo)null);
    }

    public ReIndexRequest(String action, ReindexMailboxInfo mbox) {
        this.action = action;
        this.mbox = mbox;
    }

    public String getAction() { return action; }
    public ReindexMailboxInfo getMbox() { return mbox; }
}
