package com.diogo.oliveira.xmpp.connection;

import java.io.IOException;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.DefaultExtensionElement;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import static com.diogo.oliveira.xmpp.util.Constants.GCM_ELEMENT_NAME;
import static com.diogo.oliveira.xmpp.util.Constants.GCM_NAMESPACE;

/**
 * @author Diogo Oliveira
 * @date 10/11/2015 15:27:49
 */
public class Connect extends Connection
{
    static
    {
        ProviderManager.addExtensionProvider(GCM_ELEMENT_NAME, GCM_NAMESPACE, new ExtensionElementProvider<ExtensionElement>()
        {
            @Override
            public DefaultExtensionElement parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException
            {
                String json = parser.nextText();
                return new GcmPacketExtension(json);
            }
        });
    }

    public Connect(String senderId, String apiKey) throws SmackException, IOException, XMPPException
    {
        super(senderId, apiKey);
    }
}
