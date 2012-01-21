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
package com.zimbra.soap.account.message;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;

/**
 * @zm-api-command-description Return all targets of the specified rights applicable to the requested account.
 * <p>
 * Notes:
 * <ol>
 * <li> This call only discovers grants granted on the designated target type of the specified rights.  It does not
 *      return grants granted on target types the rights can inherit from.
 * <li> For <b>sendAs</b>, <b>sendOnBehalfOf</b>, <b>sendAsDistList</b>, <b>sendOnBehalfOfDistList</b> rights,
 *      name attribute is not returned on <b>&lt;target></b> elements.  Instead, addresses in the target entry's
 *      <b>zimbraPrefAllowAddressForDelegatedSender</b> are returned in <b>&lt;e a="{email-address}"/></b> elements
 *      under the <b>&lt;target></b> element.<br />
 *      If <b>zimbraPrefAllowAddressForDelegatedSender</b> is not set on the target entry, the entry's primary
 *      email address will be return in the only <b>&lt;e a="{email-address}"/></b> element under the
 *      <b>&lt;target></b> element.
 * <li> For all other rights, <b>name</b> attribute is always returned on <b>&lt;target></b> elements, no 
 *      <b>&lt;e a="{email-address}"/></b> will be returned.  <b>name</b> attribute contains the entry's primary name.
 * </ol>
 * e.g.
 * <pre>
 * &lt;DiscoverRightsRequest>
 *    &lt;right>sendAs&lt;/right>
 *    &lt;right>sendAsDistList&lt;/right>
 *    &lt;right>viewFreeBusy&lt;/right>
 * &lt;/DiscoverRightsRequest>
 *
 * &lt;DiscoverRightsResponse>
 *    &lt;targets right="sendAs">
 *       &lt;target type="account" id="...">
 *          &lt;e a="user-one@test.com"/>
 *          &lt;e a="user.one@test.com"/>
 *       &lt;/target>
 *       &lt;target type="account" id="...">
 *          &lt;e a="user2@test.com"/>
 *       &lt;/target>
 *    &lt;/targets>
 *    &lt;targets right="sendAsDistList">
 *       &lt;target type="dl" id="...">
 *          &lt;e a="group1@test.com"/>
 *          &lt;e a="group-one@test.com"/>
 *       &lt;/target>
 *       &lt;target type="dl" id="...">
 *          &lt;e a="group2@test.com"/>
 *          &lt;e a="group-two@test.com"/>
 *       &lt;/target>
 *    &lt;/targets>
 *    &lt;targets right="viewFreeBusy">
 *       &lt;target type="account" id="..." name="user1@test.com">
 *    &lt;/targets>
 * &lt;/DiscoverRightsResponse>
 * </pre>
 */
@XmlRootElement(name=AccountConstants.E_DISCOVER_RIGHTS_REQUEST)
public class DiscoverRightsRequest {
    /**
     * @zm-api-field-description The rights.
     */
    @XmlElement(name=AccountConstants.E_RIGHT /* right */, required=true)
    private List<String> rights = Lists.newArrayList();

    public DiscoverRightsRequest() {
        this(null);
    }

    public DiscoverRightsRequest(Iterable <String> rights) {
        setRights(rights);
    }

    public void setRights(Iterable <String> rights) {
        this.rights.clear();
        if (rights != null) {
            Iterables.addAll(this.rights,rights);
        }
    }

    public void addRight(String right) {
        this.rights.add(right);
    }

    public List<String> getRights() {
        return rights;
    }
}
