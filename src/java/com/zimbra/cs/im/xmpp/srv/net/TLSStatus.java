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

package com.zimbra.cs.im.xmpp.srv.net;

/**
 * A TLSStatus enum describing the current handshaking state of this TLS connection.
 * 
 * @author Hao Chen
 */
public enum TLSStatus {

	/**
	 * ust send data to the remote side before handshaking can continue.
	 */
	NEED_WRITE,

	/**
	 * Need to receive data from the remote side before handshaking can continue.
	 */
	NEED_READ,

	/**
	 * Not be able to unwrap the incoming data because there were not enough source bytes available
	 * to make a complete packet.
	 */
	UNDERFLOW,

	/**
	 * The operation just closed this side of the SSLEngine, or the operation could not be completed
	 * because it was already closed.
	 */
	CLOSED,

	/**
	 * Handshaking is OK.
	 */
	OK;
}
