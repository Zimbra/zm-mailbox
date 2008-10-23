package com.zimbra.cs.account.accesscontrol;

import java.io.File;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class RightManager {
    private static final String E_ATTRS        = "attrs";
    private static final String E_DEFAULT      = "default";
    private static final String E_DESC         = "desc";
    private static final String E_DOC          = "doc";
    private static final String E_RIGHTS       = "rights";
    private static final String E_RIGHT        = "right";
    
    private static final String A_ATTR         = "attr";
    private static final String A_LIMIT        = "limit";
    private static final String A_NAME         = "name";
    private static final String A_ON_ENTRY     = "onEntry";
    private static final String A_TYPE         = "type";
    private static final String A_USER_RIGHT   = "userRight";
    
    private static RightManager mInstance;
    
    // keep the map sorted so "zmmailbox lp" can display in alphabetical order 
    private Map<String, UserRight> sUserRights = new TreeMap<String, UserRight>();  
    private Map<String, AdminRight> sAdminRights = new TreeMap<String, AdminRight>();  

    public static synchronized RightManager getInstance() throws ServiceException {
        if (mInstance != null) {
            return mInstance;
        }
        String dir = LC.zimbra_rights_directory.value();
        mInstance = new RightManager(dir);
        
        try {
            Right.initKnownRights(mInstance);
        } catch (ServiceException e) {
            ZimbraLog.account.error("failed to initialize known right from: " + dir, e);
            throw e;
        }
        return mInstance;
    }
    
    private RightManager(String dir) throws ServiceException {
        File fdir = new File(dir);
        if (!fdir.exists()) {
            throw ServiceException.FAILURE("rights directory does not exists: " + dir, null);
        }
        if (!fdir.isDirectory()) {
            throw ServiceException.FAILURE("rights directory is not a directory: " + dir, null);
        }
        
        File[] files = fdir.listFiles();
        for (File file : files) { 
            if (!file.getPath().endsWith(".xml")) {
                ZimbraLog.misc.warn("while loading attrs, ignoring not .xml file: " + file);
                continue;
            }
            if (!file.isFile()) {
                ZimbraLog.misc.warn("while loading attrs, ignored non-file: " + file);
            }
            try {
                loadRights(file);
            } catch (DocumentException de) {
                throw ServiceException.PARSE_ERROR("error loading rights file: " + file, de);
            }
        }
    }
    
    private boolean getBoolean(String value) throws ServiceException {
        if ("1".equals(value))
            return true;
        else if ("0".equals(value))
            return false;
        else
            throw ServiceException.PARSE_ERROR("invalid value:" + value, null);
    }
    
    private boolean getBooleanAttr(Element elem, String attr) throws ServiceException {
        String value = elem.attributeValue(attr);
        if (value == null)
            throw ServiceException.PARSE_ERROR("missing required attribute: " + attr, null);
        return getBoolean(value);
    }
    
    private boolean getBooleanAttr(Element elem, String attr, boolean defaultValue) throws ServiceException {
        String value = elem.attributeValue(attr);
        if (value == null)
            return defaultValue;
        return getBoolean(value);
    }
    
    private void parseDesc(Element eDesc, Right right) throws ServiceException {
        if (right.getDesc() != null)
            throw ServiceException.PARSE_ERROR("multiple " + E_DESC, null);
        right.setDesc(eDesc.getText());
    }
    
    private void parseDoc(Element eDoc, Right right) throws ServiceException {
        if (right.getDoc() != null)
            throw ServiceException.PARSE_ERROR("multiple " + E_DOC, null);
        right.setDoc(eDoc.getText());
    }
    
    private void parseDefault(Element eDefault, Right right) throws ServiceException {
        String defaultValue = eDefault.getText();
        if ("allow".equalsIgnoreCase(defaultValue))
            right.setDefault(Boolean.TRUE);
        else if ("deny".equalsIgnoreCase(defaultValue))
            right.setDefault(Boolean.FALSE);
        else
            throw ServiceException.PARSE_ERROR("invalid default value: " + defaultValue, null);
    }
    
    private void parseAttr(Element eAttr, AdminRight right) throws ServiceException {
        
        // parse attribute name
        String attrName = eAttr.getTextTrim();
        // TODO, validate if the attribute exists and if it is on all entry types
        AdminRight.AttrRight attrRight = right.addAttr(attrName);
            
        // parse onEntry
        String onEntries = eAttr.attributeValue(A_ON_ENTRY);
        if (onEntries == null)
            attrRight.setOnAllEntries();
        else {
            String[] entryTypes = onEntries.split(",");
            for (String entryType :entryTypes) {
                AdminRight.EntryType et = AdminRight.EntryType.fromString(entryType);
                attrRight.addOnEntry(et);
            }
        }
        
        // parse limit
        boolean limit = getBooleanAttr(eAttr, A_LIMIT);
        attrRight.setLimit(limit);
    }
    
    private void parseAttrs(Element eAttrs, Right right) throws ServiceException {
        if (!(right instanceof AdminRight))
            throw ServiceException.PARSE_ERROR(E_ATTRS + " is only allowed for admin right", null);
        
        AdminRight adminRight = (AdminRight)right;
        
        for (Iterator elemIter = eAttrs.elementIterator(); elemIter.hasNext();) {
            Element elem = (Element)elemIter.next();
            if (elem.getName().equals(A_ATTR))
                parseAttr(elem, adminRight);
            else
                throw ServiceException.PARSE_ERROR("invalid element: " + elem.getName(), null);   
        }
    }
    
    private Right parseRight(Element eRight) throws ServiceException {
        String name = eRight.attributeValue(A_NAME);
        boolean userRight = getBooleanAttr(eRight, A_USER_RIGHT, false);
        System.out.println("Parsing right " + "(" +  (userRight?"user":"admin") + ") " + name);
        
        
        AdminRight.RightType rightType = null;
        if (!userRight) {
            String rt = eRight.attributeValue(A_TYPE);
            if (rt == null)
                throw ServiceException.PARSE_ERROR("missing attribute [" + A_TYPE + "]", null);
            rightType = AdminRight.RightType.fromString(rt);
        }
            
        Right right = userRight? new UserRight(name) : new AdminRight(name, rightType);

        for (Iterator elemIter = eRight.elementIterator(); elemIter.hasNext();) {
            Element elem = (Element)elemIter.next();
            if (elem.getName().equals(E_DESC))
                parseDesc(elem, right);
            else if (elem.getName().equals(E_DOC))
                parseDoc(elem, right);
            else if (elem.getName().equals(E_DEFAULT))
                parseDefault(elem, right);
            else if (elem.getName().equals(E_ATTRS))
                parseAttrs(elem, right);
            else
                throw ServiceException.PARSE_ERROR("invalid element: " + elem.getName(), null);
        }
        if (right.getDesc() == null)
            throw ServiceException.PARSE_ERROR("missing element [" + E_DESC + "]", null);

        return right;
    }
    
    private void loadRights(File file) throws DocumentException, ServiceException {
        SAXReader reader = new SAXReader();
        Document doc = reader.read(file);
        Element root = doc.getRootElement();
        if (!root.getName().equals(E_RIGHTS))
            throw ServiceException.PARSE_ERROR("root tag is not " + E_RIGHTS, null);

        for (Iterator iter = root.elementIterator(); iter.hasNext();) {
            Element eRight = (Element) iter.next();
            if (!eRight.getName().equals(E_RIGHT))
                throw ServiceException.PARSE_ERROR("unknown element: " + eRight.getName(), null);

            String name = eRight.attributeValue(A_NAME);
            if (name == null)
                throw ServiceException.PARSE_ERROR("no name specified", null);
            
            if (sUserRights.get(name) != null || sAdminRights.get(name) != null) 
                throw ServiceException.PARSE_ERROR("right " + name + " is already defined", null);
            
            try {
                Right right = parseRight(eRight); 
                if (right instanceof UserRight)
                    sUserRights.put(name, (UserRight)right);
                else
                    sAdminRights.put(name, (AdminRight)right);
            } catch (ServiceException e) {
                throw ServiceException.PARSE_ERROR("unable to parse right: [" + name + "]", e);
            }
        }
    }
    
    public UserRight getUserRight(String right) throws ServiceException {
        UserRight r = sUserRights.get(right);
        if (r == null)
            throw ServiceException.FAILURE("invalid right " + right, null);
        return r;
    }
    
    public AdminRight getAdminRight(String right) throws ServiceException {
        AdminRight r = sAdminRights.get(right);
        if (r == null)
            throw ServiceException.FAILURE("invalid right " + right, null);
        return r;
    }
    
    public Right getRight(String right) throws ServiceException {
        Right r = sUserRights.get(right);
        if (r == null)
            r = sAdminRights.get(right);
        
        if (r == null)
            throw ServiceException.FAILURE("invalid right " + right, null);
        
        return r;
    }
    
    public Map<String, UserRight> getAllUserRights() {
        return sUserRights;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws ServiceException {
        RightManager rm = new RightManager("/Users/pshao/p4/main/ZimbraServer/conf/rights");
        /*
        for (Right r : RightManager.getInstance().getAllRights().values()) {
            System.out.println(r.getName());
            System.out.println("    desc: " + r.getDesc());
            System.out.println("    doc: " + r.getDoc());
            System.out.println();
        }
        */
        
    }
    
}
