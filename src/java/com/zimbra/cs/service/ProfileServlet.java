/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.formatter.NativeFormatter;
import com.zimbra.cs.servlet.ZimbraServlet;

public class ProfileServlet extends ZimbraServlet {

    private static final long serialVersionUID = -5209313273034536159L;
    private static final Log log = LogFactory.getLog(ProfileServlet.class); 
    
    public static String IMAGE_URI = "image";

    @Override
    public void init() throws ServletException {
        log.info("Starting up");
        super.init();
    }

    @Override
    public void destroy() {
        log.info("Shutting down");
        super.destroy();
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() == 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
        if (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.substring(1);
        }
        String[] tokens = pathInfo.split("/");
        String emailAddress = tokens[0];
        Provisioning prov = Provisioning.getInstance();
        try {
            Account account = prov.getAccountByName(emailAddress);
            if (account == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            // for now the profile picture is public.  we could add privacy acl later on.
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            try {
                Document profileImage = (Document)mbox.getItemByPath(null, IMAGE_URI, Mailbox.ID_FOLDER_PROFILE);
                String contentType = MimeDetect.getMimeDetect().detect(profileImage.getContentStream());
                resp.setContentType(contentType);
                NativeFormatter.sendbackBinaryData(req, resp, profileImage.getContentStream(), contentType, null, null, profileImage.getSize());
            } catch (ServiceException e) {
                if (e instanceof MailServiceException.NoSuchItemException) {
                    // return default image;
                    File defaultImage = new File(LC.default_profile_image.value());
                    String contentType = MimeDetect.getMimeDetect().detect(new FileInputStream(defaultImage));
                    resp.setContentType(contentType);
                    NativeFormatter.sendbackBinaryData(req, resp, new FileInputStream(defaultImage), contentType, null, null, defaultImage.length());
                } else {
                    throw e;
                }
            }
        } catch (ServiceException e) {
            log.error("can't handle profile request", e);
            throw new ServletException(e);
        }
    }
}
