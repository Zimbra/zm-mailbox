/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.account.type.AddressListInfo;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ALL_ADDRESS_LISTS_RESPONSE)
public class GetAllAddressListsResponse {

    /**
     * @zm-api-field-description Information about address lists
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AccountConstants.E_ADDRESS_LISTS /* addressLists */, required=false)
    @XmlElement(name=AccountConstants.E_ADDRESS_LIST /* addressList */, required=false)
    private List<AddressListInfo> addressLists = Lists.newArrayList();

    public GetAllAddressListsResponse() {
    }

    public void setAddressLists(Iterable <AddressListInfo> addressLists) {
        this.addressLists.clear();
        if (addressLists != null) {
            Iterables.addAll(this.addressLists, addressLists);
        }
    }

    public GetAllAddressListsResponse addAddressList(AddressListInfo addressList) {
        this.addressLists.add(addressList);
        return this;
    }

    public List<AddressListInfo> getAddressLists() {
        return Collections.unmodifiableList(addressLists);
    }
}
