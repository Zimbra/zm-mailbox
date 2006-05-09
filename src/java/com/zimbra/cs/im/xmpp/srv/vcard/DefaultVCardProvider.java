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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.srv.vcard;

import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import com.zimbra.cs.im.xmpp.database.DbConnectionManager;
import com.zimbra.cs.im.xmpp.util.AlreadyExistsException;
import com.zimbra.cs.im.xmpp.util.Log;
import com.zimbra.cs.im.xmpp.util.NotFoundException;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Default implementation of the VCardProvider interface, which reads and writes data
 * from the <tt>jiveVCard</tt> database table.
 *
 * @author Gaston Dombiak
 */
public class DefaultVCardProvider implements VCardProvider {

    private static final String LOAD_PROPERTIES =
        "SELECT value FROM jiveVCard WHERE username=?";
    private static final String DELETE_PROPERTIES =
        "DELETE FROM jiveVCard WHERE username=?";
    private static final String UPDATE_PROPERTIES =
        "UPDATE jiveVCard SET value=? WHERE username=?";
    private static final String INSERT_PROPERTY =
        "INSERT INTO jiveVCard (username, value) VALUES (?, ?)";

    /**
     * Pool of SAX Readers. SAXReader is not thread safe so we need to have a pool of readers.
     */
    private BlockingQueue<SAXReader> xmlReaders = new LinkedBlockingQueue<SAXReader>();


    public DefaultVCardProvider() {
        super();
        // Initialize the pool of sax readers
        for (int i=0; i<10; i++) {
            xmlReaders.add(new SAXReader());
        }
    }

    public Element loadVCard(String username) {
        synchronized (username.intern()) {
            Element vCardElement = null;
            java.sql.Connection con = null;
            PreparedStatement pstmt = null;
            SAXReader xmlReader = null;
            try {
                // Get a sax reader from the pool
                xmlReader = xmlReaders.take();
                con = DbConnectionManager.getConnection();
                pstmt = con.prepareStatement(LOAD_PROPERTIES);
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    vCardElement =
                            xmlReader.read(new StringReader(rs.getString(1))).getRootElement();
                }
            }
            catch (Exception e) {
                Log.error("Error loading vCard of username: " + username, e);
            }
            finally {
                // Return the sax reader to the pool
                if (xmlReader != null) {
                    xmlReaders.add(xmlReader);
                }
                try { if (pstmt != null) { pstmt.close(); } }
                catch (Exception e) { Log.error(e); }
                try { if (con != null) { con.close(); } }
                catch (Exception e) { Log.error(e); }
            }
            return vCardElement;
        }
    }

    public void createVCard(String username, Element vCardElement) throws AlreadyExistsException {
        if (loadVCard(username) != null) {
            // The user already has a vCard
            throw new AlreadyExistsException("Username " + username + " already has a vCard");
        }

        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(INSERT_PROPERTY);
            pstmt.setString(1, username);
            pstmt.setString(2, vCardElement.asXML());
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            Log.error("Error creating vCard for username: " + username, e);
        }
        finally {
            try { if (pstmt != null) { pstmt.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (con != null) { con.close(); } }
            catch (Exception e) { Log.error(e); }
        }
    }

    public void updateVCard(String username, Element vCardElement) throws NotFoundException {
        if (loadVCard(username) == null) {
            // The user already has a vCard
            throw new NotFoundException("Username " + username + " does not have a vCard");
        }
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(UPDATE_PROPERTIES);
            pstmt.setString(1, vCardElement.asXML());
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            Log.error("Error updating vCard of username: " + username, e);
        }
        finally {
            try { if (pstmt != null) { pstmt.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (con != null) { con.close(); } }
            catch (Exception e) { Log.error(e); }
        }
    }

    public void deleteVCard(String username) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(DELETE_PROPERTIES);
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            Log.error("Error deleting vCard of username: " + username, e);
        }
        finally {
            try { if (pstmt != null) { pstmt.close(); } }
            catch (Exception e) { Log.error(e); }
            try { if (con != null) { con.close(); } }
            catch (Exception e) { Log.error(e); }
        }
    }

    public boolean isReadOnly() {
        return false;
    }
}
