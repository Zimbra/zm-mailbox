@XmlSchema(
    xmlns = {
        @XmlNs(prefix="mail", namespaceURI = "urn:zimbraMail")
    },
    namespace = "urn:zimbraMail",
    elementFormDefault = XmlNsForm.QUALIFIED
)
@XmlAccessorType(XmlAccessType.NONE)

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
