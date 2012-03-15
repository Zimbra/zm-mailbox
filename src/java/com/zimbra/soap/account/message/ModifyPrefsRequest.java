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

package com.zimbra.soap.account.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.Pref;

/**
 * @zm-api-command-description Modify Preferences
 * <br />
 * Notes:
 * <br />
 * For multi-value prefs, just add the same attribute with 'n' different values:
 * <pre>
 *      &lt;ModifyPrefsRequest>
 *          &lt;pref name="foo">value1&lt;/pref>
 *          &lt;pref name="foo">value2&lt;/pref>
 *          .
 *          .
 *      &lt;/ModifyPrefsRequest>
 * </pre>
 * <br />
 * You can also add/subtract single values to/from a multi-value pref by prefixing the preference name with 
 * a '+' or '-', respectively in the same way you do when using zmprov. For example:
 * <pre>
 *      &lt;ModifyPrefsRequest>
 *          &lt;pref name="+foo">value1&lt;/pref>
 *          &lt;pref name="-foo">value2&lt;/pref>
 *          .
 *          .
 *      &lt;/ModifyPrefsRequest>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_MODIFY_PREFS_REQUEST)
public class ModifyPrefsRequest {

    /**
     * @zm-api-field-description Specify the preferences to be modified
     */
    @XmlElement(name=AccountConstants.E_PREF, required=false)
    private List<Pref> prefs = Lists.newArrayList();

    public ModifyPrefsRequest() {
    }

    public void setPrefs(Iterable <Pref> prefs) {
        this.prefs.clear();
        if (prefs != null) {
            Iterables.addAll(this.prefs,prefs);
        }
    }

    public ModifyPrefsRequest addPref(Pref pref) {
        this.prefs.add(pref);
        return this;
    }

    public List<Pref> getPrefs() {
        return Collections.unmodifiableList(prefs);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("prefs", prefs)
            .toString();
    }
}
