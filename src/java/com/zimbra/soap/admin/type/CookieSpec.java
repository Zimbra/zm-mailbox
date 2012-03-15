package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

public class CookieSpec {

   /*
    * @zm-api-field-description Cookie name
    */
   @XmlAttribute(name=AdminConstants.A_NAME, required=true)
   private String name;
   
   /**
    * no-argument constructor wanted by JAXB
    */
   @SuppressWarnings("unused")
   private CookieSpec() {
       this((String) null);
   }
   
   public CookieSpec(String name) {
       this.name = name;
   }
   
   public String getName() {
       return name;
   }
}
