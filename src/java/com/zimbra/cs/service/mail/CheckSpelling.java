package com.zimbra.cs.service.mail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import com.zimbra.cs.localconfig.LC;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.util.RemoteServerRequest;
import com.zimbra.cs.util.LiquidLog;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraContext;

/**
 * @author bburtin
 */
public class CheckSpelling extends DocumentHandler  {
    
    private long mFailTimestamp = 0;
    private boolean mWasSuccessful = true;

    /**
     * Returns the url of the spell server, or <code>null</code> if
     * the spell server hostname is not specified in LocalConfig.
     */
    private String getSpellServerUrl() {
        String hostname = LC.spell_hostname.value();
        if (StringUtil.isNullOrEmpty(hostname)) {
            return null;
        }
        int port = LC.spell_port.intValue();
        return "http://" + hostname + ":" + port + "/php/aspell.php";
    }

    public Element handle(Element request, Map context) {
        ZimbraContext lc = getZimbraContext(context);
        Element response = lc.createElement(MailService.CHECK_SPELLING_RESPONSE);

        // Make sure that the hostname is specified
        String spellServerUrl = getSpellServerUrl();
        LiquidLog.mailbox.debug(
            "CheckSpelling: spellServerUrl='" + spellServerUrl + "', mWasSuccessful=" +
            mWasSuccessful + ", mFailTimestamp=" + mFailTimestamp);
        if (spellServerUrl == null) {
            response.addAttribute(MailService.A_AVAILABLE, false);
            return response;
        }
        
        // If the last spell check attempt failed, wait before retrying
        if (!mWasSuccessful) {
            long timePassed = System.currentTimeMillis() - mFailTimestamp;
            if (timePassed < LC.spell_retry_interval_millis.intValue()) {
                response.addAttribute(MailService.A_AVAILABLE, false);
                return response;
            }
        }
        
        String text = request.getTextTrim();
        RemoteServerRequest req = new RemoteServerRequest();
        req.addParameter("text", text);
        
        try {
            req.invoke(spellServerUrl);
            Map params = req.getResponseParameters();
            String misspelled = (String) params.get("misspelled");
            if (misspelled == null) {
                throw new IOException("misspelled data not found in spell server response");
            }

            // Assemble SOAP response
            BufferedReader reader =
                new BufferedReader(new StringReader(misspelled));
            String line = null;
            int numLines = 0;
            int numMisspelled = 0;
            while ((line = reader.readLine()) != null) {
                // Each line in the response has the format "werd:word,ward,weird"
                line = line.trim();
                numLines++;
                int colonPos = line.indexOf(':');
                
                if (colonPos >= 0) {
                    Element wordEl = response.addElement(MailService.E_MISSPELLED);
                    String word = line.substring(0, colonPos);
                    String suggestions = line.substring(colonPos + 1, line.length());
                    wordEl.addAttribute(MailService.A_WORD, word);
                    wordEl.addAttribute(MailService.A_SUGGESTIONS, suggestions);
                    numMisspelled++;
                }
            }
            response.addAttribute(MailService.A_AVAILABLE, true);
            LiquidLog.mailbox.debug(
                "CheckSpelling: found " + numMisspelled + " misspelled words in " + numLines + " lines");
        } catch (IOException ex) {
            LiquidLog.mailbox.error("An error occurred while contacting the spelling server", ex);
            mWasSuccessful = false;
            mFailTimestamp = System.currentTimeMillis();
            response.addAttribute(MailService.A_AVAILABLE, false);
            return response;
        }
        
        return response;
    }
}
