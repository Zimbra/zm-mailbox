/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraKeyValuePairs;
import com.zimbra.soap.mail.type.Uid;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Set UID string in the metadata which may be used for the POP3 UID command
 * <br />
 * This request set the value to the Metadata.FN_POP3_UID ("p3uid").  The actual output of
 * POP3 UIDL command depends on the LDAP attribute zimbraFeatureCustomUIDEnabled.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SET_POP3_UID_REQUEST)
public class SetPop3UIDRequest {

	@ZimbraKeyValuePairs
    @XmlElement(name=MailConstants.E_POP3UID, required=false)
    private List<Uid> uids = Lists.newArrayList();

    public SetPop3UIDRequest() {
    }

    public void setUids(Iterable <Uid> uids) {
        this.uids.clear();
        if (uids != null) {
            Iterables.addAll(this.uids, uids);
        }
    }

    public SetPop3UIDRequest addUid(Uid uid) {
        this.uids.add(uid);
        return this;
    }

    public List<Uid> getUids() {
        return Collections.unmodifiableList(uids);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("uids", uids)
            .toString();
    }
}
