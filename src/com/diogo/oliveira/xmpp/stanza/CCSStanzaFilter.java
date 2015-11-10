package com.diogo.oliveira.xmpp.stanza;

import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;

/**
 * @author Diogo Oliveira
 * @date 04/11/2015 12:09:38
 */
public class CCSStanzaFilter implements StanzaFilter
{
    private final String server_sender_id;

    public CCSStanzaFilter(String server_sender_id)
    {
        this.server_sender_id = server_sender_id;
    }

    @Override
    public boolean accept(Stanza stanza)
    {
        if(stanza.getClass() == Stanza.class)
        {
            return true;
        }
        else if(stanza.getTo() != null)
        {
            if(stanza.getTo().startsWith(server_sender_id))
            {
                return true;
            }
        }

        return false;
    }
}
