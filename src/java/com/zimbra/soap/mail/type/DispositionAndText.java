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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.MailConstants;
import org.codehaus.jackson.annotate.JsonProperty;

@XmlAccessorType(XmlAccessType.NONE)
public class DispositionAndText {

    /**
     * @zm-api-field-tag disposition
     * @zm-api-field-description Disposition.  Sections of text that are identical to both versions are indicated with
     * <b>disp="common"</b>.  For each conflict the chunk will show <b>disp="first"</b> or <b>disp="second"</b>
     */
    @XmlAttribute(name=MailConstants.A_DISP /* disp */, required=false)
    private String disposition;

    /**
     * @zm-api-field-tag text
     * @zm-api-field-description Text
     */
    @JsonProperty(JSONElement.A_CONTENT)
    @XmlValue
    private String text;

    public DispositionAndText() {
    }

    public DispositionAndText(String disp, String txt) {
        setDisposition(disp);
        setText(txt);
    }

    public static DispositionAndText create(String disp, String txt) {
        return new DispositionAndText(disp, txt);
    }

    public void setDisposition(String disposition) {
        this.disposition = disposition;
    }
    public void setText(String text) { this.text = text; }
    public String getDisposition() { return disposition; }
    public String getText() { return text; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("disposition", disposition)
            .add("text", text);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
