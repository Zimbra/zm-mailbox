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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv.disco;

import org.dom4j.Element;
import com.zimbra.cs.im.xmpp.srv.forms.spi.XDataFormImpl;
import org.xmpp.packet.JID;

import java.util.Iterator;

/**
 * A DiscoInfoProvider is responsible for providing information about a JID's name and its node. For
 * example, the room service could implement this interface in order to provide disco#info about
 * its rooms. In this case, the JID's name will be the room's name and node will be null.<p>
 * <p/>
 * The information to provide has to include the entity's identity and the features offered and
 * protocols supported by the target entity. The identity will be provided as an Element that will
 * include the categoty, type and name attributes. Whilst the features will be just plain Strings.
 *
 * @author Gaston Dombiak
 */
public interface DiscoInfoProvider {

    /**
     * Returns an Iterator (of Element) with the target entity's identities. Each Element must
     * include the categoty, type and name attributes of the entity.
     *
     * @param name the recipient JID's name.
     * @param node the requested disco node.
     * @param senderJID the XMPPAddress of user that sent the disco info request.
     * @return an Iterator (of Element) with the target entity's identities.
     */
    public abstract Iterator<Element> getIdentities(String name, String node, JID senderJID);

    /**
     * Returns an Iterator (of String) with the supported features. The features to include are the
     * features offered and supported protocols by the target entity identified by the requested
     * name and node.
     *
     * @param name the recipient JID's name.
     * @param node the requested disco node.
     * @param senderJID the XMPPAddress of user that sent the disco info request.
     * @return an Iterator (of String) with the supported features.
     */
    public abstract Iterator<String> getFeatures(String name, String node, JID senderJID);

    /**
     * Returns an XDataForm with the extended information about the entity or null if none. Each bit
     * of information about the entity must be included as a value of a field of the form.
     *
     * @param name the recipient JID's name.
     * @param node the requested disco node.
     * @param senderJID the XMPPAddress of user that sent the disco info request.
     * @return an XDataForm with the extended information about the entity or null if none.
     */
    public abstract XDataFormImpl getExtendedInfo(String name, String node, JID senderJID);

    /**
     * Returns true if we can provide information related to the requested name and node. For
     * example, if the requested name refers to a non-existant MUC room then the answer will be
     * false. In case that the sender of the disco request is not authorized to discover this
     * information an UnauthorizedException will be thrown.
     *
     * @param name      the recipient JID's name.
     * @param node      the requested disco node.
     * @param senderJID the XMPPAddress of user that sent the disco info request.
     * @return true if we can provide information related to the requested name and node.
     */
    public abstract boolean hasInfo(String name, String node, JID senderJID);
}
