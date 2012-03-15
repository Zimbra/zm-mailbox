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
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-description Query WaitSet
 * <br />
 * This API dumps the internal state of all active waitsets.  It is intended for debugging use only and should not be
 * used for production uses.  This API is not guaranteed to be stable between releases in any way and might be
 * removed without warning.
 * <br />
 * <br />
 * <ul>SomeAccountsWaitSet:</ul>
 * <pre>
 * &lt;QueryWaitSetResponse id="WAITSETID"
 *                       defTypes="DEFAULT_TYPES"
 *                       owner="WAITSET_OWNER_ACCOUNT_ID"
 *                       ld="LAST_ACCESS_DATE"
 *                       cbSeqNo="SEQNO_OF_CB"
 *                       currentSeqNo="CURRENT_SEQUENCE_NUMBER">
 *   [&lt;ready accounts="comma-separated list of account IDs"/>]?
 *   [&lt;session types="TYPES" account="ACCOUNT_ID">
 *      [
 *      &lt;WaitSetSession interestMask="BITMASK" highestChangeId="MBOX_CHANGE_ID"
 *                      lastAccessTime="LAST_ACCESS_TIME"
 *                      creationTime="CREATION_TIME"/>
 *      ]?
 *    &lt;/session>]*
 * &lt;/QueryWaitSetResponse>
 * </pre>
 * <ul>AllAccountsWaitSet</ul>
 * <pre>
 * &lt;QueryWaitSetResponse id="WAITSETID"
 *                       defTypes="DEFAULT_TYPES"
 *                       owner="WAITSET_OWNER_ACCOUNT_ID"
 *                       ld="LAST_ACCESS_DATE"
 *                       nextSeqNo="NEXT_SEQNO"
 *                       cbSeqNo="CB_SEQNO"
 *                       currentSeqNo="CURRENT_SEQNO"
 *                       >
 *   [&lt;buffered>
 *      [&lt;commit aid="ACCOUNT_ID" cid="COMMIT_ID"/>]* // only during WS creation before first WaitSetRequest
 *   ]?
 *   [&lt;ready accounts="comma-separated list of account IDs"/>]?
 * &lt;/QueryWaitSetResponse>
 * <pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_QUERY_WAIT_SET_REQUEST)
public class QueryWaitSetRequest {

    /**
     * @zm-api-field-tag waitset-id
     * @zm-api-field-description WaitSet ID
     */
    @XmlAttribute(name=MailConstants.A_WAITSET_ID, required=false)
    private final String waitSetId;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private QueryWaitSetRequest() {
        this((String) null);
    }

    public QueryWaitSetRequest(String waitSetId) {
        this.waitSetId = waitSetId;
    }

    public String getWaitSetId() { return waitSetId; }
}
