package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.Objects;
import com.zimbra.common.soap.SmimeConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class Validity {

    /**
     * @zm-api-field-tag startDate
     * @zm-api-field-description The date the certificate is first valid from.Date is returned in String format.ex: Fri Sep 23 02:17:16 PDT 2016
     */
    @XmlElement(name=SmimeConstants.E_START_DATE, required=false)
    private String startDate;

    /**
     * @zm-api-field-tag endDate
     * @zm-api-field-description The expiration date.Date is returned in String format.ex: Fri Sep 23 02:17:16 PDT 2016
     */
    @XmlElement(name=SmimeConstants.E_END_DATE, required=false)
    private String endDate;

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.add("startDate", startDate)
            .add("endDate", endDate);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }

}
