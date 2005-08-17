/*
 * Created on Apr 30, 2004
 */
package com.zimbra.cs.mime;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.DataSource;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.object.MatchedObject;
import com.zimbra.cs.object.ObjectHandler;
import com.zimbra.cs.object.ObjectHandlerException;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.BlobMetaData;

/**
 * @author schemers
 */
public abstract class MimeHandler {
    /**
     * The name of the HTTP request parameter used to identify 
     * an individual file within an archive. Its value is unspecified; 
     * it may be the full path within the archive, or a sequence number.
     * It should be understood between the archive handler and the servlet.
     * 
     */
    public static final String ARCHIVE_SEQUENCE = "archseq";
    
    public static final String CATCH_ALL_TYPE = "all";

    private static Log mLog = LogFactory.getLog(MimeHandler.class);
  
    private static Map mHandlers = new HashMap();
    
    /**
     * maps file extension to its mime type
     */
    static final int MAX_EXT_CACHE = 100;
    private static Map mExtToType;
    static {
        mExtToType = Collections.synchronizedMap(new LRUMap(MAX_EXT_CACHE));
    }
    
    protected MimeTypeInfo mMimeTypeInfo;

    private String mMessageDigest;
    
    /** dotted-number part name */
    private String mPartName;
    
    private String mFilename;
    
    private DataSource mDataSource;

    /**
     * Gets a handler based on the specified mime type. If no handler is found, 
     * then the text/plain handler is returned for text mime types, or the unknown
     * type handler is returned for other mime types.
     * 
     * @param mimeType
     * @return
     * @throws MimeHandlerException
     */
    public static MimeHandler getMimeHandler(String mimeType)
    	throws MimeHandlerException {
    	MimeHandler handler = null;
    	mimeType = Mime.contentTypeOnly(mimeType);
    	HandlerInfo handlerInfo = (HandlerInfo) mHandlers.get(mimeType);
    	if (handlerInfo == null)
    	    handlerInfo = loadHandler(mimeType);
        
    	handler = handlerInfo.getInstance();
    	return handler;
    }
    
    /**
     * To be overridden by "catch-all" mime handlers to set the real content type.
     * @param mimeType
     */
    protected void setContentType(String mimeType) {
        // do nothing
    }

    /**
     * @param mimeType
     * @return
     */
    private static synchronized HandlerInfo loadHandler(String mimeType) {
        HandlerInfo handlerInfo = null;
        try {
            MimeTypeInfo mt = Provisioning.getInstance().getMimeType(mimeType);
            if (mt == null || mt.getHandlerClass() == null) {
                boolean isTextType = mimeType.matches(Mime.CT_TEXT_WILD) ||
                    mimeType.equalsIgnoreCase(Mime.CT_MESSAGE_RFC822);
                // All unhandled text types default to text/plain handler.
                if (isTextType) {
                    mt = Provisioning.getInstance().getMimeType(Mime.CT_DEFAULT);
                    mLog.debug("falling back to " + Mime.CT_DEFAULT + " for: " + mimeType);
                } else {
                    mt = Provisioning.getInstance().getMimeType(CATCH_ALL_TYPE);
                    assert(mt != null);
                    if (mLog.isDebugEnabled())
                        mLog.debug("falling back to catch-all handler: " + 
                                mt.getHandlerClass() + " for unknown mime type: " + mimeType);
                }
            }
            String clazz = mt.getHandlerClass();
            assert(clazz != null);
            if (clazz.indexOf('.') == -1)
                clazz = "com.zimbra.cs.mime.handler." + clazz;
            try {
                handlerInfo = new HandlerInfo();
                handlerInfo.mClass = Class.forName(clazz);
                handlerInfo.mMimeType = mt;
                handlerInfo.mRealMimeType = mimeType;
                mHandlers.put(mimeType, handlerInfo);
            } catch (Exception e) {
                if (mLog.isWarnEnabled())
                    mLog.warn("loadHandler caught exception", e);
            }
        } catch (ServiceException e) {
            if (mLog.isErrorEnabled())
                mLog.error("loadHandler caught SQLException", e);
        } 
        return handlerInfo;
    } 
    
    public String getContentType() {
        return mMimeTypeInfo.getType();
    }
    
    public String getDescription() {
        return mMimeTypeInfo.getDescription();
    }
    
    public boolean isIndexingEnabled() {
        return mMimeTypeInfo.isIndexingEnabled();
    }
    
    private static final int INT_MAX_STR_LEN = 10; // max length of an Integer.toString
    
    private static String sizeString(int size)
    {
    	String val = Integer.toString(size);
    	if (val.length() < INT_MAX_STR_LEN) {
    		val = "0000000000".substring(0, INT_MAX_STR_LEN - val.length()) + val;
    	}
    	return val;
    }
    
    /**
     * Initializes the data source for text extraction.
     * 
     * @param source
     * @throws MimeHandlerException
     * @see #getContentImpl()
     * @see #addFields(Document)
     */
    public void init(DataSource source) throws MimeHandlerException {
        mDataSource = source;
    }
    
    void setPartName(String partName) {
    	mPartName = partName;
    }
    
    public String getPartName() {
        return mPartName;
    }
    
    void setFilename(String filename) {
        mFilename = filename;
    }

    public String getFilename() {
        return mFilename;
    }
    
    void setMessageDigest(String digest) {
        mMessageDigest = digest;
    }
    
    public String getMessageDigest() {
        return mMessageDigest;
    }
    
    public DataSource getDataSource() {
        return mDataSource;
    }
    
    /**
     * Adds the indexed fields to the Lucene document for search. Each handler determines
     * a set of fields that it deems important for the type of documents it handles.
     * 
     * @param doc
     * @throws MimeHandlerException
     */
    public abstract void addFields(Document doc) throws MimeHandlerException;
    
    /**
     * Gets the text content of the document.
     * 
     * @return
     * @throws MimeHandlerException
     */
    public final String getContent() throws MimeHandlerException {
    	if (!DebugConfig.disableMimePartExtraction)
            return getContentImpl();
        else {
            if (!mDrainedContent) {
                try {
                    InputStream is = getDataSource().getInputStream();
                    // Read all bytes from the input stream and discard them.
                    // This is useful for testing MIME parser performance, as
                    // the parser may not fully parse the message unless the
                    // message part is read in its entirety.
                    // Multiple buffers will share sDrainBuffer without any
                    // synchronizing, but this is okay since we don't use the
                    // data read.
                    while (is.read(sDrainBuffer) != -1) {}
                } catch (IOException e) {
                    throw new MimeHandlerException("cannot extract text", e);
                }
                mDrainedContent = true;
            }
        	return "";
        }
    }
    private boolean mDrainedContent = false;
    private static byte[] sDrainBuffer = new byte[4096];

    protected abstract String getContentImpl() throws MimeHandlerException;
    
    /**
     * Converts the document into HTML/images for viewing.
     * 
     * @param doc
     * @param baseURL
     * @return path to the main converted HTML file.
     * @throws IOException
     * @throws ConversionException
     */
    public abstract String convert(AttachmentInfo doc, String baseURL) throws IOException, ConversionException;
    
    /**
     * Deterimines if this handler can process archive files (zip, tar, etc.).
     * 
     * @return true if the handler can handle archive, false otherwise.
     */
    public boolean handlesArchive() {
        return false;
    }
    
    /**
     * Determines if this handler can convert the document into HTML/images.
     * If this method returns false, then the original document will be returned to the browser.
     * @return
     */
    public abstract boolean doConversion();

    /**
     * 
     * @param doc
     * @param source
     * @throws ObjectHandlerException
     * @throws ServiceException
     */
    public Document getDocument()
    	throws MimeHandlerException, ObjectHandlerException, ServiceException {
        
        /*
         * Initialize the F_L_TYPE field with the content type from the
         * specified DataSouce. Additionally, if DataSource is an instance
         * of BlobDataSource (which it always should when creating a document), 
         * then also initialize F_L_BLOB_ID and F_L_SIZE fields.
         */
        
    	Document doc = new Document();
    	String contentType = getContentType();
    	doc.add(Field.Text(LuceneFields.L_MIMETYPE, contentType));
    	addFields(doc);
        String content;
    	content = getContent();
    	doc.add(Field.UnStored(LuceneFields.L_CONTENT, content));
        getObjects(content, doc);
//        if (mPartName.equals("")) {
//            doc.add(Field.Keyword(LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_NONE));
//        } else {
            doc.add(Field.Keyword(LuceneFields.L_PARTNAME, mPartName));
//        }
    	String name = mDataSource.getName();
    	if (name != null)
    	    doc.add(Field.Text(LuceneFields.L_FILENAME, name));
    	return doc;
    }
    
    public static void getObjects(String text, Document doc) throws ObjectHandlerException, ServiceException {
        if (DebugConfig.disableObjects)
            return;

        List objects = ObjectHandler.getObjectHandlers();
        StringBuffer l_objects = new StringBuffer();
        for (Iterator oit=objects.iterator(); oit.hasNext();) {
            ObjectHandler h = (ObjectHandler) oit.next();
            if (!h.isIndexingEnabled())
                continue;
            ArrayList matchedObjects = new ArrayList();
            h.parse(text, matchedObjects, !h.storeMatched());
            if (!matchedObjects.isEmpty()) {
                     if (l_objects.length() > 0)
                    l_objects.append(',');
                l_objects.append(h.getType());

                if (h.storeMatched()) {
                    HashSet set = new HashSet();
                    for (Iterator mit = matchedObjects.iterator(); mit.hasNext(); ) {
                        MatchedObject mo = (MatchedObject) mit.next();
                        set.add(mo.getMatchedText());
                    }
                
                    StringBuffer md = new StringBuffer();
                    int i=0;                
                    for (Iterator sit = set.iterator(); sit.hasNext();) {
                        //TODO: check md.length() and set an upper bound on
                        // how big we'll let the field be? Per-object or 
                        // system-wide policy?
                        BlobMetaData.encodeMetaData(Integer.toString(i++), (String)sit.next(), md);
                    }
                    String fname = "l.object."+h.getType();
                    doc.add(Field.UnIndexed(fname, md.toString()));
                }
            }
        }
        if (l_objects.length() > 0)
        	doc.add(Field.UnStored(LuceneFields.L_OBJECTS, l_objects.toString()));
    }
    
    private static class HandlerInfo {
        MimeTypeInfo mMimeType;
        Class mClass;
        String mRealMimeType;
        
        /**
         * @return
         */
        public MimeHandler getInstance() throws MimeHandlerException {
            MimeHandler handler;
            try {
                handler = (MimeHandler) mClass.newInstance();
            } catch (InstantiationException e) {
                throw new MimeHandlerException(e);
            } catch (IllegalAccessException e) {
                throw new MimeHandlerException(e);
            }
            handler.setContentType(mRealMimeType);
            handler.mMimeTypeInfo = mMimeType;
            return handler;
        }
    }

    /**
     * @param in
     * @param seq
     * @return
     */
    public AttachmentInfo getDocInfoFromArchive(AttachmentInfo archiveDocInfo, String seq) 
        throws IOException
    {
        return null;
    }

    /**
     * Returns the content type corresponding to the extension.
     * 
     * @param ext
     * @return
     */
    public static String getContentTypeByExtension(String ext) {
        try {
            String t = (String) mExtToType.get(ext.toLowerCase());
            if (t != null)
                return t;
            MimeTypeInfo mt = Provisioning.getInstance()
                    .getMimeTypeByExtension(ext);
            t = (mt == null ? CATCH_ALL_TYPE : mt.getType());
            mExtToType.put(ext.toLowerCase(), t);
            return t;
        } catch (ServiceException e) {
            mLog.error("Cannot get mime type for extension " + ext);
            return CATCH_ALL_TYPE;
        }
    }
}
