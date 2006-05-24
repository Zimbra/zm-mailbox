package com.zimbra.cs.mime;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MimeCompoundHeader {
    private enum RFC2231State { PARAM, CONTINUED, EXTENDED, EQUALS, CHARSET, LANG, VALUE, QVALUE, SLOP };

    private class RFC2231Data {
        RFC2231State state = RFC2231State.EQUALS;
        StringBuilder key = null;
        StringBuilder value = new StringBuilder();
        boolean continued = false;
        boolean encoded = false;
        StringBuilder charset = null;
    
        void setState(RFC2231State newstate) {
            state = newstate;
            if (newstate == RFC2231State.PARAM) {
                key = new StringBuilder();  value = new StringBuilder();
                continued = false;  encoded = false;
            }
        }
    
        void setContinued()  { continued = true; }
        void setEncoded() {
            encoded = true;
            if (!continued)
                charset = new StringBuilder();
        }
    
        void addCharsetChar(char c)  { charset.append(c); }
        void addKeyChar(char c)      { key.append(c); }
        void addValueChar(char c)    { value.append(c); }
    
        void saveParameter(Map<String, String> attrs) {
            if (value == null)
                return;
            String pname = key == null ? null : key.toString().toLowerCase();
            String pvalue = value.toString();
            if ("".equals(pname) && "".equals(pvalue))
                return;
            if (encoded) {
                if (charset.equals(""))
                    charset.append("us-ascii");
                try {
                    pvalue = URLDecoder.decode(pvalue, charset.toString());
                } catch (UnsupportedEncodingException uee) { 
                    System.out.println(uee);
                }
            }
            String existing = continued ? attrs.get(pname) : null;
            attrs.put(pname, existing == null ? pvalue : existing + pvalue);
            key = null;  value = null;
        }
    }

    private String mValue;
    private Map<String, String> mParams = new HashMap<String, String>();

    public MimeCompoundHeader(String header) {
        if (header == null)
            return;
        header = header.trim();

        RFC2231Data rfc2231 = new RFC2231Data();
        boolean escaped = false;

        for (int i = 0, count = header.length(); i < count; i++) {
            char c = header.charAt(i);
            if (rfc2231.state == RFC2231State.SLOP) {
                if (c == ';' || c == '\n' || c == '\r')
                    rfc2231.setState(RFC2231State.PARAM);
            } else if (c == '\r' || c == '\n') {
                if (!mParams.isEmpty() || rfc2231.value.length() > 0) {
                    rfc2231.saveParameter(mParams);
                    rfc2231.setState(RFC2231State.PARAM);
                }
                // otherwise, it's just folding and we can effectively just ignore the CR/LF
            } else if (rfc2231.state == RFC2231State.PARAM) {
                if (c == '=')
                    rfc2231.setState(RFC2231State.EQUALS);
                else if (c == '*')
                    rfc2231.setState(RFC2231State.EXTENDED);
                else if (c != ' ' && c != '\t')
                    rfc2231.addKeyChar(c);
            } else if (rfc2231.state == RFC2231State.VALUE) {
                if (c != ';' && c != ' ' && c != '\t') {
                    rfc2231.addValueChar(c);
                } else {
                    rfc2231.saveParameter(mParams);
                    rfc2231.setState(c == ';' ? RFC2231State.PARAM : RFC2231State.SLOP);
                }
            } else if (rfc2231.state == RFC2231State.QVALUE) {
                if (!escaped && c == '\\') {
                    escaped = true;
                } else if (escaped || c != '"') {
                    rfc2231.addValueChar(c);  escaped = false;
                } else {
                    rfc2231.saveParameter(mParams);
                    rfc2231.setState(RFC2231State.SLOP);
                }
            } else if (rfc2231.state == RFC2231State.EQUALS) {
                if (c == ';') {
                    rfc2231.saveParameter(mParams);
                    rfc2231.setState(RFC2231State.PARAM);
                } if (c == '"') {
                    escaped = false;
                    rfc2231.setState(RFC2231State.QVALUE);
                } else {
                    rfc2231.addValueChar(c);
                    rfc2231.setState(RFC2231State.VALUE);
                }
            } else if (rfc2231.state == RFC2231State.EXTENDED) {
                if (c >= '0' && c <= '9') {
                    if (c != '0')
                        rfc2231.setContinued();
                    rfc2231.setState(RFC2231State.CONTINUED);
                } else if (c == '=') {
                    rfc2231.setEncoded();
                    rfc2231.setState(rfc2231.continued ? RFC2231State.VALUE : RFC2231State.CHARSET);
                }
            } else if (rfc2231.state == RFC2231State.CONTINUED) {
                if (c == '=')
                    rfc2231.setState(RFC2231State.EQUALS);
                else if (c == '*')
                    rfc2231.setState(RFC2231State.EXTENDED);
                else if (c >= '0' && c <= '9')
                    rfc2231.setContinued();
            } else if (rfc2231.state == RFC2231State.CHARSET) {
                if (c == '\'')
                    rfc2231.setState(RFC2231State.LANG);
                else
                    rfc2231.addCharsetChar(c);
            } else if (rfc2231.state == RFC2231State.LANG) {
                if (c == '\'')
                    rfc2231.setState(RFC2231State.VALUE);
            }
        }

        rfc2231.saveParameter(mParams);
        mValue = mParams.remove(null);
    }

    public String getValue()            { return mValue; }
    public void setValue(String value)  { mValue = value; }

    public boolean containsParameter(String name)        { return mParams.containsKey(name); }
    public String getParameter(String name)              { return mParams.get(name); }
    public void setParameter(String name, String value)  { mParams.put(name, value); }

    public Iterator<Map.Entry<String, String>> getParameterIterator()  { return mParams.entrySet().iterator(); }


    public static class ContentType extends MimeCompoundHeader {
        private String mPrimaryType, mSubType;

        public ContentType(String header)  { super(header);  parseValue(); }

        public void setValue(String value) { super.setValue(value);  parseValue(); }

        public String getPrimaryType()  { return mPrimaryType; }
        public String getSubType()      { return mSubType; }

        private void parseValue() {
            String value = getValue();
            if (value == null)
                setValue(Mime.CT_DEFAULT);
            else {
                value = value.trim();
                int slash = value.indexOf('/');
                if (slash <= 0 || slash >= value.length() - 1)
                    setValue(Mime.CT_DEFAULT);
                else {
                    mPrimaryType = value.substring(0, slash).trim();
                    mSubType = value.substring(slash + 1).trim();
                    if (mPrimaryType.equals("") || mSubType.equals(""))
                        setValue(Mime.CT_DEFAULT);
                    else 
                        super.setValue(value.toLowerCase());
                }
            }
        }
    }

    public static void main(String[] args) {
        MimeCompoundHeader mch;
        mch = new MimeCompoundHeader("text/plain; charset=US-ASCII;\r\n\tFormat=Flowed   DelSp=Yes\r\n");
        System.out.println(mch.getValue() + " - " + mch.mParams);
        mch = new MimeCompoundHeader("   \n  attachment;\n filename*=UTF-8''%E3%82%BD%E3%83%AB%E3%83%86%E3%82%A3%E3%83%AC%E3%82%A4.rtf\n  \n ");
        System.out.println(mch.getValue() + " - " + mch.mParams);
        mch = new MimeCompoundHeader("application/x-stuff; title*0*=us-ascii'en'This%20is%20even%20more%20; title*1*=%2A%2A%2Afun%2A%2A%2A%20; title*2=\"isn't it!\"\n");
        System.out.println(mch.getValue() + " - " + mch.mParams);
        mch = new MimeCompoundHeader("multipart/mixed; charset=us-ascii;\n foo=\n  boundary=\"---\" \n");
        System.out.println(mch.getValue() + " - " + mch.mParams);
        mch = new MimeCompoundHeader("message/external-body; access-type=URL;\n URL*0=\"ftp://\";\n URL*1=\"cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar\"\n");
        System.out.println(mch.getValue() + " - " + mch.mParams);
        mch = new MimeCompoundHeader("application/x-stuff;\n\ttitle*=us-ascii'en-us'This%20is%20%2A%2A%2Afun%2A%2A%2A");
        System.out.println(mch.getValue() + " - " + mch.mParams);
    }
}
