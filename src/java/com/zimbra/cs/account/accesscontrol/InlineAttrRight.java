/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;

public class InlineAttrRight extends AttrRight {

    private static final String PARTS_SEPARATOR = "\\.";
    private static final String OP_GET = "get";
    private static final String OP_SET = "set";
    
    static InlineAttrRight newInlineAttrRight(String right) throws ServiceException {
        String[] parts = right.split(PARTS_SEPARATOR);
        if (parts.length != 3)
            throw ServiceException.PARSE_ERROR("inline attr right might have 3 parts", null);
        
        RightType rightType;
        if (OP_GET.equals(parts[0]))
            rightType = RightType.getAttrs;
        else if (OP_SET.equals(parts[0]))
            rightType = RightType.setAttrs;
        else
            throw ServiceException.PARSE_ERROR("invalid op for inline attr right: " + parts[0], null);
        
        TargetType targetType = TargetType.fromCode(parts[1]);
        if (targetType == TargetType.global)
            throw ServiceException.PARSE_ERROR("target type for inline attr right cannot be: " + parts[1], null);
        
        String attrName = parts[2];
        
        // use the {op}.{target-type}.{attr-name} string as the right name and description
        InlineAttrRight iar = new InlineAttrRight(right, rightType);
        iar.setDesc(right);
        iar.setTargetType(targetType);
        iar.validateAttr(attrName);
        iar.addAttr(attrName);
        iar.completeRight();
        
        return iar;
    }
    
    static boolean looksLikeOne(String right) {
        if (right.contains("."))
            return true;
        else 
            return false;
    }
    
    private InlineAttrRight(String name, RightType rightType) {
        super(name, rightType);
    }
    
    @Override
    boolean isTheSameRight(Right other) {
        return getName().equals(other.getName());
    }
}
