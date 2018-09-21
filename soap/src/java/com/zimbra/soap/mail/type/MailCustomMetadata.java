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

package com.zimbra.soap.mail.type;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.CustomMetadataInterface;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_METADATA)
@GraphQLType(name=GqlConstants.CLASS_MAIL_CUSTOM_METADATA, description="Custom metadatas")
public class MailCustomMetadata
extends MailKeyValuePairs
implements CustomMetadataInterface {

    // Normally present but sometimes an empty element is created to show that
    // CustomMetadata info is present but there are no sections to report on.
    /**
     * @zm-api-field-tag section
     * @zm-api-field-description Section.
     * <br />
     * Normally present.  If absent this indicates that CustomMetadata info is present but there are no sections to
     * report on.
     */
    @XmlAttribute(name=MailConstants.A_SECTION /* section */, required=false)
    private String section;

    public MailCustomMetadata() {
    }

    @Override
    public void setSection(String section) { this.section = section; }
    @Override
    @GraphQLQuery(name=GqlConstants.SECTION, description="Section. If absent this indicates that CustomMetadata info is present but there are no sections to report on.")
    public String getSection() { return section; }

    public static List <MailCustomMetadata> fromInterfaces(Iterable <CustomMetadataInterface> params) {
        if (params == null) {
            return null;
        }
        final List <MailCustomMetadata> newList = Lists.newArrayList();
        for (final CustomMetadataInterface param : params) {
            newList.add((MailCustomMetadata) param);
        }
        return newList;
    }

    public static List <CustomMetadataInterface> toInterfaces(Iterable <MailCustomMetadata> params) {
        if (params == null) {
            return null;
        }
        final List <CustomMetadataInterface> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return Collections.unmodifiableList(newList);
    }

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("section", section);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
