///*
// * ***** BEGIN LICENSE BLOCK *****
// * Version: MPL 1.1
// *
// * The contents of this file are subject to the Mozilla Public License
// * Version 1.1 ("License"); you may not use this file except in
// * compliance with the License. You may obtain a copy of the License at
// * http://www.zimbra.com/license
// *
// * Software distributed under the License is distributed on an "AS IS"
// * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
// * the License for the specific language governing rights and limitations
// * under the License.
// * 
// * Part of the Zimbra Collaboration Suite Server.
// *
// * The Original Code is Copyright (C) Jive Software. Used with permission
// * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
// * All Rights Reserved.
// *
// * Contributor(s):
// *
// * ***** END LICENSE BLOCK *****
// */
//
//package com.zimbra.cs.im.xmpp.srv.auth;
//
//import com.zimbra.cs.im.xmpp.srv.user.UserNotFoundException;
//import com.zimbra.cs.im.xmpp.srv.user.DefaultUserProvider;
//
///**
// * Default AuthProvider implementation. It authenticates against the <tt>jiveUser</tt>
// * database table and supports plain text and digest authentication.
// *
// * Because each call to authenticate() makes a database connection, the
// * results of authentication should be cached whenever possible.
// *
// * @author Matt Tucker
// */
//public class DefaultAuthProvider implements AuthProvider {
//
//    private DefaultUserProvider userProvider;
//
//    /**
//     * Constructs a new DefaultAuthProvider.
//     */
//    public DefaultAuthProvider() {
//        // Create a new default user provider since we need it to get the
//        // user's password. We always create our own user provider because
//        // we don't know what user provider is configured for the system and
//        // the contract of this class is to authenticate against the jiveUser
//        // database table.
//        userProvider = new DefaultUserProvider();
//    }
//
//    public void authenticate(String username, String password) throws UnauthorizedException {
//        if (username == null || password == null) {
//            throw new UnauthorizedException();
//        }
//        username = username.trim().toLowerCase();
//        try {
//            if (!password.equals(userProvider.getPassword(username))) {
//                throw new UnauthorizedException();
//            }
//        }
//        catch (UserNotFoundException unfe) {
//            throw new UnauthorizedException();
//        }
//        // Got this far, so the user must be authorized.
//    }
//
//    public void authenticate(String username, String token, String digest) throws UnauthorizedException {
//        if (username == null || token == null || digest == null) {
//            throw new UnauthorizedException();
//        }
//        username = username.trim().toLowerCase();
//        try {
//            String password = userProvider.getPassword(username);
//            String anticipatedDigest = AuthFactory.createDigest(token, password);
//            if (!digest.equalsIgnoreCase(anticipatedDigest)) {
//                throw new UnauthorizedException();
//            }
//        }
//        catch (UserNotFoundException unfe) {
//            throw new UnauthorizedException();
//        }
//        // Got this far, so the user must be authorized.
//    }
//
//    public boolean isPlainSupported() {
//        return true;
//    }
//
//    public boolean isDigestSupported() {
//        return true;
//    }
//}