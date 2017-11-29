package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class RelatedContactsTarget {

    public RelatedContactsTarget(String targetEmail, String affinity) {
        setTargetEmail(targetEmail);
        setAffinity(affinity);
    }

    public RelatedContactsTarget() {}

    /**
     * @zm-api-field-description The recipient field of the target.
     * Options are "to", "cc", "bcc", or unset.
     */
    @XmlAttribute(name=MailConstants.A_TARGET_AFFINITY_FIELD, required=false)
    private String affinity;

    /**
     * @zm-api-field-description The target email address.
     */
    @XmlElement(name=MailConstants.E_CONTACT, type=String.class)
    private String targetEmail;


    public void setAffinity(String affinity) { this.affinity = affinity; }
    public String getAffinity() { return affinity; }

    public void setTargetEmail(String email) { this.targetEmail = email; }
    public String getTargetEmail() { return targetEmail; }
}
