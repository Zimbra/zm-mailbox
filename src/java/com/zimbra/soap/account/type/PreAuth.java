/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;


/*
<preauth timestamp="{timestamp}" expires="{expires}">{computed-preauth-value}</preauth>
 */
public class PreAuth {

    @XmlAttribute(required=true) private long timestamp;
    @XmlAttribute private Long expiresTimestamp;
    @XmlValue private String value;
    
    public long getTimestamp() { return timestamp; }
    public PreAuth setTimestamp(long timestamp) { this.timestamp = timestamp; return this; }
    
    public Long getExpiresTimestamp() { return expiresTimestamp; }
    public PreAuth setExpiresTimestamp(Long timestamp) { this.expiresTimestamp = timestamp; return this; }
    
    public String getValue() { return value; }
    public PreAuth setValue(String value) { this.value = value; return this; }
}
