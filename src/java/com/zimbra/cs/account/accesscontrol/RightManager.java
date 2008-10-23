package com.zimbra.cs.account.accesscontrol;

import java.io.File;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class RightManager {
    private static final String E_DEFAULT      = "default";
    private static final String E_DESC         = "desc";
    private static final String E_DOC          = "doc";
    private static final String E_RIGHTS       = "rights";
    private static final String E_RIGHT        = "right";
    private static final String A_NAME         = "name";
    private static final String A_USER_RIGHT   = "userRight";
    
    private static RightManager mInstance;
    private Map<String, Right> sRights = new TreeMap<String, Right>();  // keep the map sorted so "zmmailbox lp" can display in alphabetical order 

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
    
    private Right parseRight(Element eRight, String name) throws ServiceException {
        boolean userRight = getBooleanAttr(eRight, A_USER_RIGHT, false);
        Right right = userRight? new UserRight(name) : new AdminRight(name);

        for (Iterator elemIter = eRight.elementIterator(); elemIter.hasNext();) {
            Element elem = (Element)elemIter.next();
            if (elem.getName().equals(E_DESC))
                right.setDesc(elem.getText());
            else if (elem.getName().equals(E_DOC))
                right.setDoc(elem.getText());
            else if (elem.getName().equals(E_DEFAULT)) {
                String defaultValue = elem.getText();
                if ("true".equalsIgnoreCase(defaultValue))
                    right.setDefault(Boolean.TRUE);
                else if ("false".equalsIgnoreCase(defaultValue))
                    right.setDefault(Boolean.FALSE);
                else
                    throw ServiceException.PARSE_ERROR("invalid default value: " + defaultValue, null);
            } else
                throw ServiceException.PARSE_ERROR("invalid element: " + elem.getName(), null);
        }
        if (right.getDesc() == null)
            throw ServiceException.PARSE_ERROR("missing desc", null);

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
            
            if (sRights.get(name) != null)
                throw ServiceException.PARSE_ERROR("right " + name + " is already defined", null);
            
            try {
                Right right = parseRight(eRight, name);            
                sRights.put(name, right);
            } catch (ServiceException e) {
                throw ServiceException.PARSE_ERROR("unable to parse right: [" + name + "]", e);
            }
        }
    }
    
    public Right getRight(String right) throws ServiceException {
        Right r = sRights.get(right);
        if (r == null)
            throw ServiceException.FAILURE("invalid right " + right, null);
        return r;
    }
    
    public Map<String, Right> getAllRights() {
        return sRights;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws ServiceException {
        for (Right r : RightManager.getInstance().getAllRights().values()) {
            System.out.println(r.getName());
            System.out.println("    desc: " + r.getDesc());
            System.out.println("    doc: " + r.getDoc());
            System.out.println();
        }
    }
    
}
