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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.SMIMEConfigModifications;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Modify a configuration for SMIME public key lookup via external LDAP on a domain or
 * globalconfig.
 * <br />
 * Notes: if <b>&lt;domain></b> is present, modify the config on the domain, otherwise modify the config on
 * globalconfig.
 * <br />
 * <br />
 * Attributes:
 * <table>
 * <tr> <td> <b> zimbraSMIMELdapURL * </b> </td>
 *      <td> required </td>
 *      <td> LDAL URL - Multiple space-separated URLs can be specified for fallback purpose </td>
 * </tr>
 * <tr> <td> <b> zimbraSMIMELdapStartTlsEnabled * </b> </td>
 *      <td> optional - TRUE/FALSE [default] </td>
 *      <td> Whether startTLS is enabled for the LDAP connection </td>
 * </tr>
 * <tr> <td> <b> zimbraSMIMELdapBindDn </b> </td>
 *      <td> optional - default value is empty </td>
 *      <td> Bind DN.  ZCS will do anonymous bind if this attribute is not set </td>
 * </tr>
 * <tr> <td> <b> zimbraSMIMELdapBindPassword </b> </td>
 *      <td> optional - default value is empty </td>
 *      <td> Bind password. Ignored if zimbraSMIMELdapBindDn is not set </td>
 * </tr>
 * <tr> <td> <b> zimbraSMIMELdapSearchBase * </b> </td>
 *      <td> optional - default is LDAP DIT root </td>
 *      <td> LDAP search base DN </td>
 * </tr>
 * <tr> <td> <b> zimbraSMIMELdapFilter </b> </td>
 *      <td> required </td>
 *      <td> LDAP search filter template
 *           <br />
 *           Can contain the following conversion variables:
 *           <br />
 *           %n - search key with @ (or without, if no @ was specified)
 *           <br />
 *           %u - with @ removed
 *           <br />
 *           <br />
 *           e.g. (mail=%n)
 *      </td>
 * </tr>
 * <tr> <td> <b>zimbraSMIMELdapAttribute</b> </td>
 *      <td> required </td>
 *      <td> LDAP attributes for SMIME certificates </td>
 * </tr>
 * </table>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_MODIFY_SMIME_CONFIG_REQUEST)
public class ModifySMIMEConfigRequest {

    /**
     * @zm-api-field-description SMIME Config Modifications
     */
    @XmlElement(name=AdminConstants.E_CONFIG, required=true)
    private final SMIMEConfigModifications config;

    /**
     * @zm-api-field-description Domain selector
     */
    @XmlElement(name=AdminConstants.E_DOMAIN, required=false)
    private final DomainSelector domain;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ModifySMIMEConfigRequest() {
        this((SMIMEConfigModifications) null, (DomainSelector) null);
    }

    public ModifySMIMEConfigRequest(SMIMEConfigModifications config,
                    DomainSelector domain) {
        this.config = config;
        this.domain = domain;
    }

    public SMIMEConfigModifications getConfig() { return config; }
    public DomainSelector getDomain() { return domain; }
}
