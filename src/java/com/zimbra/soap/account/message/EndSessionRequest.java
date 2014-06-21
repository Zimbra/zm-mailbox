/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014 Zimbra, Inc.
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

package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description End the current session, removing it from all caches.  Called when the browser app
 * (or other session-using app) shuts down.  Has no effect if called in a &lt;nosession> context.
 */

@XmlRootElement(name=AccountConstants.E_END_SESSION_REQUEST)
public class EndSessionRequest {
    /**
     * @zm-api-field-tag need-can-expand
     * @zm-api-field-description flag whether the <b>{exp}</b> flag is needed in the response for group entries.<br />
     *     default is 0 (false)
     */
    @XmlAttribute(name=AccountConstants.A_LOG_OFF /* logoff */, required=false)
    private ZmBoolean logoff;
    
    public void setLogOff (boolean logoff) {this.logoff = ZmBoolean.fromBool(logoff);} 
}
