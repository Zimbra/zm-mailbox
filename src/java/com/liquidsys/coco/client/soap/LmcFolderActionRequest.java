package com.liquidsys.coco.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.liquidsys.soap.DomUtil;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.mail.MailService;

public class LmcFolderActionRequest extends LmcSoapRequest {

        private String mIDList;

        private String mOp;

        private String mTargetFolder;

        private String mName;

        /**
         * Set the list of Folder ID's to operate on
         * @param idList - a list of the folders to operate on
         */
        public void setFolderList(String idList) {
                mIDList = idList;
        }

        /**
         * Set the operation
         * @param op - the operation (delete, read, etc.)
         */
        public void setOp(String op) {
                mOp = op;
        }

        public void setName(String t) {
                mName = t;
        }

        public void setTargetFolder(String f) {
                mTargetFolder = f;
        }

        public String getFolderList() {
                return mIDList;
        }

        public String getOp() {
                return mOp;
        }

        public String getTargetFolder() {
                return mTargetFolder;
        }

        public String getName() {
                return mName;
        }

        protected Element getRequestXML() {
                Element request = DocumentHelper.createElement(MailService.FOLDER_ACTION_REQUEST);
                Element a = DomUtil.add(request, MailService.E_ACTION, "");
                if (mIDList != null)
                    DomUtil.addAttr(a, MailService.A_ID, mIDList);
                if (mOp != null)
                    DomUtil.addAttr(a, MailService.A_OPERATION, mOp);
                if (mName != null)
                    DomUtil.addAttr(a, MailService.A_NAME, mName);
                if (mTargetFolder != null)
                    DomUtil.addAttr(a, MailService.A_FOLDER, mTargetFolder);
                return request;
        }

        protected LmcSoapResponse parseResponseXML(Element responseXML)
                        throws ServiceException {
                LmcFolderActionResponse response = new LmcFolderActionResponse();
                Element a = DomUtil.get(responseXML, MailService.E_ACTION);
                response.setFolderList(DomUtil.getAttr(a, MailService.A_ID));
                response.setOp(DomUtil.getAttr(a, MailService.A_OPERATION));
                return response;
        }
}