/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.type;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.SpecifyContact;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_MODIFY_CONTACT_SPEC)
public class ModifyContactSpec implements SpecifyContact<ModifyContactAttr, ModifyContactGroupMember> {

    // Used when modifying a contact
    /**
     * @zm-api-field-tag id
     * @zm-api-field-description ID - specified when modifying a contact
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private Integer id;


    /**
     * @zm-api-field-tag tag-names
     * @zm-api-field-description Comma-separated list of tag names
     */
    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    /**
     * @zm-api-field-description Contact attributes.  Cannot specify <b>&lt;vcard></b> as well as these
     */
    @XmlElement(name=MailConstants.E_ATTRIBUTE /* a */, required=false)
    @GraphQLInputField(name=GqlConstants.ATTRIBUTES, description="Cannot specify vcard as well as these")
    private final List<ModifyContactAttr> attrs = Lists.newArrayList();

    /**
     * @zm-api-field-description Contact group members.  Valid only if the contact being created is a contact group
     * (has attribute type="group")
     */
    @XmlElement(name=MailConstants.E_CONTACT_GROUP_MEMBER /* m */, required=false)
    @GraphQLInputField(name=GqlConstants.CONTACT_GROUP_MEMBERS, description="Contact group members.")
    private final List<ModifyContactGroupMember> contactGroupMembers = Lists.newArrayList();

    public ModifyContactSpec() {
    }

    public static ModifyContactSpec createForId(Integer id) {
        final ModifyContactSpec spec = new ModifyContactSpec();
        spec.setId(id);
        return spec;
    }

    @Override
    @GraphQLInputField(name=GqlConstants.ID, description="Id of the contact to modify")
    public void setId(@GraphQLNonNull Integer id) { this.id = id; }
    @Override
    @GraphQLInputField(name=GqlConstants.TAG_NAMES, description="Comma-separated list of tag names")
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    @Override
    @GraphQLInputField(name=GqlConstants.ATTRIBUTES, description="Cannot specify vcard as well as these")
    public void setAttrs(Iterable <ModifyContactAttr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs, attrs);
        }
    }

    @Override
    @GraphQLIgnore
    public void addAttr(ModifyContactAttr attr) {
        this.attrs.add(attr);
    }

    @Override
    @GraphQLInputField(name=GqlConstants.CONTACT_GROUP_MEMBERS, description="Contact group members.")
    public void setContactGroupMembers(Iterable <ModifyContactGroupMember> contactGroupMembers) {
        this.contactGroupMembers.clear();
        if (contactGroupMembers != null) {
            Iterables.addAll(this.contactGroupMembers, contactGroupMembers);
        }
    }

    @Override
    @GraphQLIgnore
    public void addContactGroupMember(ModifyContactGroupMember contactGroupMember) {
        this.contactGroupMembers.add(contactGroupMember);
    }

    @Override
    public Integer getId() { return id; }
    @Override
    public String getTagNames() { return tagNames; }
    @Override
    public List<ModifyContactAttr> getAttrs() {
        return attrs;
    }
    @Override
    public List<ModifyContactGroupMember> getContactGroupMembers() {
        return contactGroupMembers;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("tagNames", tagNames)
            .add("attrs", attrs)
            .add("contactGroupMembers", contactGroupMembers);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

    @Override
    public ModifyContactAttr addAttrWithName(String name) {
        final ModifyContactAttr mca = new ModifyContactAttr(name);
        addAttr(mca);
        return mca;
    }

    @Override
    public ModifyContactAttr addAttrWithNameAndValue(String name, String value) {
        final ModifyContactAttr mca = ModifyContactAttr.fromNameAndValue(name, value);
        addAttr(mca);
        return mca;
    }

    @Override
    public ModifyContactGroupMember addContactGroupMemberWithTypeAndValue(String type, String value) {
        final ModifyContactGroupMember mcgm = ModifyContactGroupMember.createForTypeAndValue(type, value);
        addContactGroupMember(mcgm);
        return mcgm;
    }
}
