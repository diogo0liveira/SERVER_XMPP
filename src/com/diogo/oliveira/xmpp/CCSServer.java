package com.diogo.oliveira.xmpp;

import com.diogo.oliveira.xmpp.connection.Connect;
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
 *
 * @author Diogo Oliveira
 * @date 05/11/2015 09:42:21
 */
public class CCSServer extends Connect
{
//    public static void main(String[] args) throws Exception
//    {
//        CCSServer server = new CCSServer(GCM_SENDER_ID, GCM_API_KEY);
//
//        String messageId = server.nextMessageId();
//        Map<String, String> payload = new HashMap<>();
//        payload.put("Message", "DIOGO ARAUJO OLIVEIRA");
//        payload.put("CCS", "Dummy Message");
//        payload.put("EmbeddedMessageId", messageId);
//        String collapseKey = "sample";
//        Long timeToLive = 10000L;
//        String message = createJsonMessage(REGISTRATION_ID, messageId, payload, collapseKey, timeToLive, true);
//
//        server.sendDownstreamMessage(message);
//
//        while(true)
//        {
//        }
//    }

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

    public CCSServer(String senderId, String apiKey) throws SmackException, IOException, XMPPException
    {
        super(senderId, apiKey);
    }
}
