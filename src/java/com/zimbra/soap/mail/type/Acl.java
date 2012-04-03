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

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/*
<acl [internalGrantExpiry="{millis-since-epoch}"] [guestGrantExpiry="{millis-since-epoch}"]>
  <grant .. />*
</acl>
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Acl {

    /**
     * @zm-api-field-tag millis-since-epoch
     * @zm-api-field-description Time when grants to internal grantees expire.
     *   If not specified in the request, defaults to the maximum allowed expiry for internal grants.
     *   If not specified in the response, defaults to 0.
     *   Value of 0 indicates that these grants never expire.
     */
    @XmlAttribute(name=MailConstants.A_INTERNAL_GRANT_EXPIRY /* internalGrantExpiry */, required=false)
    private Long internalGrantExpiry;

    /**
     * @zm-api-field-tag millis-since-epoch
     * @zm-api-field-description Time when grants to guest grantees expire.
     *   If not specified in the request, defaults to the maximum allowed expiry for guest/external
     *   user grants.  If not specified in the response, defaults to 0.
     *   Value of 0 indicates that these grants never expire.
     */
    @XmlAttribute(name=MailConstants.A_GUEST_GRANT_EXPIRY /* guestGrantExpiry */, required=false)
    private Long guestGrantExpiry;

    /**
     * @zm-api-field-description Grants
     */
    @XmlElement(name=MailConstants.E_GRANT /* grant */, required=false)
    private List<Grant> grants = Lists.newArrayList();

    public Acl() {
    }

    public Long getInternalGrantExpiry() {
        return internalGrantExpiry;
    }

    public void setInternalGrantExpiry(Long internalGrantExpiry) {
        this.internalGrantExpiry = internalGrantExpiry;
    }

    public Long getGuestGrantExpiry() {
        return guestGrantExpiry;
    }

    public void setGuestGrantExpiry(Long guestGrantExpiry) {
        this.guestGrantExpiry = guestGrantExpiry;
    }

    public List<Grant> getGrants() {
        return Collections.unmodifiableList(grants);
    }

    public void setGrants(Collection<Grant> grants) {
        this.grants.clear();
        if (grants != null) {
            this.grants.addAll(grants);
        }
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
                .add("internalGrantExpiry", internalGrantExpiry)
                .add("guestGrantExpiry", guestGrantExpiry)
                .add("grants", grants);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
