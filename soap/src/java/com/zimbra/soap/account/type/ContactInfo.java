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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.ContactGroupMemberInterface;
import com.zimbra.soap.base.ContactInterface;
import com.zimbra.soap.base.CustomMetadataInterface;
import com.zimbra.soap.type.ContactAttr;
import com.zimbra.soap.type.ZmBoolean;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_ACCOUNT_CONTACT_INFO, description="Contact information")
public class ContactInfo
implements ContactInterface {

    // Added by e.g. GalSearchControl.doLocalGalAccountSearch
    /**
     * @zm-api-field-tag contact-sort-field-value
     * @zm-api-field-description Sort field value
     */
    @XmlAttribute(name=MailConstants.A_SORT_FIELD /* sf */, required=false)
    private String sortField;

    /**
     * @zm-api-field-description
     */
    @XmlAttribute(name=AccountConstants.A_EXP /* exp */, required=false)
    private ZmBoolean canExpand;

    // id is the only attribute or element that can be required:
    //    GalSearchResultCallback.handleContact(Contact c) and CreateContact.handle
    //    sometimes just create E_CONTACT elements with only an A_ID attribute
    /**
     * @zm-api-field-tag contact-id 
     * @zm-api-field-description Contact ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private String id;

    /**
     * @zm-api-field-tag contact-folder
     * @zm-api-field-description Folder
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folder;

    /**
     * @zm-api-field-tag contact-flags
     * @zm-api-field-description Flags
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    /**
     * @zm-api-field-description Tags (deprecated)
     */
    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    /**
     * @zm-api-field-tag contact-tag-names
     * @zm-api-field-description Tag names
     */
    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    /**
     * @zm-api-field-tag contact-change-date
     * @zm-api-field-description Change date
     */
    @XmlAttribute(name=MailConstants.A_CHANGE_DATE /* md */, required=false)
    private Long changeDate;

    /**
     * @zm-api-field-tag modified-seq-id
     * @zm-api-field-description Modified sequence ID
     */
    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=false)
    private Integer modifiedSequenceId;

    /**
     * @zm-api-field-tag contact-date
     * @zm-api-field-description Contact date
     */
    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private Long date;

    /**
     * @zm-api-field-tag contact-revision-id
     * @zm-api-field-description Revision ID
     */
    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revisionId;

    /**
     * @zm-api-field-tag contact-fileAs
     * @zm-api-field-description FileAs string for contact
     */
    @XmlAttribute(name=MailConstants.A_FILE_AS_STR /* fileAsStr */, required=false)
    private String fileAs;

    /**
     * @zm-api-field-tag contact-email-1
     * @zm-api-field-description Email address 1
     */
    @XmlAttribute(name="email" /* ContactConstants.A_email */, required=false)
    private String email;

    /**
     * @zm-api-field-tag contact-email-2
     * @zm-api-field-description Email address 2
     */
    @XmlAttribute(name="email2" /* ContactConstants.A_email2 */, required=false)
    private String email2;

    /**
     * @zm-api-field-tag contact-email-3
     * @zm-api-field-description Email address 3
     */
    @XmlAttribute(name="email3" /* ContactConstants.A_email3 */, required=false)
    private String email3;

    /**
     * @zm-api-field-tag contact-type
     * @zm-api-field-description Contact type
     * <br />
     * Default value is <b>"contact"</b>. If is present and set to <b>"group"</b>, then the contact is a
     * personal distribution list.
     */
    @XmlAttribute(name="type", required=false)
    private String type;

    /**
     * @zm-api-field-description dlist - retained for backwards compatibility
     */
    @XmlAttribute(name="dlist", required=false)
    private String dlist;

    // See GalSearchResultCallback.handleContact(Contact c)
    /**
     * @zm-api-field-tag contact-ref
     * @zm-api-field-description Reference
     */
    @XmlAttribute(name=AccountConstants.A_REF /* ref */, required=false)
    private String reference;

    /**
     * @zm-api-field-tag contact-group-too-many-members
     * @zm-api-field-description Flag whether there were too many members.
     * <br />
     * If the number of members on a GAL group is greater than the specified max, no members are returned and
     * this flag is set to <b>1 (true)</b> instead.
     */
    @XmlAttribute(name=MailConstants.A_TOO_MANY_MEMBERS /* tooManyMembers */, required=false)
    private ZmBoolean tooManyMembers;

    /**
     * @zm-api-field-description Custom metadata
     */
    @XmlElement(name=MailConstants.E_METADATA /* meta */, required=false)
    private List<AccountCustomMetadata> metadatas = Lists.newArrayList();

    /**
     * @zm-api-field-description Contact attributes
     */
    @XmlElement(name=MailConstants.E_A /* a */, required=false)
    private List<ContactAttr> attrs = Lists.newArrayList();

    /**
     * @zm-api-field-description Contact group members
     */
    @XmlElement(name=MailConstants.E_CONTACT_GROUP_MEMBER /* m */, required=false)
    private List<ContactGroupMember> contactGroupMembers = Lists.newArrayList();

    /**
     * @zm-api-field-tag isOwner
     * @zm-api-field-description For group entries, flags whether user is the owner of a group
     * (type=group in attrs on a <b>&lt;cn></b>).
     */
    @XmlAttribute(name=AccountConstants.A_IS_OWNER /* isOwner */, required=false)
    ZmBoolean isOwner;

    /**
     * @zm-api-field-tag isMember
     * @zm-api-field-description For group entries, flags whether user is a member of a group
     * (type=group in attrs on a <b>&lt;cn></b>).
     */
    @XmlAttribute(name=AccountConstants.A_IS_MEMBER /* isMember */, required=false)
    ZmBoolean isMember;


    public ContactInfo() {
    }

    public ContactInfo(String id) {
        this.id = id;
    }

    private ContactInfo(String sortField, String id) {
        this.sortField = sortField;
        this.id = id;
    }

    public static ContactInfo createForId(String id) {
        return new ContactInfo(id);
    }

    public static ContactInfo createForSortFieldAndId(String sortField, String id) {
        return new ContactInfo(sortField, id);
    }

    @Override
    public void setSortField(String sortField) { this.sortField = sortField; }
    @Override
    public void setCanExpand(Boolean canExpand) { this.canExpand = ZmBoolean.fromBool(canExpand); }
    @Override
    public void setId(String id) { this.id = id; }
    @Override
    public void setFolder(String folder) { this.folder = folder; }
    @Override
    public void setFlags(String flags) { this.flags = flags; }
    @Override
    public void setTags(String tags) { this.tags = tags; }
    @Override
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    @Override
    public void setChangeDate(Long changeDate) { this.changeDate = changeDate; }
    @Override
    public void setModifiedSequenceId(Integer modifiedSequenceId) {
        this.modifiedSequenceId = modifiedSequenceId;
    }
    @Override
    public void setDate(Long date) { this.date = date; }
    @Override
    public void setRevisionId(Integer revisionId) {
        this.revisionId = revisionId;
    }
    @Override
    public void setFileAs(String fileAs) { this.fileAs = fileAs; }
    @Override
    public void setEmail(String email) { this.email = email; }
    @Override
    public void setEmail2(String email2) { this.email2 = email2; }
    @Override
    public void setEmail3(String email3) { this.email3 = email3; }
    @Override
    public void setType(String type) { this.type = type; }
    @Override
    public void setDlist(String dlist) { this.dlist = dlist; }
    @Override
    public void setReference(String reference) { this.reference = reference; }
    @Override
    public void setTooManyMembers(Boolean tooManyMembers) {
        this.tooManyMembers = ZmBoolean.fromBool(tooManyMembers);
    }
    public void setMetadatas(Iterable <AccountCustomMetadata> metadatas) {
        this.metadatas.clear();
        if (metadatas != null) {
            Iterables.addAll(this.metadatas,metadatas);
        }
    }

    public void addMetadata(AccountCustomMetadata metadata) {
        this.metadatas.add(metadata);
    }

    @Override
    public void setAttrs(Iterable <ContactAttr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs,attrs);
        }
    }

    @Override
    public void addAttr(ContactAttr attr) {
        this.attrs.add(attr);
    }

    public void setContactGroupMembers(Iterable <ContactGroupMember> contactGroupMembers) {
        this.contactGroupMembers.clear();
        if (contactGroupMembers != null) {
            Iterables.addAll(this.contactGroupMembers,contactGroupMembers);
        }
    }

    public void addContactGroupMember(ContactGroupMember contactGroupMember) {
        this.contactGroupMembers.add(contactGroupMember);
    }

    public void setIsOwner(Boolean isOwner) {
        this.isOwner = ZmBoolean.fromBool(isOwner);
    }

    public void setIsMember(Boolean isMember) {
        this.isMember = ZmBoolean.fromBool(isMember);
    }

    @Override
    @GraphQLQuery(name=GqlConstants.SORT_FIELD, description="Sort field value")
    public String getSortField() { return sortField; }
    @Override
    @GraphQLQuery(name=GqlConstants.IS_EXPANDABLE, description="Denotes whether user can expand group members")
    public Boolean getCanExpand() { return ZmBoolean.toBool(canExpand); }
    @Override
    @GraphQLQuery(name=GqlConstants.ID, description="Unique contact ID")
    @GraphQLNonNull
    public String getId() { return id; }
    @Override
    @GraphQLQuery(name=GqlConstants.FOLDER_ID, description="The containing Folder ID")
    public String getFolder() { return folder; }
    @Override
    @GraphQLQuery(name=GqlConstants.FLAGS, description="(f)lagged, has (a)ttachment")
    public String getFlags() { return flags; }
    @Override
    @GraphQLIgnore
    public String getTags() { return tags; }
    @Override
    @GraphQLQuery(name=GqlConstants.TAG_NAMES, description="Comma-separated list of tag names")
    public String getTagNames() { return tagNames; }
    @Override
    @GraphQLQuery(name=GqlConstants.LAST_MODIFIED, description="Modified date in seconds")
    public Long getChangeDate() { return changeDate; }
    @Override
    @GraphQLQuery(name=GqlConstants.MODIFIED_SEQUENCE, description="Modified sequence")
    public Integer getModifiedSequenceId() { return modifiedSequenceId; }
    @Override
    @GraphQLQuery(name=GqlConstants.DATE, description="Date in milliseconds")
    public Long getDate() { return date; }
    @Override
    @GraphQLQuery(name=GqlConstants.REVISION, description="Saved sequence number")
    public Integer getRevisionId() { return revisionId; }
    @Override
    @GraphQLQuery(name=GqlConstants.FILE_AS, description="Current fileAs string for display/sorting purposes")
    public String getFileAs() { return fileAs; }
    @Override
    @GraphQLQuery(name=GqlConstants.EMAIL, description="Contact email address")
    public String getEmail() { return email; }
    @Override
    @GraphQLQuery(name=GqlConstants.EMAIL2, description="Contact email address 2")
    public String getEmail2() { return email2; }
    @Override
    @GraphQLQuery(name=GqlConstants.EMAIL3, description="Contact email address 3")
    public String getEmail3() { return email3; }
    @Override
    @GraphQLQuery(name=GqlConstants.TYPE, description="Contact type, Default value is \"contact\". If is present and set to \"group\", then the contact is a personal distribution list.")
    public String getType() { return type; }
    @Override
    @GraphQLQuery(name=GqlConstants.DLIST, description="Contact distribution list")
    public String getDlist() { return dlist; }
    @Override
    @GraphQLQuery(name=GqlConstants.REFERENCE, description="Global Address List entry reference")
    public String getReference() { return reference; }
    @Override
    @GraphQLQuery(name=GqlConstants.IS_TOO_MANY_MEMBERS, description="Denotes whether the number of entries on a GAL group exceeds the specified max")
    public Boolean getTooManyMembers() { return ZmBoolean.toBool(tooManyMembers); }

    @GraphQLQuery(name=GqlConstants.METADATAS, description="Custom metadata information")
    public List<AccountCustomMetadata> getMetadatas() {
        return metadatas;
    }
    @Override
    @GraphQLQuery(name=GqlConstants.ATTRIBUTES, description="Attributes")
    public List<ContactAttr> getAttrs() {
        return attrs;
    }

    @GraphQLQuery(name=GqlConstants.CONTACT_GROUP_MEMBERS, description="Contact group members")
    public List<ContactGroupMember> getContactGroupMembers() {
        return Collections.unmodifiableList(contactGroupMembers);
    }

    @GraphQLQuery(name=GqlConstants.IS_OWNER, description="For group entries, flags whether user is the owner of a group")
    public Boolean isOwner() {
        return ZmBoolean.toBool(isOwner);
    }

    @GraphQLQuery(name=GqlConstants.IS_MEMBER, description="For group entries, flags whether user is a member of a group ")
    public Boolean isMember() {
        return ZmBoolean.toBool(isMember);
    }

    // non-JAXB method
    @Override
    public void setMetadataInterfaces(
            Iterable<CustomMetadataInterface> metadatas) {
        this.metadatas = AccountCustomMetadata.fromInterfaces(metadatas);
    }

    // non-JAXB method
    @Override
    public void addMetadataInterfaces(CustomMetadataInterface metadata) {
        addMetadata((AccountCustomMetadata)metadata);
    }

    // non-JAXB method
    @Override
    @GraphQLIgnore
    public List<CustomMetadataInterface> getMetadataInterfaces() {
        return AccountCustomMetadata.toInterfaces(metadatas);
    }

    public static Iterable <ContactInfo> fromInterfaces(Iterable <ContactInterface> params) {
        if (params == null)
            return null;
        List <ContactInfo> newList = Lists.newArrayList();
        for (ContactInterface param : params) {
            newList.add((ContactInfo) param);
        }
        return newList;
    }

    public static List <ContactInterface> toInterfaces(Iterable <ContactInfo> params) {
        if (params == null)
            return null;
        List <ContactInterface> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return newList;
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("sortField", sortField)
            .add("canExpand", canExpand)
            .add("id", id)
            .add("folder", folder)
            .add("flags", flags)
            .add("tags", tags)
            .add("tagNames", tagNames)
            .add("changeDate", changeDate)
            .add("modifiedSequenceId", modifiedSequenceId)
            .add("date", date)
            .add("revisionId", revisionId)
            .add("fileAs", fileAs)
            .add("email", email)
            .add("email2", email2)
            .add("email3", email3)
            .add("type", type)
            .add("dlist", dlist)
            .add("reference", reference)
            .add("metadatas", metadatas)
            .add("attrs", attrs)
            .add("contactGroupMembers", contactGroupMembers);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }

    @Override
    public void setContactGroupMemberInterfaces(Iterable<ContactGroupMemberInterface> contactGroupMembers) {
        this.setContactGroupMembers(ContactGroupMember.fromInterfaces(contactGroupMembers));
    }

    @Override
    public void addContactGroupMember(ContactGroupMemberInterface contactGroupMember) {
        this.addContactGroupMember((ContactGroupMember) contactGroupMember);
    }

    @Override
    @GraphQLIgnore
    public List<ContactGroupMemberInterface> getContactGroupMemberInterfaces() {
        return ContactGroupMember.toInterfaces(contactGroupMembers);
    }
}
