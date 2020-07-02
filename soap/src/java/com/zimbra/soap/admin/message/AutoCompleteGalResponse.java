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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.admin.type.ContactInfo;
import com.zimbra.soap.base.ContactInterface;
import com.zimbra.soap.base.AutoCompleteGalInterface;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_AUTO_COMPLETE_GAL_RESPONSE)
public class AutoCompleteGalResponse implements AutoCompleteGalInterface {

    /**
     * @zm-api-field-tag more-flag
     * @zm-api-field-description Set to 1 if the results were truncated
     */
    @XmlAttribute(name=MailConstants.A_QUERY_MORE /* more */, required=false)
    private ZmBoolean more;

    // TODO:Is this actually set anywhere in the server?
    /**
     * @zm-api-field-tag tokenize-key-op
     * @zm-api-field-description Either "and" or "or" (if present)
     * <br />
     * <ul>
     * <li> Not present if the search key was not tokenized.
     * <li> Some clients backtrack on GAL results assuming the results of a more specific key is the subset of a
     *      more generic key, and it checks cached results instead of issuing another SOAP request to the server.  If
     *      search key was tokenized and expanded with AND or OR, this cannot be assumed.
     * </ul>
     */
    @XmlAttribute(name=AccountConstants.A_TOKENIZE_KEY /* tokenizeKey */, required=false)
    private ZmBoolean tokenizeKey;

    /**
     * @zm-api-field-description Flag if pagination is supported
     */
    @XmlAttribute(name=AccountConstants.A_PAGINATION_SUPPORTED /* paginationSupported */, required=false)
    private ZmBoolean pagingSupported;

    /**
     * @zm-api-field-description Contacts matching the autocomplete request
     */
    @XmlElement(name=MailConstants.E_CONTACT /* cn */, required=false)
    private List<ContactInfo> contacts = Lists.newArrayList();

    // Believe that GalSearchResultCallback.handleDeleted(ItemId id) is not used for AutoCompleteGal
    // If it were used - it would matter what order as it would probably be a list of Id

    public AutoCompleteGalResponse() {
    }

    private AutoCompleteGalResponse(Boolean more, Boolean tokenizeKey) {
        this.setMore(more);
        this.setTokenizeKey(tokenizeKey);
    }

    public static AutoCompleteGalResponse createForMoreAndTokenizeKey(Boolean more, Boolean tokenizeKey) {
        return new AutoCompleteGalResponse(more, tokenizeKey);
    }

    @Override
    public void setMore(Boolean more) { this.more = ZmBoolean.fromBool(more); }
    @Override
    public void setTokenizeKey(Boolean tokenizeKey) { this.tokenizeKey = ZmBoolean.fromBool(tokenizeKey); }
    @Override
    public void setPagingSupported(Boolean pagingSupported) {
        this.pagingSupported = ZmBoolean.fromBool(pagingSupported);
    }

    public void setContacts(Iterable <ContactInfo> contacts) {
        this.contacts.clear();
        if (contacts != null) {
            Iterables.addAll(this.contacts,contacts);
        }
    }

    public void addContact(ContactInfo contact) {
        this.contacts.add(contact);
    }

    @Override
    public Boolean getMore() { return ZmBoolean.toBool(more); }
    @Override
    public Boolean getTokenizeKey() { return ZmBoolean.toBool(tokenizeKey); }
    @Override
    public Boolean getPagingSupported() { return ZmBoolean.toBool(pagingSupported); }
    public List<ContactInfo> getContacts() {
        return contacts;
    }

    @Override
    public List<ContactInterface> getContactInterfaces() {
        return Collections.unmodifiableList(ContactInfo.toInterfaces(contacts));
    }

    @Override
    public void setContactInterfaces(
            Iterable<ContactInterface> contacts) {
        setContacts(ContactInfo.fromInterfaces(contacts));
    }

    @Override
    public void addContactInterface(ContactInterface contact) {
        addContact((ContactInfo) contact);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("more", more)
            .add("tokenizeKey", tokenizeKey)
            .add("pagingSupported", pagingSupported)
            .add("contacts", contacts);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
