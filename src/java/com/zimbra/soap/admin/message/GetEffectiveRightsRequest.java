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

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.EffectiveRightsTargetSelector;
import com.zimbra.soap.admin.type.GranteeSelector;

/**
 * @zm-api-command-description Returns <b>effective ADMIN</b> rights the authenticated admin has on the specified
 * target entry.
 * <br />
 * <br />
 * Effective rights are the rights the admin is actually allowed.  It is the net result of applying ACL checking
 * rules given the target and grantee.  Specifically denied rights will <b>not</b> be returned.
 * <br />
 * <br />
 * The result can help the admin console decide on what tabs to display after a target is selected.  For example,
 * after user1 is selected, if the admin does not have right to setPassword, it should probably hide or gray out
 * the setPassword tab.
 * <br />
 * e.g.
 * <pre>
 *     &lt;GetEffectiveRightsRequest>
 *       &lt;target type="account" by="id">bba95d7d-0b13-401f-a343-03a8f5a96f7c"/>
 *       &lt;grantee by="name">admin@test.com&lt;/grantee>
 *     &lt;/GetEffectiveRightsRequest>
 *
 *     &lt;GetEffectiveRightsResponse>
 *       &lt;grantee name="admin@test.com&lt;/grantee" id=""/>
 *       &lt;target type="account" name="user1@test.com" id="bba95d7d-0b13-401f-a343-03a8f5a96f7c">
 *         &lt;right n="setPassword"/>
 *         &lt;right n="renameAccount"/>
 *         &lt;right n="deleteAccount"/>
 *         &lt;setAttrs>
 *           &lt;a n="zimbraMailQuota" min="100000000"/>
 *           &lt;a n="zimbraMailStatus"/>
 *           &lt;a n="zimbraFeatureMailEnabled" values="TRUE,FALSE"/>
 *           ...
 *         &lt;/setAttrs>
 *         &lt;getAttrs>
 *           &lt;a n="..."/>
 *           &lt;a n="..."/>
 *           ...
 *         &lt;/getAttrs>
 *       &lt;/target>
 *     &lt;/GetEffectiveRightsRequest>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_EFFECTIVE_RIGHTS_REQUEST)
public class GetEffectiveRightsRequest {

    public static final String EXPAND_SET_ATTRS = "setAttrs";
    public static final String EXPAND_GET_ATTRS = "getAttrs";

    private static Splitter COMMA_SPLITTER = Splitter.on(",");
    private static Joiner COMMA_JOINER = Joiner.on(",");

    @XmlTransient
    private Boolean expandSetAttrs;
    @XmlTransient
    private Boolean expandGetAttrs;

    /**
     * @zm-api-field-description Target
     */
    @XmlElement(name=AdminConstants.E_TARGET, required=true)
    private final EffectiveRightsTargetSelector target;

    /**
     * @zm-api-field-description Grantee.  If <b>&lt;grantee></b> is omitted, the account identified by the
     * auth token is regarded as the grantee.
     */
    @XmlElement(name=AdminConstants.E_GRANTEE, required=false)
    private final GranteeSelector grantee;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetEffectiveRightsRequest() {
        this((EffectiveRightsTargetSelector) null, (GranteeSelector) null,
                (Boolean) null, (Boolean) null);
    }

    public GetEffectiveRightsRequest(EffectiveRightsTargetSelector target,
            GranteeSelector grantee,
            Boolean expandSetAttrs, Boolean expandGetAttrs) {
        this.target = target;
        this.grantee = grantee;
        setExpandSetAttrs(expandSetAttrs);
        setExpandGetAttrs(expandGetAttrs);
    }

    /**
     * @zm-api-field-tag expand-all-attrs
     * @zm-api-field-description Whether to include all attribute names in the <b>&lt;getAttrs>/&lt;setAttrs></b>
     * elements in the response if all attributes of the target are gettable/settable
     * Valid values are:
     * <table>
     * <tr> <td> <b>getAttrs</b> </td> <td> expand attrs in getAttrs in the response </td> </tr>
     * <tr> <td> <b>setAttrs</b> </td> <td> expand attrs in setAttrs in the response </td> </tr>
     * <tr> <td> <b>getAttrs,setAttrs</b> </td> 
     *                           <td> expand attrs in both getAttrs and setAttrs in the response </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_EXPAND_ALL_ATTRS, required=false)
    public String getExpandAllAttrs() {
        List <String> settings = Lists.newArrayList();
        if ((expandSetAttrs != null) && expandSetAttrs)
            settings.add(EXPAND_SET_ATTRS);
        if ((expandGetAttrs != null) && expandGetAttrs)
            settings.add(EXPAND_GET_ATTRS);
        String retVal = COMMA_JOINER.join(settings);
        if (retVal.length() == 0)
            return null;
        else
            return retVal;
    }

    public void setExpandAllAttrs(String types)
    throws ServiceException {
        expandGetAttrs = null;
        expandSetAttrs = null;
        for (String typeString : COMMA_SPLITTER.split(types)) {
            String exp = typeString.trim();
            if (exp.equals(EXPAND_SET_ATTRS))
                expandSetAttrs = true;
            else if (exp.equals(EXPAND_GET_ATTRS))
                expandGetAttrs = true;
            else
                throw ServiceException.INVALID_REQUEST(
                    "invalid " + AdminConstants.A_EXPAND_ALL_ATTRS +
                    " value: " + exp, null);
        }
    }

    public void setExpandGetAttrs(Boolean expandGetAttrs) {
        this.expandGetAttrs = expandGetAttrs;
    }

    public Boolean getExpandGetAttrs() { return expandGetAttrs; }

    public void setExpandSetAttrs(Boolean expandSetAttrs) {
        this.expandSetAttrs = expandSetAttrs;
    }

    public Boolean getExpandSetAttrs() { return expandSetAttrs; }

    public EffectiveRightsTargetSelector getTarget() { return target; }
    public GranteeSelector getGrantee() { return grantee; }
}
