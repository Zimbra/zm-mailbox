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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.SimpleSessionInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_SESSIONS_RESPONSE)
public class GetSessionsResponse {

    /**
     * @zm-api-field-tag more-flag
     * @zm-api-field-description <b>1 (true)</b> if more sessions left to return
     */
    @XmlAttribute(name=AdminConstants.A_MORE, required=true)
    private final ZmBoolean more;

    /**
     * @zm-api-field-tag total-num-accounts
     * @zm-api-field-description Total number of accounts that matched search (not affected by limit/offset)
     */
    @XmlAttribute(name=AdminConstants.A_TOTAL, required=true)
    private final int total;

    /**
     * @zm-api-field-description Session information
     */
    @XmlElement(name="s", required=false)
    private List<SimpleSessionInfo> sessions = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetSessionsResponse() {
        this(false, -1);
    }

    public GetSessionsResponse(boolean more, int total) {
        this.more = ZmBoolean.fromBool(more);
        this.total = total;
    }

    public void setSessions(Iterable <SimpleSessionInfo> sessions) {
        this.sessions.clear();
        if (sessions != null) {
            Iterables.addAll(this.sessions,sessions);
        }
    }

    public GetSessionsResponse addSession(SimpleSessionInfo session) {
        this.sessions.add(session);
        return this;
    }

    public boolean getMore() { return ZmBoolean.toBool(more); }
    public int getTotal() { return total; }
    public List<SimpleSessionInfo> getSessions() {
        return Collections.unmodifiableList(sessions);
    }
}
