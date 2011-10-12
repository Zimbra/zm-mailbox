/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

package com.zimbra.soap.mail.type;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;

/*
<grant perm="{rights}" gt="{grantee-type}" zid="{zimbra-id}" d="{grantee-name}" [pw="{password-for-guest}"] [key=="{access-key}"]/>*
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class Grant {

    @XmlEnum
    public enum GranteeType {
        @XmlEnumValue("usr") USER ("usr"),
        @XmlEnumValue("grp") GROUP ("grp"),
        @XmlEnumValue("all") AUTH_USER ("all"),
        @XmlEnumValue("dom") DOMAIN ("dom"),
        @XmlEnumValue("guest") GUEST ("guest"),
        @XmlEnumValue("key") KEY ("key"),
        @XmlEnumValue("pub") PUBLIC ("pub");

        private static Map<String, GranteeType> nameToGranteeType = new HashMap<String, GranteeType>();

        static {
            for (GranteeType gt : values()) {
                nameToGranteeType.put(gt.toString(), gt);
            }
        }

        private String name;

        private GranteeType(String name) {
            this.name = name;
        }

        public static GranteeType fromString(String s)
        throws ServiceException {
            GranteeType gt = nameToGranteeType.get(s);
            if (gt == null) {
                throw ServiceException.INVALID_REQUEST("Invalid grantee type '" + s + "'.  Valid values are " +
                    StringUtil.join(",", values()), null);
            }
            return gt;
        }

        @Override
        public String toString() {
            return name;
        }
    };

    @XmlAttribute(name=MailConstants.A_RIGHTS /* perm */, required=true)
    private String perm;

    @XmlAttribute(name=MailConstants.A_GRANT_TYPE /* gt */, required=true)
    private GranteeType granteeType;

    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID /* zid */, required=true)
    private String granteeId;

    @XmlAttribute(name=MailConstants.A_DISPLAY /* d */, required=false)
    private String granteeName;

    @XmlAttribute(name=MailConstants.A_PASSWORD /* pw */, required=false)
    private String guestPassword;

    @XmlAttribute(name=MailConstants.A_ACCESSKEY /* key */, required=false)
    private String accessKey;

    public Grant() {
    }

    public void setPerm(String perm) { this.perm = perm; }
    public void setGranteeType(GranteeType granteeType) { this.granteeType = granteeType; }
    public void setGranteeId(String granteeId) { this.granteeId = granteeId; }
    public void setGranteeName(String granteeName) { this.granteeName = granteeName; }
    public void setGuestPassword(String guestPassword) { this.guestPassword = guestPassword; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getPerm() { return perm; }
    public GranteeType getGranteeType() { return granteeType; }
    public String getGranteeId() { return granteeId; }
    public String getGranteeName() { return granteeName; }
    public String getGuestPassword() { return guestPassword; }
    public String getAccessKey() { return accessKey; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("perm", perm)
            .add("granteeType", granteeType)
            .add("granteeId", granteeId)
            .add("granteeName", granteeName)
            .add("guestPassword", guestPassword)
            .add("accessKey", accessKey);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
