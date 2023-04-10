/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.KeyAndValue;

/*
   <uid id="{msg-id}">{uid}</uid>
 */
public class Uid implements KeyAndValue {

    /**
     * @zm-api-field-tag message-id
     * @zm-api-field-description Message ID
     */
    @XmlAttribute(name=MailConstants.A_ID, required=true)
    private String msgid;

    /**
     * @zm-api-field-tag uid value
     * @zm-api-field-description POP3 UID value
     */
    @XmlValue
    private String value;

    public Uid() {
    }

    public Uid(String msgid) {
        setMsgId(msgid);
    }

    public Uid(String msgid, String value) {
        setMsgId(msgid);
        setValue(value);
    }

    public static Uid createUidWithMsgidAndValue(String msgid, String value) {
        return new Uid(msgid, value);
    }

    public String getMsgId() { return msgid; }
    public void setMsgId(String msgid) { this.msgid = msgid; }

    @Override
    public String getValue() { return value; }
    @Override
    public void setValue(String value) { this.value = value; }

    public static Multimap<String, String> toMultimap(Iterable<Uid> uids) {
        Multimap<String, String> map = ArrayListMultimap.create();
        for (Uid uid : uids) {
            map.put(uid.getMsgId(), uid.getValue());
        }
        return map;
    }

    @Override
    public void setKey(String key) { setMsgId(key); }
    @Override
    public String getKey() { return getMsgId(); }
}
