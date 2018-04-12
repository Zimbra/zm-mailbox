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

package com.zimbra.soap.admin.type;

import org.w3c.dom.Element;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.ZimletConstants;
import com.zimbra.soap.base.ZimletConfigInfo;
import com.zimbra.soap.base.ZimletContextInterface;
import com.zimbra.soap.base.ZimletDesc;
import com.zimbra.soap.base.ZimletInterface;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"zimletContext", "zimlet", "zimletConfig", "zimletHandlerConfig"})
public class AdminZimletInfo
implements ZimletInterface {

    /**
     * @zm-api-field-description Zimlet context
     */
    @XmlElement(name=AccountConstants.E_ZIMLET_CONTEXT /* zimletContext */, required=false)
    private AdminZimletContext zimletContext;

    /**
     * @zm-api-field-description Zimlet description
     */
    @XmlElement(name=ZimletConstants.ZIMLET_TAG_ZIMLET /* zimlet */, required=false)
    private AdminZimletDesc zimlet;

    /**
     * @zm-api-field-description Other elements
     */
    @XmlElement(name=ZimletConstants.ZIMLET_TAG_CONFIG /* zimletConfig */, required=false)
    private AdminZimletConfigInfo zimletConfig;

    @XmlAnyElement
    private Element zimletHandlerConfig;

    public AdminZimletInfo() {
    }

    public void setZimletContext(AdminZimletContext zimletContext) { this.zimletContext = zimletContext; }
    public void setZimlet(AdminZimletDesc zimlet) { this.zimlet = zimlet; }
    public void setZimletConfig(AdminZimletConfigInfo zimletConfig) { this.zimletConfig = zimletConfig; }
    @Override
    public void setZimletHandlerConfig(Element zimletHandlerConfig) { this.zimletHandlerConfig = zimletHandlerConfig; }

    @Override
    public AdminZimletContext getZimletContext() { return zimletContext; }
    @Override
    public AdminZimletDesc getZimlet() { return zimlet; }
    @Override
    public AdminZimletConfigInfo getZimletConfig() { return zimletConfig; }
    @Override
    public Element getZimletHandlerConfig() { return zimletHandlerConfig; }

    @Override
    public void setZimletContext(ZimletContextInterface zimletContext) {
        setZimletContext((AdminZimletContext) zimletContext);
    }

    @Override
    public void setZimlet(ZimletDesc zimlet) { setZimlet((AdminZimletDesc) zimlet); }
    @Override
    public void setZimletConfig(ZimletConfigInfo zimletConfig) {
        setZimletConfig((AdminZimletConfigInfo) zimletConfig);
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
