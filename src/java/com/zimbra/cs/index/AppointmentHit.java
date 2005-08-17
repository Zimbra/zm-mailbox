/*
 * Created on Feb 15, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.liquidsys.coco.index;

import org.apache.lucene.document.Document;

import com.liquidsys.coco.mailbox.Mailbox;

/**
 * @author tim
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AppointmentHit extends MessageHit {

    /**
     * @param results
     * @param mbx
     * @param d
     * @param score
     */
    public AppointmentHit(LiquidQueryResultsImpl results, Mailbox mbx, Document d,
            float score) {
        super(results, mbx, d, score);
    }

    /**
     * @param results
     * @param mbx
     * @param id
     * @param score
     */
    public AppointmentHit(LiquidQueryResultsImpl results, Mailbox mbx, int id,
            float score) {
        super(results, mbx, id, score);
    }

}
