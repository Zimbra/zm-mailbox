/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.formatter;

import java.util.List;

import com.zimbra.common.service.ServiceException;

@SuppressWarnings("serial")
public class FormatterServiceException extends ServiceException {
    // codes
    public static final String INVALID_FORMAT = "formatter.INVALID_FORMAT";
    public static final String INVALID_TYPE = "formatter.INVALID_TYPE";
    public static final String MISMATCHED_META = "formatter.MISMATCHED_META";
    public static final String MISMATCHED_SIZE = "formatter.MISMATCHED_SIZE";
    public static final String MISMATCHED_TYPE = "formatter.MISMATCHED_TYPE";
    public static final String MISSING_BLOB = "formatter.MISSING_BLOB";
    public static final String MISSING_META = "formatter.MISSING_META";
    public static final String MISSING_VCARD_FIELDS = "formatter.MISSING_VCARD_FIELDS";
    public static final String UNKNOWN_ERROR = "formatter.UNKNOWN_ERROR";

    // arguments
    public static final String FILE_NAME = "filename";
    public static final String ITEM_PATH = "path";
    public static final String ITEM_TYPE = "view";

    // Constructors
    FormatterServiceException(String message, String code,
        boolean isReceiversFault, Argument... args) {
        super(message, code, isReceiversFault, args);
    }

    FormatterServiceException(String message, String code,
        boolean isReceiversFault, Throwable cause, Argument... args) {
        super(message, code, isReceiversFault, cause, args);
    }
    
    FormatterServiceException(String message, String code,
        boolean isReceiversFault, Throwable cause, List<Argument> args) {
        super(message, code, isReceiversFault, cause, args);
    }

    // exception makers
    public static FormatterServiceException INVALID_FORMAT(String filename) {
        return new FormatterServiceException("invalid file format",
            INVALID_FORMAT, false, arg(FILE_NAME, filename));
    }

    public static FormatterServiceException INVALID_TYPE(String view,
        String path) {
        return new FormatterServiceException("folder cannot contain item type "
            + view, INVALID_TYPE, false, arg(ITEM_TYPE, view), arg(ITEM_PATH,
            path));
    }

    public static FormatterServiceException MISMATCHED_META(String path) {
        return new FormatterServiceException(
            "mismatched item content and meta", MISMATCHED_META, false, arg(
            ITEM_PATH, path));
    }

    public static FormatterServiceException MISMATCHED_SIZE(String path) {
        return new FormatterServiceException("ignored item data size mismatch",
            MISMATCHED_SIZE, false, arg(ITEM_PATH, path));
    }

    public static FormatterServiceException MISMATCHED_TYPE(String path) {
        return new FormatterServiceException(
            "cannot overwrite non matching data", MISMATCHED_TYPE, false, arg(
            ITEM_PATH, path));
    }

    public static FormatterServiceException MISSING_BLOB(String path) {
        return new FormatterServiceException("missing item blob for meta",
            MISSING_BLOB, false, arg(ITEM_PATH, path));
    }

    public static FormatterServiceException MISSING_META(String path) {
        return new FormatterServiceException(
            "item content missing meta information", MISSING_META, false, arg(
            ITEM_PATH, path));
    }

    public static FormatterServiceException MISSING_VCARD_FIELDS(String path) {
        return new FormatterServiceException(
            "no contact fields found in vcard", MISSING_VCARD_FIELDS, false,
            arg(ITEM_PATH, path));
    }

    public static FormatterServiceException UNKNOWN_ERROR(Throwable cause) {
        if (cause instanceof ServiceException)
            return WRAPPED_EXCEPTION((ServiceException) cause);
        return new FormatterServiceException(cause.getMessage() == null ?
            cause.toString() : cause.getMessage(), UNKNOWN_ERROR,
            true, cause);
    }

    public static FormatterServiceException UNKNOWN_ERROR(String path,
        Throwable cause) {
        if (cause instanceof ServiceException)
            return WRAPPED_EXCEPTION((ServiceException) cause);
        return new FormatterServiceException(cause.getMessage() == null ?
            cause.toString() : cause.getMessage(), UNKNOWN_ERROR,
            true, cause, arg(ITEM_PATH, path));
    }

    // convenience
    private static FormatterServiceException WRAPPED_EXCEPTION(
        ServiceException e) {
        return new FormatterServiceException(e.getMessage(), e.getCode(),
            e.isReceiversFault(), e.getCause(), e.getArgs());
    }

    private static Argument arg(String name, String value) {
        return new Argument(name, value, Argument.Type.STR);
    }
}
