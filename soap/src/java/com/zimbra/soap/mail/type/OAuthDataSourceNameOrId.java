package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.NONE)
public class OAuthDataSourceNameOrId extends DataSourceNameOrId {

    public static OAuthDataSourceNameOrId createForName(String name) {
        OAuthDataSourceNameOrId obj = new OAuthDataSourceNameOrId();
        obj.setName(name);
        return obj;
    }

    public static OAuthDataSourceNameOrId createForId(String id) {
        OAuthDataSourceNameOrId obj = new OAuthDataSourceNameOrId();
        obj.setId(id);
        return obj;
    }
}
