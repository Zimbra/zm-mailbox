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

package com.zimbra.client;

import com.google.common.base.Function;
import com.google.common.collect.BiMap;
import com.google.common.collect.EnumBiMap;
import com.zimbra.common.account.Key;
import com.zimbra.soap.account.type.Identity;
import com.zimbra.soap.account.type.Signature;
import com.zimbra.soap.mail.type.Folder;
import com.zimbra.soap.type.AccountBy;


/**
 * Converts between {@code com.zimbra.soap} objects and {@code com.zimbra.client} objects.
 */
public class SoapConverter {
    
    private static final BiMap<Folder.View, ZFolder.View> VIEW_MAP;
    private static final BiMap<AccountBy, Key.AccountBy> ACCOUNT_BY_MAP;
    
    static {
        VIEW_MAP = EnumBiMap.create(Folder.View.class, ZFolder.View.class);
        VIEW_MAP.put(Folder.View.UNKNOWN, ZFolder.View.unknown);
        VIEW_MAP.put(Folder.View.APPOINTMENT, ZFolder.View.appointment);
        VIEW_MAP.put(Folder.View.CHAT, ZFolder.View.chat);
        VIEW_MAP.put(Folder.View.CONTACT, ZFolder.View.contact);
        VIEW_MAP.put(Folder.View.CONVERSATION, ZFolder.View.conversation);
        VIEW_MAP.put(Folder.View.DOCUMENT, ZFolder.View.document);
        VIEW_MAP.put(Folder.View.MESSAGE, ZFolder.View.message);
        VIEW_MAP.put(Folder.View.REMOTE_FOLDER, ZFolder.View.remote);
        VIEW_MAP.put(Folder.View.SEARCH_FOLDER, ZFolder.View.search);
        VIEW_MAP.put(Folder.View.TASK, ZFolder.View.task);
        
        ACCOUNT_BY_MAP = EnumBiMap.create(AccountBy.class, Key.AccountBy.class);
        ACCOUNT_BY_MAP.put(AccountBy.name , Key.AccountBy.name);
        ACCOUNT_BY_MAP.put(AccountBy.id, Key.AccountBy.id);
        ACCOUNT_BY_MAP.put(AccountBy.adminName, Key.AccountBy.adminName);
        ACCOUNT_BY_MAP.put(AccountBy.foreignPrincipal, Key.AccountBy.foreignPrincipal);
        ACCOUNT_BY_MAP.put(AccountBy.krb5Principal, Key.AccountBy.krb5Principal);
        ACCOUNT_BY_MAP.put(AccountBy.appAdminName, Key.AccountBy.appAdminName);
    }
    
    public static Function<Folder.View, ZFolder.View> FROM_SOAP_VIEW = new Function<Folder.View, ZFolder.View>() {
            @Override
            public ZFolder.View apply(Folder.View from) {
                ZFolder.View to = VIEW_MAP.get(from);
                return (to != null ? to : ZFolder.View.unknown);
            }
    };
    
    public static Function<ZFolder.View, Folder.View> TO_SOAP_VIEW = new Function<ZFolder.View, Folder.View>() {
        @Override
        public Folder.View apply(ZFolder.View from) {
            Folder.View to = VIEW_MAP.inverse().get(from);
            return (to != null ? to : Folder.View.UNKNOWN);
        }
    };

    public static Function<Identity, ZIdentity> FROM_SOAP_IDENTITY = new Function<Identity, ZIdentity>() {
        @Override
        public ZIdentity apply(Identity from) {
            return new ZIdentity(from);
        }
    };

    public static Function<ZIdentity, Identity> TO_SOAP_IDENTITY = new Function<ZIdentity, Identity>() {
        @Override
        public Identity apply(ZIdentity from) {
            return from.getData();
        }
    };

    public static Function<Signature, ZSignature> FROM_SOAP_SIGNATURE = new Function<Signature, ZSignature>() {
        @Override
        public ZSignature apply(Signature from) {
            return new ZSignature(from);
        }
    };

    public static Function<ZSignature, Signature> TO_SOAP_SIGNATURE = new Function<ZSignature, Signature>() {
        @Override
        public Signature apply(ZSignature from) {
            return from.getData();
        }
    };
    
    public static Function<AccountBy, Key.AccountBy> FROM_SOAP_ACCOUNT_BY =
        new Function<AccountBy, Key.AccountBy>() {
        @Override
        public Key.AccountBy apply(AccountBy by) {
            return ACCOUNT_BY_MAP.get(by);
        }
    };
    
    public static Function<Key.AccountBy, AccountBy> TO_SOAP_ACCOUNT_BY =
        new Function<Key.AccountBy, AccountBy>() {
        @Override
        public AccountBy apply(Key.AccountBy by) {
            return ACCOUNT_BY_MAP.inverse().get(by);
        }
    };
}
