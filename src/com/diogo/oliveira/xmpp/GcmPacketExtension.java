package com.diogo.oliveira.xmpp;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.packet.DefaultExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.StringUtils;

import static com.diogo.oliveira.xmpp.util.Constants.GCM_ELEMENT_NAME;
import static com.diogo.oliveira.xmpp.util.Constants.GCM_NAMESPACE;

/**
 *
 * @author Diogo Oliveira
 * @date 06/11/2015 12:00:42
 */
public class GcmPacketExtension extends DefaultExtensionElement
{
    private static final Logger LOGGER = Logger.getLogger(GcmPacketExtension.class.getName());
    private final String json;

    public GcmPacketExtension(String json)
    {
        super(GCM_ELEMENT_NAME, GCM_NAMESPACE);
        this.json = json;
    }

    public String getJson()
    {
        return json;
    }

    @Override
    public CharSequence toXML()
    {
        /* REMOVER QUANDO RELEASE */
        LOGGER.log(Level.INFO, json);
        LOGGER.log(Level.INFO, String.format("<%s xmlns=\"%s\">%s</%s>", GCM_ELEMENT_NAME, GCM_NAMESPACE, StringUtils.escapeForXML(json), GCM_ELEMENT_NAME));

        return (String.format("<%s xmlns=\"%s\">%s</%s>", GCM_ELEMENT_NAME, GCM_NAMESPACE, StringUtils.escapeForXML(json), GCM_ELEMENT_NAME));
    }

    public Stanza toPacket()
    {
        Message message = new Message();
        message.addExtension(this);
        return message;
    }
}
