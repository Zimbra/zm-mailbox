/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.account.message;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name= AccountConstants.E_SEND_TFA_CODE_REQUEST)
public class SendTFACodeRequest {

    public SendTFACodeRequest() {

    }

    public SendTFACodeRequest(SendTFACodeAction action) {
        setAction(action);
    }

    public SendTFACodeAction getAction() {
        return action;
    }

    public void setAction(SendTFACodeAction action) {
        this.action = action;
    }

    @XmlAttribute(name=AccountConstants.E_ACTION)
    private SendTFACodeAction action;

    @XmlEnum
    public enum SendTFACodeAction {

        @XmlEnumValue("email") EMAIL("email"),
        @XmlEnumValue("reset") RESET("reset");
        private final String action;

        private SendTFACodeAction(String action) {
            this.action = action;
        }

        @Override
        public String toString() {
            return action;
        }
    }
}