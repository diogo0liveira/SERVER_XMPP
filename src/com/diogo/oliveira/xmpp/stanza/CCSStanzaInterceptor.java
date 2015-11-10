package com.diogo.oliveira.xmpp.stanza;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.packet.Stanza;

/**
 * @author Diogo Oliveira
 * @date 04/11/2015 12:34:38
 */
public class CCSStanzaInterceptor implements StanzaListener
{
    @Override
    public void processPacket(Stanza stanza) throws SmackException.NotConnectedException
    {
        Logger.getLogger(CCSStanzaInterceptor.class.getName()).log(Level.INFO, "Sent: {0}", stanza.toXML());
    }
}
