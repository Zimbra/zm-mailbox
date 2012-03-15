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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CosSelector;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.TargetWithType;

// see soap-right.txt
/**
 * @zm-api-command-description Returns attributes, with defaults and constraints if any,  that can be set by the
 * authed admin when an object is created.
 * <br />
 * <b>GetCreateObjectAttrsRequest</b> returns the equivalent of setAttrs portion of <b>GetEffectiveRightsResponse</b>.
 * <br />
 * GetCreateObjectAttrsRequest is needed becasue GetEffectiveRightsRequest requires a target, but when we are
 * creating a object, the target object does not exist yet.
 * <br />
 * <br />
 * The result can help the admin console decide on what tabs/attributes to display for creating objects.
 * <br />
 * e.g. 1.  Creating an account:
 * <pre>
 * &lt;GetCreateObjectAttrsRequest>
 *   &lt;target type="account"/>
 *   &lt;domain by="name">test.com&lt;/domain>
 *   &lt;cos by="name">standard&lt;/cos>
 * &lt;/GetCreateObjectAttrsRequest>
 * 
 * &lt;GetCreateObjectAttrsResponse>
 *     &lt;setAttrs>
 *           &lt;a n="zimbraMailQuota"/>
 *               &lt;constraint>
 *                   &lt;min>1&lt;/min>
 *                   &lt;max>3&lt;/max>
 *               &lt;/constraint>
 *               &lt;default>
 *                   &lt;v>2&lt;/v>
 *               &lt;/default>
 *           &lt;a n="zimbraMailStatus"/>
 *           &lt;a n="zimbraFeatureCalendarEnabled"/>
 *           ...
 *     &lt;/setAttrs>
 * &lt;/GetCreateObjectAttrsResponse>
 * </pre>
 * e.g. 2.  Creating a server:
 * 
 * <pre>
 * &lt;GetCreateObjectAttrsRequest>
 *   &lt;target type="server"/>
 * &lt;/GetCreateObjectAttrsRequest>
 * 
 * &lt;GetCreateObjectAttrsResponse>
 *     &lt;target type="server"/>
 *     &lt;setAttrs>
 *           &lt;a n="zimbraLmtpExposeVersionOnBanner"/>
 *           &lt;a n="zimbraXMPPServerDialbackKey"/>
 *           &lt;a n="zimbraReverseProxyImapEnabledCapability"/>
 *               &lt;constraint>
 *                   &lt;values>
 *                       &lt;v>IMAP4rev1&lt;/v>
 *                       &lt;v>BINARY&lt;/v>
 *                   &lt;/values>
 *               &lt;/constraint>
 *           ...
 *     &lt;/setAttrs>
 * &lt;/GetCreateObjectAttrsResponse>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_CREATE_OBJECT_ATTRS_REQUEST)
public class GetCreateObjectAttrsRequest {

    /**
     * @zm-api-field-description Target
     */
    @XmlElement(name=AdminConstants.E_TARGET, required=true)
    private final TargetWithType target;

    /**
     * @zm-api-field-description Domain
     * <br />
     * required if {target-type} is account/calresource/dl/domain, ignored otherwise.
     * <ul>
     * <li> if {target-type} is account/calresource/dl: this is the domain in which the object will be in.
     *       the domain can be speciffied by id or by name
     * <li> if {target-type} is domain, it is the domain name to be created.
     *       e.g. to create a subdomain named foo.bar.test.com, should pass in 
     *       &lt;domain by="name">foo.bar.test.com&lt;/domain>.
     * </ul>
     */
    @XmlElement(name=AdminConstants.E_DOMAIN, required=false)
    private final DomainSelector domain;

    /**
     * @zm-api-field-description COS
     * <br />
     * Optional if {target-type} is account/calresource, ignored otherwise
     * <br />
     * If missing, default cos of the domain will be used
     */
    @XmlElement(name=AdminConstants.E_COS, required=false)
    private final CosSelector cos;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetCreateObjectAttrsRequest() {
        this((TargetWithType) null, (DomainSelector) null, (CosSelector) null);
    }

    public GetCreateObjectAttrsRequest(TargetWithType target) {
        this(target, (DomainSelector) null, (CosSelector) null);
    }

    public GetCreateObjectAttrsRequest(TargetWithType target,
                DomainSelector domain, CosSelector cos) {
        this.target = target;
        this.domain = domain;
        this.cos = cos;
    }

    public TargetWithType getTarget() { return target; }
    public DomainSelector getDomain() { return domain; }
    public CosSelector getCos() { return cos; }
}
