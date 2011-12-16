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

package com.zimbra.doc.soap.doclet;

import java.util.Map;

import com.google.common.collect.Maps;

import com.zimbra.doc.soap.ApiClassDocumentation;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

public class DocletApiListener {

    /**
     * Use for a general description of the SOAP Command.  Use in JAXB class for a Request.
     */
    public static final String TAG_COMMAND_DESCRIPTION = "@zm-api-command-description";
    /**
     * Use to flag that a command is part of Network Edition
     */
    public static final String TAG_COMMAND_NETWORK_ONLY = "@zm-api-command-network-edition";
    /**
     * Use for description of the SOAP Request.  Use in JAXB class for a Request.
     */
    public static final String TAG_COMMAND_REQUEST_DESCRIPTION = "@zm-api-request-description";
    /**
     * Use for description of the SOAP Response.  Use in JAXB class for a Response.
     */
    public static final String TAG_COMMAND_RESPONSE_DESCRIPTION = "@zm-api-response-description";
    /**
     * Use for description of a field related to an element, attribute or element value.
     * Place near the field (or method where appropriate) containing the JAXB annotation.
     */
    public static final String TAG_FIELD_DESCRIPTION = "@zm-api-field-description";
    /**
     * Use for references in documentation to the value of a field.
     * For instance "@zm-api-field-tag thing-name" would result in :
     *     <SetThingNameRequest name="{thing-name}" />
     * instead of, say
     *     <SetThingNameRequest name="String" />
     */
    public static final String TAG_FIELD_TAG = "@zm-api-field-tag";

    /**
     * Maps Class names to related documentation
     */
    private Map<String,ApiClassDocumentation> docMap = Maps.newHashMap();

    public DocletApiListener() {
    }

    public void processJavadocResults(RootDoc rootDoc) {
        for (ClassDoc classDoc : rootDoc.classes()) {
            processClass(classDoc);
        }
    }

    private void processClass(ClassDoc classDoc) {
        ApiClassDocumentation doc = new ApiClassDocumentation();
        processClassTags(doc, classDoc.tags());
        for (FieldDoc fieldDoc : classDoc.fields()) {
            processFieldTags(doc, fieldDoc);
        }
        for (MethodDoc methodDoc : classDoc.methods()) {
            processMethodTags(doc, methodDoc);
        }
        if (doc.hasDocumentation()) {
            docMap.put(classDoc.toString(), doc);
        }
    }

    private void processClassTags(ApiClassDocumentation doc, Tag[] tags) {
        for (Tag tag: tags) {
            if (TAG_COMMAND_DESCRIPTION.equals(tag.name())) {
                doc.setCommandDescription(tag.text());
            } else if (TAG_COMMAND_REQUEST_DESCRIPTION.equals(tag.name())) {
                doc.setClassDescription(tag.text());
            } else if (TAG_COMMAND_NETWORK_ONLY.equals(tag.name())) {
                doc.setClassDescription(tag.text());
            } else if (TAG_COMMAND_RESPONSE_DESCRIPTION.equals(tag.name())) {
                doc.setClassDescription(tag.text());
            }
        }
    }

    private void processFieldTags(ApiClassDocumentation doc, FieldDoc fieldDoc) {
        for (Tag tag: fieldDoc.tags()) {
            if (TAG_FIELD_DESCRIPTION.equals(tag.name())) {
                doc.addFieldDescription(fieldDoc.name(), tag.text());
            } else if (TAG_FIELD_TAG.equals(tag.name())) {
                doc.addFieldTag(fieldDoc.name(), tag.text());
            }
        }
    }

    private void processMethodTags(ApiClassDocumentation doc, MethodDoc methodDoc) {
        for (Tag tag: methodDoc.tags()) {
            String fieldName = guessFieldNameFromGetterOrSetter(methodDoc.name());
            if (fieldName == null) {
                continue;
            }
            if (TAG_FIELD_DESCRIPTION.equals(tag.name())) {
                doc.addFieldDescription(fieldName, tag.text());
            } else if (TAG_FIELD_TAG.equals(tag.name())) {
                doc.addFieldTag(fieldName, tag.text());
            }
        }
    }

    private String guessFieldNameFromGetterOrSetter(String methodName) {
        String fieldName = null;
        if ((methodName.startsWith("set")) || (methodName.startsWith("get"))) {
            fieldName = methodName.substring(3,4).toLowerCase() + methodName.substring(4);
        } else if (methodName.startsWith("is")) {
            fieldName = methodName.substring(2,3).toLowerCase() + methodName.substring(3);
        }
        return fieldName;
    }

    public Map<String,ApiClassDocumentation> getDocMap() {
        return docMap;
    }

}
