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
 * Part of the Zimbra Collaboration Suite Server.
 *
 * The Original Code is Copyright (C) Jive Software. Used with permission
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv.disco;

import java.util.Iterator;

/**
 * ServerFeaturesProviders are responsible for providing the features offered and supported
 * protocols by the SERVER. Example of server features are: jabber:iq:agents, jabber:iq:time, etc.
 * <p/>
 * <p/>
 * When the server starts up, IQDiscoInfoHandler will request to all the services that implement
 * the ServerFeaturesProvider interface for their features. Whenever a disco request is received
 * IQDiscoInfoHandler will add to the provided information all the collected features. Therefore, a
 * service must implement this interface in order to offer/publish its features as part of the
 * server features.
 *
 * @author Gaston Dombiak
 */
public interface ServerFeaturesProvider {

    /**
     * Returns an Iterator (of String) with the supported features by the server. The features to
     * include are the features offered and supported protocols by the SERVER. The idea is that
     * different modules may provide their features that will ultimately be part of the features
     * offered by the server.
     *
     * @return an Iterator (of String) with the supported features by the server.
     */
    public abstract Iterator<String> getFeatures();
}
