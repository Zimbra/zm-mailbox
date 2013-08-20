/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap.upgrade;

import java.util.Arrays;

import com.zimbra.cs.account.FileGenUtil;
import com.zimbra.cs.account.Entry.EntryType;


public class Description {
    private UpgradeOp op;
    private String[] attrs;
    private EntryType[] entryTypes;
    private String oldValue;
    private String newValue;
    private String notes;
    
    Description(UpgradeOp op, String[] attrs, EntryType[] entryTypes,
            String oldValue, String newValue, String notes) {
        this.op = op;
        this.attrs = attrs;
        this.entryTypes = entryTypes;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.notes = notes;
    }
    
    void describe() {
        LdapUpgradePrinter printer = op.printer;
        
        printer.format("Bug %s\n", op.getBug());
        
        if (attrs != null) {
            printer.format("Attrs: %s\n", Arrays.deepToString(attrs));
        }
        
        if (entryTypes != null) {
            printer.format("Entry types: %s\n", Arrays.deepToString(entryTypes));
        }
        
        printer.format("From value: %s\n", oldValue == null ? "" : oldValue);
        printer.format("To value: %s\n", newValue == null ? "" : newValue);
        
        if (notes != null) {
            // printer.format("Notes: %s\n", notes);
            String formattedNotes = FileGenUtil.wrapComments((notes==null?"":notes), 70, "    ");
            printer.format("Notes:\n%s\n", formattedNotes);
        }
        
        printer.println();
    }
}
