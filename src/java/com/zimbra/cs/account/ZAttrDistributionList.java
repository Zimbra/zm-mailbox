/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.Map;

/**
 * AUTO-GENERATED. DO NOT EDIT.
 *
 */
public class ZAttrDistributionList extends MailTarget {

    protected ZAttrDistributionList(String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs, null);
    }

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 5.0 schemers 20081104-1827 */

    /**
     * RFC2256: common name(s) for which the entity is known by
     *
     * @return cn, or null unset
     */
    @ZAttr(id=-1)
    public String getCn() {
        return getAttr(Provisioning.A_cn);
    }

    /**
     * RFC2256: descriptive information
     *
     * @return description, or null unset
     */
    @ZAttr(id=-1)
    public String getDescription() {
        return getAttr(Provisioning.A_description);
    }

    /**
     * RFC2798: preferred name to be used when displaying entries
     *
     * @return displayName, or null unset
     */
    @ZAttr(id=-1)
    public String getDisplayName() {
        return getAttr(Provisioning.A_displayName);
    }

    /**
     * RFC1274: RFC822 Mailbox
     *
     * @return mail, or null unset
     */
    @ZAttr(id=-1)
    public String getMail() {
        return getAttr(Provisioning.A_mail);
    }

    /**
     * RFC1274: user identifier
     *
     * @return uid, or null unset
     */
    @ZAttr(id=-1)
    public String getUid() {
        return getAttr(Provisioning.A_uid);
    }

    /**
     * Zimbra access control list
     *
     * @return zimbraACE, or ampty array if unset
     */
    @ZAttr(id=659)
    public String[] getACE() {
        return getMultiAttr(Provisioning.A_zimbraACE);
    }

    /**
     * Deprecated since: 3.2.0. greatly simplify dl/group model. Orig desc:
     * Zimbra Systems Unique Group ID
     *
     * @return zimbraGroupId, or null unset
     */
    @ZAttr(id=325)
    public String getGroupId() {
        return getAttr(Provisioning.A_zimbraGroupId);
    }

    /**
     * Zimbra Systems Unique ID
     *
     * @return zimbraId, or null unset
     */
    @ZAttr(id=1)
    public String getId() {
        return getAttr(Provisioning.A_zimbraId);
    }

    /**
     * locale of entry, e.g. en_US
     *
     * @return zimbraLocale, or null unset
     */
    @ZAttr(id=345)
    public String getLocaleAsString() {
        return getAttr(Provisioning.A_zimbraLocale);
    }

    /**
     * administrative notes
     *
     * @return zimbraNotes, or null unset
     */
    @ZAttr(id=9)
    public String getNotes() {
        return getAttr(Provisioning.A_zimbraNotes);
    }

    ///// END-AUTO-GEN-REPLACE


}
