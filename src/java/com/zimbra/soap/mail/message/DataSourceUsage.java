package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class DataSourceUsage {

    public DataSourceUsage() {}

    @XmlAttribute(name=MailConstants.A_ID, required=true)
    private String id;

    @XmlAttribute(name=MailConstants.A_DS_USAGE, required=true)
    private Long usage;

    public void setId(String id) { this.id = id; }
    public String getId() { return id; }

    public void setUsage(Long usage) { this.usage = usage; }
    public Long getUsage() { return usage; }
}
