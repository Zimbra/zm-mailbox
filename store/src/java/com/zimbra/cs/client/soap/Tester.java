/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client.soap;

import java.io.IOException;
import java.io.File;
import java.util.*;

import org.apache.http.HttpException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.client.*;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.service.mail.ItemAction;

public class Tester {

    private static String trashFolderID;
    private static String inboxFolderID;
    private static String cvsConvID;       // recurring handle to conversation

    private static final String FOLDER_NAME_TRASH = "Trash";
    private static final String FOLDER_NAME_INBOX = "INBOX";

    private static LmcEmailAddress gEa;
    static {
        gEa = new LmcEmailAddress();
        gEa.setType("t");
        gEa.setContent("kluge@yahoo.com");
        gEa.setEmailAddress("kluge@yahoo.com");
    }

    private static void recursiveDumpFolder(LmcFolder f, int depth) {

        for (int j = 0; j < depth; j++)
            System.out.print("\t");
        System.out.println(f);
        String fName = f.getName();
        if (fName != null)
            if (fName.equals(FOLDER_NAME_TRASH))
                trashFolderID = f.getFolderID();
            else if (fName.equals(FOLDER_NAME_INBOX))
                inboxFolderID = f.getFolderID();
        LmcFolder subFolders[] = f.getSubFolders();
        for (int i = 0; subFolders != null && i < subFolders.length; i++)
            recursiveDumpFolder(subFolders[i], depth + 1);
    }

    private static void doSearchReadDelete(LmcSession session, String serverURL)
        throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {
        /* search to find messages that have CVS in the subject */
        System.out.println("==== SEARCH \"CVS\" ======");
        LmcSearchRequest sReq = new LmcSearchRequest();
        sReq.setOffset("0");
        sReq.setLimit("30");
        sReq.setQuery("Subject:\"CVS COMMIT\"");
        sReq.setTypes(MailItem.Type.MESSAGE.toString());
        sReq.setSession(session);
        LmcSearchResponse sResp = (LmcSearchResponse) sReq.invoke(serverURL);

        /* ran search for messages so hopefully everything is a message */
        List mList = sResp.getResults();
        LmcMessage msg = null;
        for (Iterator mit = mList.iterator(); mit.hasNext(); ) {
            msg = (LmcMessage) mit.next();
            System.out.println(msg);
        }

        // this is a little cheesy.  if it can't find a message based on
        // the search above it just throws an exception.
        String msgID = null;
        if (mList.isEmpty())
            throw new LmcSoapClientException("test mailbox probably not set up properly");
        else
            msgID = msg.getID();

        /* read one */
        System.out.println("reading message " + msgID);
        LmcGetMsgRequest gmReq = new LmcGetMsgRequest();
        gmReq.setMsgToGet(msgID);
        gmReq.setRead("1");
        gmReq.setSession(session);
        LmcGetMsgResponse gmResp = (LmcGetMsgResponse) gmReq.invoke(serverURL);

        /* move it to trash */
        System.out.println("move to trash message " + msgID);
        LmcMsgActionRequest maReq = new LmcMsgActionRequest();
        maReq.setMsgList(msgID);
        maReq.setOp(ItemAction.OP_MOVE);
        maReq.setFolder(trashFolderID);
        maReq.setSession(session);
        LmcMsgActionResponse maResp = (LmcMsgActionResponse) maReq.invoke(serverURL);

        /* hard delete it */
        System.out.println("hard delete message " + msgID);
        maReq.setOp(ItemAction.OP_HARD_DELETE);
        maReq.setFolder(null);
        maResp = (LmcMsgActionResponse) maReq.invoke(serverURL);
    }

    private static void doCreateDeleteFolder(LmcSession session, String serverURL)
            throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {
        /* create new folder "testfolder" */
        System.out.println("==== CREATE FOLDER ======");
        LmcCreateFolderRequest cfReq = new LmcCreateFolderRequest();
        cfReq.setSession(session);
        cfReq.setName("testfolder");
        cfReq.setParentID(inboxFolderID);
        LmcCreateFolderResponse cfResp = (LmcCreateFolderResponse) cfReq.invoke(serverURL);
        String newID = cfResp.getFolder().getFolderID();
        System.out.println("created new folder with ID " + newID);

        /* delete the folder we just created */
        System.out.println("==== DELETE FOLDER ======");
        LmcFolderActionRequest faReq = new LmcFolderActionRequest();
        faReq.setSession(session);
        faReq.setFolderList(newID);
        faReq.setOp(ItemAction.OP_HARD_DELETE);
        LmcFolderActionResponse faResp = (LmcFolderActionResponse) faReq.invoke(serverURL);
        System.out.println("delete folder successful");
    }

    private static void doSearchAndConvAction(LmcSession session, String serverURL)
            throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {
        /* search to find messages that have CVS in the subject */
        System.out.println("==== SEARCH \"CVS\" ======");
        LmcSearchRequest sReq = new LmcSearchRequest();
        sReq.setOffset("0");
        sReq.setLimit("30");
        sReq.setQuery("Subject:\"CVS COMMIT\"");
        sReq.setTypes(MailItem.Type.CONVERSATION.toString());
        sReq.setSession(session);
        LmcSearchResponse sResp = (LmcSearchResponse) sReq.invoke(serverURL);

        LmcConversation conv = null;
        LmcConversation firstConv = null;
        List cList = sResp.getResults();
        for (Iterator cit = cList.iterator(); cit.hasNext(); ) {
            conv = (LmcConversation) cit.next();
            if (firstConv == null)
                firstConv = conv;
            System.out.println(conv);
        }

        // this is a little cheesy.  if it can't find a conv based on
        // the search above it just throws an exception.
        if (cList.isEmpty())
            throw new LmcSoapClientException("test mailbox probably not set up properly");
        else
            cvsConvID = conv.getID();


        // now move the conversation to the trash folder

        System.out.println("move to trash conv " + cvsConvID);
        LmcConvActionRequest caReq = new LmcConvActionRequest();
        caReq.setConvList(cvsConvID);
        caReq.setOp("move"); // XXX need constant
        caReq.setFolder(trashFolderID);
        caReq.setSession(session);
        LmcConvActionResponse caResp = (LmcConvActionResponse) caReq.invoke(serverURL);

        // and move it back
        System.out.println("move to INBOX conv " + cvsConvID);
        caReq.setConvList(cvsConvID);
        caReq.setOp("move"); // XXX need constant
        caReq.setFolder(inboxFolderID);
        caReq.setSession(session);
        caResp = (LmcConvActionResponse) caReq.invoke(serverURL);

    }

    private static void getAndDumpContacts(LmcSession session, String serverURL)
            throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {
        /* get contacts */
        System.out.println("==== GET CONTACTS ======");
        LmcGetContactsRequest gcReq = new LmcGetContactsRequest();
        gcReq.setSession(session);
        LmcGetContactsResponse gcResp = (LmcGetContactsResponse) gcReq.invoke(serverURL);

        /* dump the contacts */
        System.out.println("====== DUMP CONTACTS ======");
        LmcContact contacts[] = gcResp.getContacts();
        for (int o = 0; contacts != null && o < contacts.length; o++)
            System.out.println(contacts[o]);

    }

    private static void doSearchConv(LmcSession session, String serverURL)
            throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {
        // just search for CVS in a conv we know will have it
        System.out.println("==== SEARCH CONV ======");
        LmcSearchConvRequest sReq = new LmcSearchConvRequest();
        sReq.setOffset("0");
        sReq.setLimit("30");
        sReq.setTypes(MailItem.Type.MESSAGE.toString());
        sReq.setQuery("CVS");
        sReq.setSession(session);
        sReq.setConvID(cvsConvID);
        LmcSearchConvResponse sResp = (LmcSearchConvResponse) sReq.invoke(serverURL);

        /* ran search for messages so hopefully everything is a message */
        List mList = sResp.getResults();
        LmcMessage msg = null;
        for (Iterator mit = mList.iterator(); mit.hasNext(); ) {
            msg = (LmcMessage) mit.next();
            System.out.println(msg);
        }
    }

    private static void doBrowse(LmcSession session, String serverURL)
            throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {
        System.out.println("======= BROWSE BY DOMAIN ======");
        LmcBrowseRequest bReq = new LmcBrowseRequest();
        bReq.setBrowseBy("domains");
        bReq.setSession(session);
        LmcBrowseResponse bResp = (LmcBrowseResponse) bReq.invoke(serverURL);
        System.out.println("got back browse data");
        LmcBrowseData bd[] = bResp.getData();
        for (int i = 0; i < bd.length; i++) {
            System.out.println(bd[i].getFlags() + " " + bd[i].getData());
        }
    }



    private static void doCreateDeleteContact(LmcSession session, String serverURL)
            throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {
        // create the contact
        System.out.println("=========== CREATING CONTACT ============");
        LmcCreateContactRequest ccReq = new LmcCreateContactRequest();
        ccReq.setSession(session);
        LmcContact c = new LmcContact();
        LmcContactAttr attrs[] = new LmcContactAttr[] {
            new LmcContactAttr("email", "1", null, "schumie@f1.com"),
            new LmcContactAttr("firstName", "2", null, "Michael"),
            new LmcContactAttr("lastName", "3", null, "Schumacher")
        };
        c.setAttrs(attrs);
        ccReq.setContact(c);
        LmcCreateContactResponse ccResp = (LmcCreateContactResponse) ccReq.invoke(serverURL);
        String newID = ccResp.getContact().getID();

        // make sure we have the new contact
        getAndDumpContacts(session, serverURL);

        // delete the contact
        System.out.println("=========== DELETING CONTACT ============");
        LmcContactActionRequest caReq = new LmcContactActionRequest();
        caReq.setSession(session);
        caReq.setOp(ItemAction.OP_HARD_DELETE);
        caReq.setIDList(newID);
        LmcContactActionResponse caResp = (LmcContactActionResponse) caReq.invoke(serverURL);


        // make sure we deleted the new contact
        getAndDumpContacts(session, serverURL);
    }

    private static void doCreateDeleteTag(LmcSession session, String serverURL)
            throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {

        // create the tag
        System.out.println("=========== CREATING TAG ============");
        LmcCreateTagRequest ctReq = new LmcCreateTagRequest();
        ctReq.setSession(session);
        ctReq.setName("testtag3");
        ctReq.setColor("0"); // XXX where are the allowed colors defined?
        LmcCreateTagResponse ctResp = (LmcCreateTagResponse) ctReq.invoke(serverURL);
        LmcTag newTag = ctResp.getTag();
        System.out.println("created tag " + newTag.getID());

        // delete the tag
        System.out.println("=========== DELETING TAG ============");
        LmcTagActionRequest taReq = new LmcTagActionRequest();
        taReq.setSession(session);
        taReq.setOp(ItemAction.OP_HARD_DELETE);
        taReq.setTagList(newTag.getID());
        LmcTagActionResponse taResp = (LmcTagActionResponse) taReq.invoke(serverURL);
    }

    private static void doCreateGetDeleteNote(LmcSession session, String serverURL)
            throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {
        // create the Note
        System.out.println("=========== CREATING NOTE ============");
        LmcCreateNoteRequest ctReq = new LmcCreateNoteRequest();
        ctReq.setSession(session);
        ctReq.setParentID(inboxFolderID);
        ctReq.setContent("this is a test note");
        ctReq.setColor("0"); // XXX where are the allowed colors defined?
        LmcCreateNoteResponse ctResp = (LmcCreateNoteResponse) ctReq.invoke(serverURL);
        LmcNote newNote = ctResp.getNote();
        String noteID = newNote.getID();
        System.out.println("created Note " + noteID);

        // get the Note
        System.out.println("=========== GET NOTE ============");
        LmcGetNoteRequest gnReq = new LmcGetNoteRequest();
        gnReq.setSession(session);
        gnReq.setNoteToGet(noteID);
        LmcGetNoteResponse gnResp = (LmcGetNoteResponse) gnReq.invoke(serverURL);
        newNote = gnResp.getNote();
        System.out.println("created Note\n" + newNote);

        // delete the Note
        System.out.println("=========== DELETING NOTE ============");
        LmcNoteActionRequest taReq = new LmcNoteActionRequest();
        taReq.setSession(session);
        taReq.setOp(ItemAction.OP_HARD_DELETE);
        taReq.setNoteList(noteID);
        LmcNoteActionResponse taResp = (LmcNoteActionResponse) taReq.invoke(serverURL);
        System.out.println("successfully deleted note " + taResp.getNoteList());
    }

    private static void dumpPrefs(HashMap prefMap) {
        System.out.println("===== DUMP THE PREFS ===== ");
        Set s = prefMap.entrySet();
        Iterator i = s.iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            System.out.println("key " + (String) entry.getKey() + " value " +
                    (String) entry.getValue());
        }
    }

    private static void doGetDumpPrefs(LmcSession session, String serverURL)
            throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {
        System.out.println("====== GET PREFS ==========");
        String prefs[] = new String[] { "zimbraPrefMailSignatureEnabled",
                                        "zimbraPrefSaveToSent" };
        LmcGetPrefsRequest prefReq = new LmcGetPrefsRequest();
        prefReq.setSession(session);
        prefReq.setPrefsToGet(prefs);
        LmcGetPrefsResponse prefResp = (LmcGetPrefsResponse) prefReq.invoke(serverURL);
        HashMap prefMap = prefResp.getPrefsMap();

        dumpPrefs(prefMap);
    }

    private static void doModifyDumpPrefs(LmcSession session, String serverURL)
            throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {
        System.out.println("=========== MODIFY PREFS ==========");
        HashMap prefMods = new HashMap();
        prefMods.put("zimbraPrefMailSignatureEnabled", "TRUE");
        prefMods.put("zimbraPrefSaveToSent", "TRUE");

        LmcModifyPrefsRequest mpReq = new LmcModifyPrefsRequest();
        mpReq.setSession(session);
        mpReq.setPrefMods(prefMods);
        LmcModifyPrefsResponse mpResp = (LmcModifyPrefsResponse) mpReq.invoke(serverURL);

        doGetDumpPrefs(session, serverURL);
    }


    private static void doChangePassword(LmcSession session, String account, String currPassword, String serverURL)
            throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {
        System.out.println("=========== CHANGE PASSWORD ==========");

        // change the password
        LmcChangePasswordRequest cpReq = new LmcChangePasswordRequest();
        cpReq.setSession(session);
        cpReq.setAccount(account);
        cpReq.setOldPassword(currPassword);
        cpReq.setPassword("test1234");
        cpReq.invoke(serverURL);

        // change it back
        System.out.println("changing the password back");
        cpReq.setOldPassword("test1234");
        cpReq.setPassword(currPassword);
        cpReq.invoke(serverURL);
    }

    private static void doAddMsg(LmcSession session, String serverURL)
            throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {
        System.out.println("====== ADD MSG =======");

        LmcMessage lMsg = new LmcMessage();
        lMsg.setEmailAddresses(new LmcEmailAddress[] { gEa });
        Date d = new Date();
        lMsg.setSubject("AddMsg: " + d);
        lMsg.setFolder(inboxFolderID);
        lMsg.setContent("From: kluge@example.zimbra.com\r\nTo: kluge@dogfood.example.zimbra.com\r\nSubject: AddMsg " + d + "\r\n\r\nThis is some text.");
        LmcAddMsgRequest amr = new LmcAddMsgRequest();
        amr.setMsg(lMsg);
        amr.setSession(session);
        LmcAddMsgResponse amrResp = (LmcAddMsgResponse) amr.invoke(serverURL);
        System.out.println("Add successful, resulting ID " + amrResp.getID());
    }


    private static void doGetInfo(LmcSession session, String serverURL)
            throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {
        System.out.println("=========== GET INFO ==========");
        LmcGetInfoRequest giReq = new LmcGetInfoRequest();
        giReq.setSession(session);
        LmcGetInfoResponse giResp = (LmcGetInfoResponse) giReq.invoke(serverURL);
        System.out.println("Account name: " + giResp.getAcctName());
        System.out.println("lifetime: " + giResp.getLifetime());
        dumpPrefs(giResp.getPrefMap());
    }

    private static void doSearchGal(LmcSession session, String serverURL, String searchTarget)
            throws IOException, LmcSoapClientException, ServiceException, SoapFaultException, HttpException {
        System.out.println("=========== SEARCH GAL ==========");
        LmcSearchGalRequest sgReq = new LmcSearchGalRequest();
        sgReq.setSession(session);
        sgReq.setName(searchTarget);
        LmcSearchGalResponse sgResp = (LmcSearchGalResponse) sgReq.invoke(serverURL);
        System.out.println("Search results ----");
        LmcContact contacts[] = sgResp.getContacts();
        for (int o = 0; contacts != null && o < contacts.length; o++)
            System.out.println(contacts[o]);
    }

    public static void main(String argv[]) {
        CliUtil.toolSetup();

        if (argv.length != 3) {
            System.out.println("Usage: Tester <serverURL> <username> <password>");
            System.out.println("where:");
            System.out.println("<serverURL> is the full URL to the SOAP service");
            System.out.println("<username> is the name of the user to log in as");
            System.out.println("<password> is that user's password");
            System.out.println("NOTE: THIS COMMAND WILL DELETE E-MAIL!!!");
        }
        String serverURL = argv[0];
        System.out.println("connecting to " + serverURL + " as " + argv[1] +
            " with password " + argv[2]);

        try {
            /* do a ping */
            LmcPingRequest pr = new LmcPingRequest();
            LmcPingResponse pResp = (LmcPingResponse) pr.invoke(serverURL);

            /* auth first */
            System.out.println("========= AUTHENTICATE ===========");
            LmcAuthRequest auth = new LmcAuthRequest();
            auth.setUsername(argv[1]);
            auth.setPassword(argv[2]);
            LmcAuthResponse authResp = (LmcAuthResponse) auth.invoke(serverURL);
            LmcSession session = authResp.getSession();

            /* get some prefs -- this is not part of the login sequence now */
            doGetDumpPrefs(session, serverURL);

            /* get the tags */
            System.out.println("======== GET TAGS =======");
            LmcGetTagRequest gtReq = new LmcGetTagRequest();
            gtReq.setSession(session);
            LmcGetTagResponse gtResp = (LmcGetTagResponse) gtReq.invoke(serverURL);

            /* dump the tags */
            System.out.println("==== DUMP TAGS ======");
            LmcTag tags[] = gtResp.getTags();
            for (int t = 0; tags != null && t < tags.length; t++)
                System.out.println(tags[t]);

            /* get the folders */
            System.out.println("==== GET FOLDERS ======");
            LmcGetFolderRequest gfReq = new LmcGetFolderRequest();
            gfReq.setSession(session);
            LmcGetFolderResponse gfResp = (LmcGetFolderResponse) gfReq.invoke(serverURL);

            /* dump the folders */
            System.out.println("====== DUMP FOLDERS ======");
            LmcFolder folder = gfResp.getRootFolder();
            recursiveDumpFolder(folder, 0);

            /* inbox listing */
            System.out.println("==== SEARCH in:inbox ======");
            LmcSearchRequest sReq = new LmcSearchRequest();
            sReq.setOffset("0");
            sReq.setLimit("30");
            sReq.setQuery("in:inbox");
            sReq.setSession(session);
            sReq.setTypes(MailItem.Type.CONVERSATION.toString());
            LmcSearchResponse sResp = (LmcSearchResponse) sReq.invoke(serverURL);

            /* dump the search */
            System.out.println("====== DUMP SEARCH ======");
            System.out.println("offset=\"" + sResp.getOffset() + "\" more=\"" +
                               sResp.getMore() + "\"");
            LmcConversation conv = null;
            LmcConversation firstConv = null;
            List cList = sResp.getResults();
            for (Iterator cit = cList.iterator(); cit.hasNext(); ) {
                conv = (LmcConversation) cit.next();
                if (firstConv == null)
                    firstConv = conv;
                System.out.println(conv);
            }


            /*****  at this point the emulation of a login is complete *****/
            /*****  the following code emulates the first conv retrieval *****/
            getAndDumpContacts(session, serverURL);


            /* get the first conversation from the search */
            System.out.println("===== GET CONVERSATION =====");
            LmcGetConvRequest gconvReq = new LmcGetConvRequest();
            gconvReq.setConvToGet(firstConv.getID());
            gconvReq.setSession(session);
            /*
             * the client gets message detail in the getConvRequest and then
             * fetches the msg with GetMsgReq anyway.  so that's repeated here.
             */
            String msgDetail[] = new String[] { firstConv.getMessages()[0].getID() };
            gconvReq.setMsgsToGet(msgDetail);
            LmcGetConvResponse gconvResp = (LmcGetConvResponse) gconvReq.invoke(serverURL);

            /* dump the conversation response */
            System.out.println("===== DUMP CONVERSATION ===== ");
            System.out.println(gconvResp.getConv());

            /* get the message in that conversation */
            System.out.println("===== GET MESSAGE ===== ");
            LmcGetMsgRequest gmReq = new LmcGetMsgRequest();
            gmReq.setRead("1");
            gmReq.setMsgToGet(msgDetail[0]);
            gmReq.setSession(session);
            LmcGetMsgResponse gmResp = (LmcGetMsgResponse) gmReq.invoke(serverURL);

            /* dump the message in that conversation */
            System.out.println("===== DUMP MESSAGE ===== ");
            System.out.println(gmResp.getMsg());

            /**** that completes emulation of viewing a conv and its first message ****/

            /* send a new message */
            System.out.println("===== SEND MESSAGE ===== ");
            LmcMessage lMsg = new LmcMessage();
            lMsg.setEmailAddresses(new LmcEmailAddress[] { gEa });
            lMsg.setSubject("msg from the test program");
            LmcMimePart smrMp = new LmcMimePart();
            smrMp.setContentType("text/plain");
            smrMp.setContent("there is some fresh coffee somewhere");
            lMsg.addMimePart(smrMp);
            LmcSendMsgRequest smr = new LmcSendMsgRequest();
            smr.setMsg(lMsg);
            smr.setSession(session);

            // add an attachment.  XXX hardcoded stuff...
            String aid = smr.postAttachment("http://dogfood.example.zimbra.com/service/upload",
                                            session, new File("c:/temp/ops.txt"), ".example.zimbra.com", 5000);
            System.out.println("got back attachment id " + aid);
            lMsg.setAttachmentIDs(new String[] { aid});
            LmcSendMsgResponse smrResp = (LmcSendMsgResponse) smr.invoke(serverURL);

            /* print result of sending new message */
            System.out.println("==== DUMP SEND MSG RESPONSE ====");
            System.out.println("Send successful, resulting ID " + smrResp.getID());

            doSearchReadDelete(session, serverURL);

            doSearchAndConvAction(session, serverURL);

            doCreateDeleteFolder(session, serverURL);

            // will also dump contacts
            doCreateDeleteContact(session, serverURL);

            doCreateDeleteTag(session, serverURL);

            doModifyDumpPrefs(session, serverURL);

            doCreateGetDeleteNote(session, serverURL);

            doChangePassword(session, argv[1], argv[2], serverURL);

            doGetInfo(session, serverURL);

            doSearchGal(session, serverURL, "Kevin");  // will not match
            doSearchGal(session, serverURL, "Satish"); // will match

            doSearchConv(session, serverURL);

            doBrowse(session, serverURL);

            doAddMsg(session, serverURL);

        } catch (SoapFaultException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (LmcSoapClientException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            e.printStackTrace();
        } catch (HttpException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
