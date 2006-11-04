/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import com.zimbra.cs.service.ServiceException;

import java.util.Arrays;
import java.util.Map;

/**
 * @author schemers
 */
public class DataSource extends NamedEntry implements Comparable {

    public enum Type {
        pop3;
        
        public static Type fromString(String s) throws ServiceException {
            try {
                return Type.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid type: "+s+", valid values: "+Arrays.asList(Type.values()), e); 
            }
        }
    };
    
    private String mName;
    private Type mType;

    public DataSource(Type type, String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs, null);
        mType = type;
    }
    
    public Type getType() {
        return mType;
    }

}


