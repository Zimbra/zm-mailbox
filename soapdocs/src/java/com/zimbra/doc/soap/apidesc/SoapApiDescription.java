/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.doc.soap.apidesc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.doc.soap.Command;
import com.zimbra.doc.soap.Root;
import com.zimbra.doc.soap.ValueDescription;
import com.zimbra.doc.soap.XmlAttributeDescription;
import com.zimbra.doc.soap.XmlElementDescription;
import com.zimbra.soap.JaxbUtil;

public class SoapApiDescription {
    private String buildVersion;
    private String buildDate;
    private final List<SoapApiCommand> commands = Lists.newArrayList();
    private final List<SoapApiType> types = Lists.newArrayList();

    /* no-argument constructor needed for deserialization */
    @SuppressWarnings("unused")
    private SoapApiDescription () {
    }

    public SoapApiDescription (String version, String date) {
        buildVersion = getMajorVersion(version);
        buildDate = date;
    }

    /**
     * Gets the major version component (for example, "8.02") of the build version.
     * 
     * @param version the version
     * @return the major version component
     */
    private static String getMajorVersion(String version) {
        if (version != null) {
            String[] tokens = version.split("\\_");
            if (tokens != null && tokens.length > 0)
                return tokens[0];
        }
        return version;
    }

    public String getBuildVersion() { return buildVersion; }
    public String getBuildDate() { return buildDate; }

    public List<SoapApiCommand> getCommands() { return commands; }
    public void addCommand(SoapApiCommand command) {
        commands.add(command);
    }

    public void build(Root soapApiDataModelRoot) {
        Map<String,SoapApiType> typesMap = Maps.newTreeMap();
        for (Command cmd : soapApiDataModelRoot.getAllCommands()) {
            addCommand(new SoapApiCommand(cmd));
            for (XmlElementDescription elemDesc : cmd.getAllElements()) {
                if (!elemDesc.isJaxbType()) {
                    continue;
                }
                Class<?> jaxbClass = elemDesc.getJaxbClass();
                if ((jaxbClass != null) && (!typesMap.containsKey(jaxbClass.getName()))) {
                    typesMap.put(jaxbClass.getName(), new SoapApiType(elemDesc));
                }
                SoapApiType valueType = getJaxbSoapApiType(elemDesc.getValueType());
                if (valueType != null) {
                    if ((!typesMap.containsKey(valueType.getClassName()))) {
                        typesMap.put(valueType.getClassName(), valueType);
                    }
                }
                for (XmlAttributeDescription attr : elemDesc.getAttribs()) {
                    valueType = getJaxbSoapApiType(attr.getValueDescription());
                    if (valueType != null) {
                        if ((!typesMap.containsKey(valueType.getClassName()))) {
                            typesMap.put(valueType.getClassName(), valueType);
                        }
                    }
                }
            }
        }
        types.addAll(typesMap.values());
        Collections.sort(commands);
    }

    private SoapApiType getJaxbSoapApiType(ValueDescription valueType) {
        if (valueType != null) {
            Class<?> klass;
            try {
                klass = Class.forName(valueType.getClassName());
                if (JaxbUtil.isJaxbType(klass)) {
                    return new SoapApiType(klass);
                }
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    public static SoapApiDescription deserializeFromJson(InputStream in)
    throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(in, SoapApiDescription.class);
    }

    public static SoapApiDescription deserializeFromJson(File inFile)
    throws JsonParseException, JsonMappingException, IOException {
        String fname = inFile.getName();
        if ((fname != null) && (fname.endsWith(".gz"))) {
            FileInputStream fis = new FileInputStream(inFile);
            GZIPInputStream gis = new GZIPInputStream(fis);
            return deserializeFromJson(gis);
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inFile, SoapApiDescription.class);
    }

    public void serializeToJson(File outFile)
    throws JsonGenerationException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL); // no more null-valued properties
        mapper.setSerializationConfig(mapper.getSerializationConfig().with(SerializationConfig.Feature.INDENT_OUTPUT));
        mapper.writeValue(outFile, this);
    }

    public List<SoapApiType> getTypes() { return types; }
}
