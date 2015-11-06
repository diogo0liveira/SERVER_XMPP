package com.diogo.oliveira.server.xmpp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocketFactory;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.DefaultExtensionElement;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 *
 * @author Diogo Oliveira
 * @date 05/11/2015 09:42:21
 */
public class CCSServer
{
    public static final String GCM_NAMESPACE = "google:mobile:data";
    public static final String GCM_ELEMENT_NAME = "gcm";

    private static final String GCM_SERVER = "gcm.googleapis.com";
    private static final int GCM_PORT = 5235;

    private static final String REGISTRATION_ID = "cVnzMzdNcHo:APA91bFlv-Jbi9kZqRz7qL-LrlM5nVRGAvs29K87Ubi-pK8yCLuXwiudkhO-RTzswPh69BhBP1-Ygb--JzN1tok_3p773R6MehbwZxLOFJSpa2qTtdq_YhK6LpD5bDaAbm9Ql_1V7z_A";
    private static final String GCM_API_KEY = "AIzaSyD3Ai1ulbjds4Hjd2UCmbZiiwh-ccSYMew";
    private static final String GCM_SENDER_ID = "737179229350";

    private static final Logger LOGGER = Logger.getLogger(CCSServer.class.getName());
    protected volatile boolean connectionDraining = false;
    private XMPPTCPConnection connection;

    private enum MessageType
    {
        ACK("ack"), NACK("nack"), NORMAL(null), CONTROL("control"), UNRECOGNIZED("unrecognized");

        public String value;

        MessageType(String value)
        {
            this.value = value;
        }

        public static MessageType get(Object value)
        {
            for(MessageType intervalo : MessageType.values())
            {
                if(intervalo.value.equals(value))
                {
                    return intervalo;
                }
            }

            return UNRECOGNIZED;
        }
    }

//    public static void main(String[] args) throws Exception
//    {
//        CCSServer server = new CCSServer();
//        server.connect(GCM_SENDER_ID, GCM_API_KEY);
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

    private static final class GcmPacketExtension extends DefaultExtensionElement
    {
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

    public void connect(String senderId, String apiKey) throws XMPPException, IOException, SmackException
    {
        XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                .setServiceName(GCM_SERVER)
                .setHost(GCM_SERVER)
                .setPort(GCM_PORT)
                .setSendPresence(false)
                .setConnectTimeout(30000)
                .setDebuggerEnabled(false)
                .setCompressionEnabled(false)
                .setSocketFactory(SSLSocketFactory.getDefault())
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .build();

        connection = new XMPPTCPConnection(configuration);

        /* TESTAR */
        Roster roster = Roster.getInstanceFor(connection);
        roster.setRosterLoadedAtLogin(false);

        connection.connect();
        connection.addConnectionListener(new LoggingConnectionListener());
        connection.addAsyncStanzaListener(new CCSStanzaListener(), new CCSStanzaFilter());
        connection.addPacketInterceptor(new CCSStanzaInterceptor(), new CCSStanzaFilter());
        connection.login(senderId + "@gcm.googleapis.com", apiKey);
    }

    /**
     * Retorna um ID de mensagem aleatória para identificar exclusivamente uma mensagem.
     * <p>
     * <p>
     * Nota: This is generated by a pseudo random number generator for illustration purpose, and is not guaranteed to be unique.
     *
     * @return
     */
    public String nextMessageId()
    {
        return "msgid:" + UUID.randomUUID().toString();
    }

    /**
     * Envia uma mensagem jusante.
     *
     * @param jsonRequest Mensagem a ser enviada no formato json.
     *
     * @throws NotConnectedException
     */
    protected void send(String jsonRequest) throws NotConnectedException
    {
        connection.sendStanza(new GcmPacketExtension(jsonRequest).toPacket());
    }

    /**
     * Sends a downstream message to GCM.
     *
     * @param jsonRequest Mensagem a ser enviada no formato json.
     *
     * @return true if the message has been successfully sent.
     *
     * @throws NotConnectedException
     */
    public boolean sendDownstreamMessage(String jsonRequest) throws NotConnectedException
    {
        if(!connectionDraining)
        {
            send(jsonRequest);
            return true;
        }

        LOGGER.info("Descartando mensagem a jusante uma vez que a conexão está draining.");
        return false;
    }

    /**
     * Cria uma mensagem de ACK JSON para uma mensagem upstream recebida de um aplicativo.
     *
     * @param to        registrationId do dispositivo que enviou a mensagem.
     * @param messageId messageId da mensagem.
     *
     * @return
     */
    protected static String createJsonACK(String to, String messageId)
    {
        Map<String, Object> message = new HashMap<>();
        message.put("message_type", "ack");
        message.put("to", to);
        message.put("message_id", messageId);

        /* REMOVER QUANDO RELEASE */
        LOGGER.info(JSONValue.toJSONString(message));

        return JSONValue.toJSONString(message);
    }

    /**
     * Cria uma mensagem JSON codificado.
     *
     * @param to             Dispositivo de destino (Obrigatório).
     * @param messageId      Único para o qual CCS irá enviar um "ACK/NACK" (Obrigatório).
     * @param payload        O conteúdo da mensagem pretendida para a aplicação (Opcional).
     * @param collapseKey    GCM parâmetro collapse_key (Opcional).
     * @param timeToLive     GCM time_to_live (Opcional).
     * @param delayWhileIdle GCM parâmetro delay_while_idle (Optional).
     *
     * @return JSON encoded GCM message.
     */
    public static String createJsonMessage(String to, String messageId, Map<String, String> payload, String collapseKey, Long timeToLive, Boolean delayWhileIdle)
    {
        Map<String, Object> message = new HashMap<>();
        message.put("message_id", messageId);
        message.put("data", payload);
        message.put("to", to);

        if(collapseKey != null)
        {
            message.put("collapse_key", collapseKey);
        }
        if(timeToLive != null)
        {
            message.put("time_to_live", timeToLive);
        }
        if(delayWhileIdle != null && delayWhileIdle)
        {
            message.put("delay_while_idle", true);
        }

        return JSONValue.toJSONString(message);
    }

    /**
     * Puxador de mensagem a partir de um aplicativo do dispositivo.
     * <p>
     * <p>
     * Este servidor envia uma mensagem de eco de volta ao dispositivo. Subclasses deve substituir esse método para processar corretamente
     * as mensagens montante.
     *
     * @param jsonObject Mensagem a ser enviada no formato json.
     */
    protected void processMessageUpstream(Map<String, Object> jsonObject)
    {
        String category = (String)jsonObject.get("category");
        String from = (String)jsonObject.get("from");

        @SuppressWarnings("unchecked")
        Map<String, String> payload = (Map<String, String>)jsonObject.get("data");
        payload.put("ECHO", "Application: " + category);

        /* Enviar uma resposta ECHO de volta */
        String echo = createJsonMessage(from, nextMessageId(), payload,
                "echo:CollapseKey", null, false);

        try
        {
            sendDownstreamMessage(echo);
        }
        catch(NotConnectedException e)
        {
            LOGGER.log(Level.WARNING, "Não conectado, mensagem de eco não é enviado", e);
        }
    }

    protected void processMessageControl(Map<String, Object> jsonObject)
    {
        LOGGER.log(Level.INFO, "handleControlMessage(): {0}", jsonObject);
        String controlType = (String)jsonObject.get("control_type");

        if("CONNECTION_DRAINING".equals(controlType))
        {
            connectionDraining = true;
        }
        else
        {
            LOGGER.log(Level.INFO, "Unrecognized control type: %s. This could happen ifnew features are " + "added to the CCS protocol.", controlType);
        }
    }
    
        /**
     * Trata uma mensagem ACK.
     * <p>
     * <p>
     * Registra uma mensagem INFO, mas subclasses poderia substituí-lo para tratar adequadamente ack.
     *
     * @param jsonObject Mensagem a ser enviada no formato json.
     */
    protected void processReceiptACK(Map<String, Object> jsonObject)
    {
        LOGGER.log(Level.INFO, "handleAckReceipt() from: {0}, messageId: {1}", new Object[]
        {
            (String)jsonObject.get("from"), (String)jsonObject.get("message_id")
        });
    }

    /**
     * Trata uma mensagem NACK.
     * <p>
     * <p>
     * Registra uma mensagem INFO, mas subclasses poderia substituí-lo para tratar adequadamente nack.
     *
     * @param jsonObject Mensagem a ser enviada no formato json.
     */
    protected void processReceiptNACK(Map<String, Object> jsonObject)
    {
        LOGGER.log(Level.INFO, "handleNackReceipt() from: {0}, messageId: {1}", new Object[]
        {
            (String)jsonObject.get("from"), (String)jsonObject.get("message_id")
        });
    }

    private static final class CCSStanzaFilter implements StanzaFilter
    {
        @Override
        public boolean accept(Stanza stanza)
        {
            if(stanza.getClass() == Stanza.class)
            {
                return true;
            }
            else if(stanza.getTo() != null)
            {
                if(stanza.getTo().startsWith(GCM_SENDER_ID))
                {
                    return true;
                }
            }

            return false;
        }
    }

    private class CCSStanzaListener implements StanzaListener
    {
        private final Logger LOGGER = Logger.getLogger(CCSStanzaListener.class.getName());

        @Override
        public void processPacket(Stanza packet)
        {
            try
            {
                LOGGER.log(Level.INFO, "Recebido: {0}", packet.toXML());

                Message message = (Message)packet;
                GcmPacketExtension gcmPacketExtension = (GcmPacketExtension)message.getExtension(GCM_NAMESPACE);
                String json = gcmPacketExtension.getJson();

                Map<String, Object> jsonObject = (Map<String, Object>)JSONValue.parseWithException(json);

                /* Presente para "ACK/NACK", ou null caso contrário */
                switch(MessageType.get(jsonObject.get("message_type")))
                {
                    case NORMAL:
                    {
                        /* Mensagem upstream normal */
                        processMessageUpstream(jsonObject);

                        /* Enviar ACK para CCS */
                        String messageId = (String)jsonObject.get("message_id");
                        String from = (String)jsonObject.get("from");
                        String ACK = createJsonACK(from, messageId);

                        try
                        {
                            send(ACK);
                        }
                        catch(SmackException.NotConnectedException ex)
                        {
                            LOGGER.log(Level.SEVERE, null, ex);
                        }
                        break;
                    }
                    case ACK:
                    {
                        /* Processa mensagem ACK */
                        processReceiptACK(jsonObject);
                        break;
                    }
                    case NACK:
                    {
                        /* Processa mensagem NACK */
                        processReceiptNACK(jsonObject);
                        break;
                    }
                    case CONTROL:
                    {
                        /* Processa mensagem CONTROL */
                        processMessageControl(jsonObject);
                        break;
                    }
                    case UNRECOGNIZED:
                    default:
                    {
                        LOGGER.log(Level.WARNING, "Unrecognized message type (%s)", jsonObject.get("message_type").toString());
                        break;
                    }
                }
            }
            catch(ParseException ex)
            {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    private class CCSStanzaInterceptor implements StanzaListener
    {
        private final Logger LOGGER = Logger.getLogger(CCSStanzaInterceptor.class.getName());

        @Override
        public void processPacket(Stanza packet)
        {
            LOGGER.log(Level.INFO, "Sent: {0}", packet.toXML());
        }
    }

    private static final class LoggingConnectionListener implements ConnectionListener
    {
        private static final Logger LOGGER = Logger.getLogger(LoggingConnectionListener.class.getName());

        @Override
        public void connected(XMPPConnection xmppConnection)
        {
            LOGGER.info("Conectado.");
        }

        @Override
        public void reconnectionSuccessful()
        {
            LOGGER.info("Reconectando..");
        }

        @Override
        public void reconnectionFailed(Exception e)
        {
            LOGGER.log(Level.INFO, "Reconexão falhou.. ", e);
        }

        @Override
        public void reconnectingIn(int seconds)
        {
            LOGGER.log(Level.INFO, "Reconnecting in %d secs", seconds);
        }

        @Override
        public void connectionClosedOnError(Exception e)
        {
            LOGGER.log(Level.INFO, "Conexão fechada, houver erro. ", e);
        }

        @Override
        public void connectionClosed()
        {
            LOGGER.info("Conexão fechada.");
        }

        @Override
        public void authenticated(XMPPConnection connection, boolean debuggable)
        {
            LOGGER.info("Autenticado.");
        }
    }
}
