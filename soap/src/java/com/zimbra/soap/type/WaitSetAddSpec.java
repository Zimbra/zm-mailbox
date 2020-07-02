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

package com.zimbra.soap.type;

import java.util.Collection;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class WaitSetAddSpec {

    private static Joiner COMMA_JOINER = Joiner.on(",");
    private static Splitter COMMA_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

    /**
     * @zm-api-field-tag account-name
     * @zm-api-field-description Name of account
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description ID of account.  Ignored if <b>name</b> is supplied
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag last-known-sync-token
     * @zm-api-field-description Last known sync token
     */
    @XmlAttribute(name=MailConstants.A_TOKEN /* token */, required=false)
    private String token;

    /**
     * @zm-api-field-tag waitset-types
     * @zm-api-field-description interest types: comma-separated list.  Currently:
     * <table>
     * <tr> <td> <b>f</b> </td> <td> folders </td> </tr>
     * <tr> <td> <b>m</b> </td> <td> messages </td> </tr>
     * <tr> <td> <b>c</b> </td> <td> contacts </td> </tr>
     * <tr> <td> <b>a</b> </td> <td> appointments </td> </tr>
     * <tr> <td> <b>t</b> </td> <td> tasks </td> </tr>
     * <tr> <td> <b>d</b> </td> <td> documents </td> </tr>
     * <tr> <td> <b>all</b> </td> <td> all types (equiv to "f,m,c,a,t,d") </td> </tr>
     * </table>
     * <p>If not specified, the value of <b>defTypes</b> in the request is used</p>
     */
    @XmlAttribute(name=MailConstants.A_TYPES /* types */, required=false)
    private String interests;

    private final Set<Integer> folderInterests = Sets.newHashSet();

    public WaitSetAddSpec() {
    }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }

    public void setId(String id) { this.id = id; }
    public String getId() { return id; }

    public void setToken(String token) { this.token = token; }
    public String getToken() { return token; }

    public void setInterests(String interests) { this.interests = interests; }
    public String getInterests() { return interests; }

    /**
     * @zm-api-field-tag waitset-folder-interests
     * @zm-api-field-description Comma separated list of IDs for folders.
     */
    @XmlAttribute(name=MailConstants.A_FOLDER_INTERESTS /* folderInterests */, required=false)
    public String getFolderInterests() {
        if (folderInterests.isEmpty()) {
            return null;
        }
        return COMMA_JOINER.join(folderInterests);
    }

    public void setFolderInterests(String fInterests) {
        this.folderInterests.clear();
        for (String fi : COMMA_SPLITTER.split(Strings.nullToEmpty(fInterests))) {
            folderInterests.add(Integer.parseInt(fi));
        }
    }

    public void addFolderInterest(Integer folderId) {
        this.folderInterests.add(folderId);
    }

    @XmlTransient
    public Set<Integer> getFolderInterestsAsSet() { return folderInterests; }

    public WaitSetAddSpec setFolderInterests(Integer... folderIds) {
        this.folderInterests.clear();
        if (folderIds != null) {
            for (Integer folderId : folderIds) {
                this.folderInterests.add(folderId);
            }
        }
        return this;
    }

    public WaitSetAddSpec setFolderInterests(Collection<Integer> folderIds) {
        this.folderInterests.clear();
        if (folderIds != null) {
            this.folderInterests.addAll(folderIds);
        }
        return this;
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("id", id)
            .add("token", token)
            .add("interests", interests)
            .add("folderInterests", folderInterests);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}