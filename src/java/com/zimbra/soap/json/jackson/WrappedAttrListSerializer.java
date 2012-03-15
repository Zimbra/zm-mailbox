/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.json.jackson;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import com.zimbra.soap.base.KeyAndValue;

/**
 * JsonSerializer to be used when have a wrapped list of attrs.
 * e.g.
 *     @XmlElementWrapper(name=AccountConstants.E_PREFS, required=false)
 *     @XmlElement(name=AccountConstants.E_PREF, required=false)
 *     @JsonSerialize(using=WrappedAttrListSerializer.class)
 *     private List<Pref> prefs = Lists.newArrayList();
 * gives something similar to :
 *     "prefs" : {
 *      "_attrs" : {
 *        "zimbraPrefMailFlashTitle" : "FALSE",
 *        "zimbraPrefIMToasterEnabled" : "FALSE"
 *     }
 * 
 * TODO: For duplicate keys, Getting :
 *          "zimbraZimletAvailableZimlets" : "+com_zimbra_email",
 *          "zimbraZimletAvailableZimlets" : "+com_zimbra_url",
 *       Want       
 *          "zimbraZimletAvailableZimlets": [
 *          "+com_zimbra_email",
 *          "+com_zimbra_url"],
 */
public class WrappedAttrListSerializer extends JsonSerializer<List<KeyAndValue>>{

    @Override
    public void serialize(List<KeyAndValue> pairs, JsonGenerator jgen,
            SerializerProvider provider) throws IOException,
            JsonProcessingException {
        if (pairs == null) {
            return;
        }
        jgen.writeStartObject();
        jgen.writeObjectFieldStart("_attrs");
        for (KeyAndValue pair : pairs) {
            jgen.writeStringField(pair.getKey(), pair.getValue());
        }
        jgen.writeEndObject();
    }
}
