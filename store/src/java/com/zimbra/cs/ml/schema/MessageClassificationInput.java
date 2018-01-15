package com.zimbra.cs.ml.schema;

import java.util.List;

import javax.mail.internet.MimeMessage;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.ml.feature.ComputedFeatures;
import com.zimbra.cs.ml.feature.Feature;

/**
 * Input into a message ClassificationQuery.
 *
 */
public class MessageClassificationInput extends AbstractClassificationInput<Message> {

    private String url;
    private String text;
    private MimeMessage mm;

    public MessageClassificationInput() {
        super();
    }

    public MessageClassificationInput(ComputedFeatures<Message> features) throws ServiceException {
        super(features);
    }

    public String getUrl() {
        return url;
    }

    public String getText() {
        return text;
    }

    public MimeMessage getMimeMessage() {
        return mm;
    }

    public List<Float> getFeatures() {
        return encodedFeatures;
    }

    public MessageClassificationInput setUrl(String url) {
        this.url = url;
        return this;
    }

    public MessageClassificationInput setMimeMessage(MimeMessage mm) {
        this.mm = mm;
        return this;
    }

    public MessageClassificationInput setText(String text) {
        this.text = text;
        return this;
    }

    public void addFloatFeature(float feature) {
        encodedFeatures.add(feature);
    }

    public void addIntFeature(int feature) {
        encodedFeatures.add((float) feature);
    }

    public void addBoolFeature(boolean feature) {
        encodedFeatures.add((float) (feature ? 1 : 0));
    }

    public void addFeature(Object obj) throws ServiceException {
        if (obj instanceof Boolean) {
            addBoolFeature((Boolean) obj);
        } else if (obj instanceof Float) {
            addFloatFeature((Float) obj);
        } else if (obj instanceof Integer) {
            addIntFeature((Integer) obj);
        } else if (obj instanceof Double) {
            addFloatFeature(((Double) obj).floatValue());
        }else {
            ZimbraLog.ml.warn("cannot encode feature of type " + obj.getClass().getName());
        }

    }

    @Override
    protected void init(ComputedFeatures<Message> computedFeatures) throws ServiceException {
        Message msg = computedFeatures.getItem();
        setMimeMessage(msg.getMimeMessage());
        String url = msg.getBlob().getLocator();
        if (url != null) {
            setUrl(url);
        }
        for (Feature<?> feature: computedFeatures.getFeatures()) {
            addFeature(feature.getFeatureValue());
        }
    }
}
