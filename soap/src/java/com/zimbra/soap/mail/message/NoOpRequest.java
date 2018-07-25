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

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description A request that does nothing and always returns nothing. Used to keep a session alive,
 * and return any pending notifications.
 * <br />
 * If "wait" is set, and if the current session allows them, this request will block until there are new notifications
 * for the client.  Note that the soap envelope must reference an existing session that has notifications enabled, and
 * the notification sequencing number should be specified.
 * <br />
 * If "wait" is set, the caller can specify whether notifications on delegate sessions will cause the operation to
 * return.  If "delegate" is unset, delegate mailbox notifications will be ignored.  "delegate" is set by default.
 * <br />
 * Some clients (notably browsers) have a global-limit on the number of outstanding sockets...in situations with two
 * App Instances connected to one Zimbra Server, the browser app my appear to 'hang' if two app sessions attempt to do
 * a blocking-NoOp simultaneously.  Since the apps are completely separate in the browser, it is impossible for the
 * apps to coordinate with each other -- therefore the 'limitToOneBlocked' setting is exposed by the server.  If
 * specified, the server will only allow a given user to have one single waiting-NoOp on the server at a time, it will
 * complete (with waitDisallowed set) any existing limited hanging NoOpRequests when a new request comes in.
 * <br />
 * The server may reply with a "waitDisallowed" attribute on response to a request with wait set.  If "waitDisallowed"
 * is set, then blocking-NoOpRequests (ie requests with wait set) are <b>not</b> allowed by the server right now, and
 * the client should stop attempting them.
 * <br />
 * The client may specify a custom timeout-length for their request if they know something about the particular
 * underlying network.  The server may or may not honor this request (depending on server configured max/min values:
 * see LocalConfig variables <b>zimbra_noop_default_timeout, zimbra_noop_min_timeout and zimbra_noop_max_timeout</b>)
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_NO_OP_REQUEST)
public class NoOpRequest {

    /**
     * @zm-api-field-tag wait
     * @zm-api-field-description Wait setting
     */
    @XmlAttribute(name=MailConstants.A_WAIT /* wait */, required=false)
    private ZmBoolean wait;

    /**
     * @zm-api-field-tag delegate
     * @zm-api-field-description If "wait" is set, the caller can use this setting to determine whether notifications
     * on delegate sessions will cause the operation to return.  If "delegate" is unset, delegate mailbox
     * notifications will be ignored.  "delegate" is set by default.
     */
    @XmlAttribute(name=MailConstants.A_DELEGATE /* delegate */, required=false)
    private ZmBoolean includeDelegates;

    /**
     * @zm-api-field-tag enforce-limit
     * @zm-api-field-description If specified, the server will only allow a given user to have one single
     * waiting-NoOp on the server at a time, it will complete (with waitDisallowed set) any existing limited hanging
     * NoOpRequests when a new request comes in.
     */
    @XmlAttribute(name=MailConstants.A_LIMIT_TO_ONE_BLOCKED /* limitToOneBlocked */, required=false)
    private ZmBoolean enforceLimit;

    /**
     * @zm-api-field-tag timeout-millis-to-wait
     * @zm-api-field-description The client may specify a custom timeout-length for their request if they know
     * something about the particular underlying network.
     * The server may or may not honor this request (depending on server configured max/min values: see LocalConfig
     * variables <b>zimbra_noop_default_timeout, zimbra_noop_min_timeout and zimbra_noop_max_timeout</b>)
     */
    @XmlAttribute(name=MailConstants.A_TIMEOUT /* timeout */, required=false)
    private Long timeout;

    public NoOpRequest() {
    }

    public static NoOpRequest createWithWaitAndTimeout(boolean waitVal, long timeoutVal) {
        NoOpRequest req = new NoOpRequest();
        req.setWait(waitVal);
        req.setTimeout(timeoutVal);
        return req;
    }

    public void setWait(Boolean wait) { this.wait = ZmBoolean.fromBool(wait); }
    public void setIncludeDelegates(Boolean includeDelegates) { this.includeDelegates = ZmBoolean.fromBool(includeDelegates); }
    public void setEnforceLimit(Boolean enforceLimit) { this.enforceLimit = ZmBoolean.fromBool(enforceLimit); }
    public void setTimeout(Long timeout) { this.timeout = timeout; }
    public Boolean getWait() { return ZmBoolean.toBool(wait); }
    public Boolean getIncludeDelegates() { return ZmBoolean.toBool(includeDelegates); }
    public Boolean getEnforceLimit() { return ZmBoolean.toBool(enforceLimit); }
    public Long getTimeout() { return timeout; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("wait", wait)
            .add("includeDelegates", includeDelegates)
            .add("enforceLimit", enforceLimit)
            .add("timeout", timeout);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
