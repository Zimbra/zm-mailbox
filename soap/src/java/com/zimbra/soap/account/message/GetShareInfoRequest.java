/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.GranteeChooser;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Get information about published shares
 * @zm-api-request-description
 * Notes:
 * <ul>
 * <li> if <b>&lt;owner></b> is *not* specified the server will search the LDAP directory for published shares
 *      (<b>zimbraSharedItem</b> account attribute) accessible to the authed user.
 * <li> if <b>&lt;owner></b> *is* specified, the server will iterate through the owner's mailbox to discover all
 *      shares applicable to the authed user, instead of looking at any of the published share info.
 * <li> All applicable shares will be returned, including any shares that are:
 *     <table>
 *     <tr> <td>shared with the account directly</td>                              <td></td></tr>
 *     <tr> <td>shared with any group(and parent groups) the account belongs.</td> <td>(*is* supported)</td></tr>
 *     <tr> <td>shared with the cos assigned to the account. </td>                 <td>(*is* supported)</td></tr>
 *     <tr> <td>shared with the domain this account is in. </td>                   <td>(*is* supported)</td></tr>
 *     <tr> <td>shared with all authed users (i.e. all Zimbra users) </td>         <td>(*is* supported)</td></tr>
 *     <tr> <td>shared with the public </td>                                       <td>(*is* supported)</td></tr>
 *     </table>
 * </ul>
 * e.g.
 * <ol>
 * <li> What folders are shared with any of the groups I belong to?
 *      <ol type="a">
 *      <li> folders of any user
 *           <pre>
 *              &lt;GetShareInfoRequest>
 *                  &lt;grantee type="grp">
 *              &lt;/GetShareInfoRequest>
 *           </pre>
 *      <li> folders of a particular user
 *           <pre>
 *              &lt;GetShareInfoRequest>
 *                  &lt;grantee type="grp">
 *                  &lt;owner by="name">user1@example.com&lt;/owner>
 *              &lt;/GetShareInfoRequest>
 *           </pre>
 *      </ol>

 * <li> What folders does a particular user share with me?
 *      <ol type="a">
 *      <li> include all folders directly shared with me and shared with an entry I can inherit shares from:
 *           <pre>
 *              &lt;GetShareInfoRequest>
 *                  &lt;owner by="name">user1@example.com&lt;/owner>
 *              &lt;/GetShareInfoRequest>
 *           </pre>
 *      <li> include only folders directly shared with me.
 *           <pre>
 *              &lt;GetShareInfoRequest>
 *                  &lt;grantee type="usr">
 *                  &lt;owner by="name">user1@example.com&lt;/owner>
 *              &lt;/GetShareInfoRequest>
 *           </pre>
 *      </ol>
 * <li> Show me all folders shared directly with me and with any entry I can inherit shares from:
 *           <pre>
 *              &lt;GetShareInfoRequest>
 *              &lt;/GetShareInfoRequest>
 *           </pre>
 * </ol>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_SHARE_INFO_REQUEST)
public class GetShareInfoRequest {

    /**
     * @zm-api-field-description Flags that have been proxied to this server because the specified "owner account" is
     * homed here.  Do not proxy in this case. (Used internally by ZCS)
     */
    @XmlAttribute(name=AccountConstants.A_INTERNAL, required=false)
    private ZmBoolean internal;

    /**
     * @zm-api-field-description Flag whether own shares should be included:
     * <ul>
     * <li> <b>0</b> if shares owned by the requested account should not be included in the response
     * <li> <b>1</b> (default) include shares owned by the requested account
     * </ul>
     * (It might be useful to see the shares I've shared to a DL that I belong to so that I know I'm sharing it
     * correctly.)
     */
    @XmlAttribute(name=AccountConstants.A_INCLUDE_SELF, required=false)
    private ZmBoolean includeSelf;

    /**
     * @zm-api-field-description Filter by the specified grantee type
     */
    @XmlElement(name=AccountConstants.E_GRANTEE, required=false)
    private GranteeChooser grantee;

    /**
     * @zm-api-field-description Specifies the owner of the share
     */
    @XmlElement(name=AccountConstants.E_OWNER, required=false)
    private AccountSelector owner;

    public GetShareInfoRequest() {
    }

    public static GetShareInfoRequest create(AccountSelector owner, GranteeChooser grantee, Boolean includeSelf) {
        GetShareInfoRequest req = new GetShareInfoRequest();
        req.setOwner(owner);
        req.setGrantee(grantee);
        req.setIncludeSelf(includeSelf);
        return req;
    }

    public void setInternal(Boolean internal) { this.internal = ZmBoolean.fromBool(internal); }
    public void setIncludeSelf(Boolean includeSelf) { this.includeSelf = ZmBoolean.fromBool(includeSelf); }

    public void setGrantee(GranteeChooser grantee) { this.grantee = grantee; }
    public void setOwner(AccountSelector owner) { this.owner = owner; }
    public Boolean getInternal() { return ZmBoolean.toBool(internal); }
    public Boolean getIncludeSelf() { return ZmBoolean.toBool(includeSelf); }
    public GranteeChooser getGrantee() { return grantee; }
    public AccountSelector getOwner() { return owner; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("internal", internal)
            .add("includeSelf", includeSelf)
            .add("grantee", grantee)
            .add("owner", owner)
            .toString();
    }
}
