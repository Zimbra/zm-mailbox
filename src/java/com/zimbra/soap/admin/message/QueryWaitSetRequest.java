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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
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
 * </pre>
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
