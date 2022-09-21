/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
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

package com.zimbra.soap.mail.message;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_APPOINTMENT_IDS_SINCE_RESPONSE)
public class GetAppointmentIdsSinceResponse {
    @XmlElement(name=MailConstants.A_MODIFIED_IDS /* mids */, required=false)
    private List<Integer> mids;
    @XmlElement(name=MailConstants.A_DELETED_IDS /* dids */, required=false)
    private List<Integer> dids;

    public GetAppointmentIdsSinceResponse() {
        this.mids = null;
        this.dids = null;
    }

    public GetAppointmentIdsSinceResponse(List<Integer> mids, List<Integer> dids) {
        this.mids = mids;
        this.dids = dids;
    }

    /**
     * @return the mids
     */
    public List<Integer> getMids() {
        return mids;
    }

    /**
     * @param mids the mids to set
     */
    public void setMids(List<Integer> mids) {
        this.mids = new ArrayList<Integer>();
        this.mids.addAll(mids);
    }

    /**
     * @param mid the mid to add
     */
    public void addMids(Integer mid) {
        if (this.mids == null) {
            this.mids = new ArrayList<Integer>();
        }
        this.mids.add(mid);
    }

    /**
     * @return the dids
     */
    public List<Integer> getDids() {
        return dids;
    }

    /**
     * @param dids the dids to set
     */
    public void setDids(List<Integer> dids) {
        this.dids = new ArrayList<Integer>();
        this.dids.addAll(dids);
    }

    /**
     * @param did the did to add
     */
    public void addDids(Integer did) {
        if (this.dids == null) {
            this.dids = new ArrayList<Integer>();
        }
        this.dids.add(did);
    }
}
