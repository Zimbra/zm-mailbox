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

import org.w3c.dom.Element;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.ZimletConstants;
import com.zimbra.soap.base.ZimletConfigInfo;
import com.zimbra.soap.base.ZimletContextInterface;
import com.zimbra.soap.base.ZimletDesc;
import com.zimbra.soap.base.ZimletInterface;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"zimletContext", "zimlet", "zimletConfig", "zimletHandlerConfig"})
@GraphQLType(name=GqlConstants.CLASS_ACCOUNT_ZIMLET_INFO, description="Zimlets for account")
public class AccountZimletInfo
implements ZimletInterface {

    /**
     * @zm-api-field-description Zimlet context
     */
    @XmlElement(name=AccountConstants.E_ZIMLET_CONTEXT /* zimletContext */, required=false)
    private AccountZimletContext zimletContext;

    /**
     * @zm-api-field-description Zimlet description
     */
    @XmlElement(name=ZimletConstants.ZIMLET_TAG_ZIMLET /* zimlet */, required=false)
    private AccountZimletDesc zimlet;

    /**
     * @zm-api-field-description Other elements
     */
    @XmlElement(name=ZimletConstants.ZIMLET_TAG_CONFIG /* zimletConfig */, required=false)
    private AccountZimletConfigInfo zimletConfig;

    @XmlAnyElement
    private Element zimletHandlerConfig;

    public AccountZimletInfo() {
    }

    public void setZimletContext(AccountZimletContext zimletContext) { this.zimletContext = zimletContext; }
    public void setZimlet(AccountZimletDesc zimlet) { this.zimlet = zimlet; }
    public void setZimletConfig(AccountZimletConfigInfo zimletConfig) { this.zimletConfig = zimletConfig; }
    @Override
    public void setZimletHandlerConfig(Element zimletHandlerConfig) { this.zimletHandlerConfig = zimletHandlerConfig; }

    @Override
    @GraphQLQuery(name=GqlConstants.ZIMLET_CONTEXT, description="Zimlet context")
    public AccountZimletContext getZimletContext() { return zimletContext; }
    @Override
    @GraphQLQuery(name=GqlConstants.ZIMLET, description="Zimlet description")
    public AccountZimletDesc getZimlet() { return zimlet; }
    @Override
    @GraphQLQuery(name=GqlConstants.ZIMLET_CONFIG, description="Other elements")
    public AccountZimletConfigInfo getZimletConfig() { return zimletConfig; }
    @Override
    @GraphQLQuery(name=GqlConstants.ZIMLET_HANDLER_CONFIG)
    public Element getZimletHandlerConfig() { return zimletHandlerConfig; }

    @Override
    public void setZimletContext(ZimletContextInterface zimletContext) {
        setZimletContext((AccountZimletContext) zimletContext);
    }

    @Override
    public void setZimlet(ZimletDesc zimlet) { setZimlet((AccountZimletDesc) zimlet); }
    @Override
    public void setZimletConfig(ZimletConfigInfo zimletConfig) {
        setZimletConfig((AccountZimletConfigInfo) zimletConfig);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("zimletContext", zimletContext)
            .add("zimlet", getZimlet())
            .add("zimletConfig", getZimletConfig())
            .add("zimletHandlerConfig", zimletHandlerConfig);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
