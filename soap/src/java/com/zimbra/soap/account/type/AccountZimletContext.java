/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.MoreObjects;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.base.ZimletContextInterface;

import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_ACCOUNT_ZIMLET_CONTEXT, description="Zimlet context")
public class AccountZimletContext
implements ZimletContextInterface {

    /**
     * @zm-api-field-tag zimlet-base-url
     * @zm-api-field-description Zimlet Base URL
     */
    @XmlAttribute(name=AccountConstants.A_ZIMLET_BASE_URL /* baseUrl */, required=true)
    private String zimletBaseUrl;

    /**
     * @zm-api-field-tag zimlet-priority
     * @zm-api-field-description Zimlet Priority
     */
    @XmlAttribute(name=AccountConstants.A_ZIMLET_PRIORITY /* priority */, required=false)
    private Integer zimletPriority;

    /**
     * @zm-api-field-tag zimlet-presence
     * @zm-api-field-description Zimlet presence
     * <br />
     * Valid values: <b>mandatory</b> | <b>enabled</b> | <b>disabled</b>
     */
    @XmlAttribute(name=AccountConstants.A_ZIMLET_PRESENCE /* presence */, required=true)
    private String zimletPresence;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AccountZimletContext() {
        this((String) null, (Integer) null, (String) null);
    }

    public AccountZimletContext(String zimletBaseUrl, Integer zimletPriority,
                            String zimletPresence) {
        this.setZimletBaseUrl(zimletBaseUrl);
        this.setZimletPriority(zimletPriority);
        this.setZimletPresence(zimletPresence);
    }

    public static AccountZimletContext createForBaseUrlPriorityAndPresence(
            String zimletBaseUrl, Integer zimletPriority, String zimletPresence) {
        return new AccountZimletContext(zimletBaseUrl, zimletPriority, zimletPresence);
    }

    @Override
    public void setZimletBaseUrl(String zimletBaseUrl) { this.zimletBaseUrl = zimletBaseUrl; }
    @Override
    public void setZimletPriority(Integer zimletPriority) { this.zimletPriority = zimletPriority; }
    @Override
    public void setZimletPresence(String zimletPresence) { this.zimletPresence = zimletPresence; }

    @GraphQLQuery(name=GqlConstants.ZIMLET_BASE_URL, description="Zimlet Base URL")
    @GraphQLNonNull
    @Override
    public String getZimletBaseUrl() { return zimletBaseUrl; }
    @GraphQLQuery(name=GqlConstants.ZIMLET_PRIORITY, description="Zimlet Priority")
    @Override
    public Integer getZimletPriority() { return zimletPriority; }
    @GraphQLQuery(name=GqlConstants.ZIMLET_PRESENCE, description="Zimlet presence")
    @GraphQLNonNull
    @Override
    public String getZimletPresence() { return zimletPresence; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("zimletBaseUrl", getZimletBaseUrl())
            .add("zimletPriority", getZimletPriority())
            .add("zimletPresence", getZimletPresence());
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
