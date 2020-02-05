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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_APPOINTMENT_IDS_IN_RANGE_RESPONSE)
public class GetAppointmentIdsInRangeResponse {
    @XmlAccessorType(XmlAccessType.NONE)
    public static class AppointmentIdAndDate {
        @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
        private String id;
        @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
        private Integer date;
        public AppointmentIdAndDate() {
            this(null, null);
        }
        public AppointmentIdAndDate(String id, Integer date) {
            this.id = id;
            this.date = date;
        }
        /**
         * @return the id
         */
        public String getId() {
            return id;
        }
        /**
         * @param id the id to set
         */
        public void setId(String id) {
            this.id = id;
        }
        /**
         * @return the date
         */
        public Integer getDate() {
            return date;
        }
        /**
         * @param date the date to set
         */
        public void setDate(Integer date) {
            this.date = date;
        }
    }
    @XmlElement(name=MailConstants.E_APPOINTMENT_DATA /* apptData */, required=false)
    private List<AppointmentIdAndDate> appointmentData;

    public GetAppointmentIdsInRangeResponse() {
        this.appointmentData = null;
    }

    public GetAppointmentIdsInRangeResponse(List<AppointmentIdAndDate> appointmentData) {
        this.appointmentData = appointmentData;
    }
    /**
     * @return the appointmentData
     */
    public List<AppointmentIdAndDate> getAppointmentData() {
        return appointmentData;
    }

    /**
     * @param appointmentData the appointmentData to set
     */
    public void setAppointmentData(List<AppointmentIdAndDate> appointmentData) {
        this.appointmentData = new ArrayList<AppointmentIdAndDate>();
        this.appointmentData.addAll(appointmentData);
    }

    /**
     * @param appointmentData the appointmentData to set
     */
    public void addAppointmentData(AppointmentIdAndDate appointmentData) {
        if (this.appointmentData == null) {
            this.appointmentData = new ArrayList<AppointmentIdAndDate>();
        }
        this.appointmentData.add(appointmentData);
    }
}
