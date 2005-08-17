/*
 * Created on Oct 15, 2004
 */
package com.zimbra.cs.index;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;


import java.util.*;
//import java.io.IOException;
//
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//
//import org.apache.lucene.search.*;
import org.apache.lucene.document.*;

/**
 * @author tim
 * 
 * Really, this class should be renamed to LiquidQueryResultsImpl, and the 
 * SuperInterface should be LiquidQueryResultsImpl -- will fix this as soon as I 
 * get some free time.
 */
abstract class LiquidQueryResultsImpl implements LiquidQueryResults
{
    /////////////////////////
    //
    // These come from the LiquidQueryResults interface:
    //
    // void resetIterator() throws ServiceException;
    // LiquidHit getNext() throws ServiceException;
    // LiquidHit peekNext() throws ServiceException;
    //
    
    public abstract void doneWithSearchResults() throws ServiceException;
    public abstract LiquidHit skipToHit(int hitNo) throws ServiceException;
    
    public boolean hasNext() throws ServiceException {
        return (peekNext() != null);
    }
    
    private HashMap mConversationHits;
    private HashMap mMessageHits;
    private HashMap mPartHits;
    private HashMap mContactHits;
    private HashMap mNoteHits;
  
  LiquidQueryResultsImpl(byte[] types, int searchOrder) { 
      mTypes = types;
      for (int i = 0; i < types.length; i++) {
          switch(types[i]) {
          case 0: // everything!
              mReturnMessages = true;
              mReturnContacts = true;
              mReturnAppointments = true;
              mReturnNotes = true;
              break;
          case MailItem.TYPE_CONVERSATION:
          case MailItem.TYPE_MESSAGE:
              mReturnMessages = true;
              break;
          case MailItem.TYPE_CONTACT:
              mReturnContacts = true;
              break;
//          case MailItem.TYPE_INVITE:
//              mReturnAppointments = true;
//              break;
          case MailItem.TYPE_NOTE:
              mReturnNotes = true;
              break;
          }
      }
      
      mSearchOrder = searchOrder;
      
      mConversationHits = new LinkedHashMap();
      mMessageHits = new LinkedHashMap();
      mPartHits = new LinkedHashMap();
      mContactHits = new LinkedHashMap();
      mNoteHits = new LinkedHashMap();
  };
  
  public LiquidHit getFirstHit() throws ServiceException {
      resetIterator();
      return getNext();
  }
  
  private byte[] mTypes;
  private int mSearchOrder;
  
  int getSearchOrder() {
      return mSearchOrder;
  }
  
  byte[] getTypes() { 
      return mTypes;
  }
  
  boolean mReturnContacts = false;
  boolean mReturnMessages = false;
  boolean mReturnNotes = false;
  boolean mReturnAppointments = false;
  
  boolean canReturnContacts() { return mReturnContacts; };
  boolean canReturnMessages() { return mReturnMessages; };
  boolean canReturnNotes() { return mReturnNotes; };
  boolean canReturnAppointments() { return mReturnAppointments; };
  
  protected ConversationHit getConversationHit(Mailbox mbx, Integer convId, float score) {
      ConversationHit ch = (ConversationHit) mConversationHits.get(convId);
      if (ch == null) {
          ch = new ConversationHit(this, mbx, convId, score);
          mConversationHits.put(convId, ch);
      } else {
          ch.updateScore(score);
      }
      return ch;
  }

  protected ContactHit getContactHit(Mailbox mbx, Document d, float score) {
      Integer blobId = Integer.decode(d.get(LuceneFields.L_MAILBOX_BLOB_ID));
      return getContactHit(mbx, blobId, d, score);
  }
  
  protected ContactHit getContactHit(Mailbox mbx, Integer blobId, Document d, float score) {
      ContactHit hit = (ContactHit) mContactHits.get(blobId);
      if (hit == null) {
          if (d != null) {
              hit = new ContactHit(this, mbx, blobId.intValue(), d, score);
          } else {
              hit = new ContactHit(this, mbx, blobId.intValue(), d, score);
          }
          mContactHits.put(blobId, hit);
      } else {
          hit.updateScore(score);
      }
      return hit;
  }
  
  protected NoteHit getNoteHit(Mailbox mbx, Document d, float score) {
      Integer blobId = Integer.decode(d.get(LuceneFields.L_MAILBOX_BLOB_ID));
      return getNoteHit(mbx, blobId, d, score);
  }

  protected NoteHit getNoteHit(Mailbox mbx, Integer blobId, Document d, float score) {
      NoteHit hit = (NoteHit) mNoteHits.get(blobId);
      if (hit == null) {
          if (d != null) {
              hit = new NoteHit(this, mbx, d, score);
          } else {
              hit = new NoteHit(this, mbx, d, score);
          }
          mNoteHits.put(blobId, hit);
      } else {
          hit.updateScore(score);
      }
      return hit;
  }
  

  protected MessageHit getMessageHit(Mailbox mbx, Document d, float score) {
      Integer messageId = Integer.decode(d.get(LuceneFields.L_MAILBOX_BLOB_ID));
      return getMessageHit(mbx, messageId, d, score);
  }

  protected MessageHit getMessageHit(Mailbox mbx, Integer messageId, Document d, float score) {
      MessageHit hit = (MessageHit) mMessageHits.get(messageId);
      if (hit == null) {
          if (d != null) {
              hit = new MessageHit(this, mbx, d, score);
          } else {
              hit = new MessageHit(this, mbx, messageId.intValue(), score);
          }
          mMessageHits.put(messageId, hit);
      } else {
          hit.updateScore(score);
      }
      return hit;
  }

  protected MessagePartHit getMessagePartHit(Mailbox mbx, Document d, float score) 
  {
      String partKey = d.get(LuceneFields.L_MAILBOX_BLOB_ID) + "-" + d.get(LuceneFields.L_PARTNAME);
      MessagePartHit hit = (MessagePartHit) mPartHits.get(partKey);
      if (hit == null) {
          hit = new MessagePartHit(this, mbx, d, score);
          mPartHits.put(partKey, hit);
      } else {
          hit.updateScore(score);
      }
      return hit;
  }    
  
  protected MessagePartHit getMessagePartHit(Mailbox mbx, Integer mailboxBlobId, 
          String partName, Document d, float score) 
  {
      String partKey = mailboxBlobId.toString()+"-"+partName;
      MessagePartHit hit = (MessagePartHit) mPartHits.get(partKey);
      if (hit == null) {
          hit = new MessagePartHit(this, mbx, d, score);
          mPartHits.put(partKey, hit);
      } else {
          hit.updateScore(score);
      }
      return hit;
  }    
}
