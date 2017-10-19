package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class RelatedContactResult {

    public RelatedContactResult() {}

    public RelatedContactResult(String email) {
        setEmail(email);
    }

    /**
     * @zm-api-field-description The related contact's email address.
     */
    @XmlAttribute(name=MailConstants.A_EMAIL, required=true)
    private String email;

    /**
     * @zm-api-field-description The related contact's name, if available.
     */
    @XmlAttribute(name=MailConstants.A_PERSONAL, required=false)
    private String name;

    /**
     * @zm-api-field-description Flag specifying the scope of affinity
     * for this contact. The higher the number, the wider the scope.
     */
    @XmlAttribute(name=MailConstants.A_AFFINITY_SCOPE, required=false)
    private int scope;

    public int getScope() { return scope; }
    public void setScope(int scope) { this.scope = scope; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
