package com.gcm.server.xmpp.connection;

import com.gcm.server.xmpp.GCMMessage;
import com.gcm.server.xmpp.MessageReceivedListener;
import com.gcm.server.xmpp.stanza.CCSStanzaFilter;
import com.gcm.server.xmpp.stanza.CCSStanzaInterceptor;
import com.gcm.server.xmpp.util.JsonKey;
import com.gcm.server.xmpp.util.MessageType;
import java.io.IOException;
import javax.net.ssl.SSLSocketFactory;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Message;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import static com.gcm.server.xmpp.util.Constants.GCM_NAMESPACE;
import static com.gcm.server.xmpp.util.Constants.GCM_PORT;
import static com.gcm.server.xmpp.util.Constants.GCM_SERVER;
import static com.gcm.server.xmpp.util.MessageType.ACK;
import static com.gcm.server.xmpp.util.MessageType.CONTROL;
import static com.gcm.server.xmpp.util.MessageType.NACK;
import static com.gcm.server.xmpp.util.MessageType.NORMAL;
import static com.gcm.server.xmpp.util.MessageType.UNRECOGNIZED;

/**
 * @author Diogo Oliveira
 * @date 06/11/2015 11:54:52
 */
abstract class Connection
{
    private static final Logger LOGGER = Logger.getLogger(Connection.class.getName());
    private MessageReceivedListener messageReceivedListener;
    private volatile boolean connectionDraining = false;
    private final XMPPTCPConnection connection;

    public Connection(String senderId, String apiKey) throws SmackException, IOException, XMPPException
    {
        XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                .setServiceName(GCM_SERVER)
                .setHost(GCM_SERVER)
                .setPort(GCM_PORT)
                .setSendPresence(false)
                .setConnectTimeout(10000)
                .setDebuggerEnabled(true)
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
        connection.addAsyncStanzaListener(new CCSStanzaListener(), new CCSStanzaFilter(senderId));
        connection.addPacketInterceptor(new CCSStanzaInterceptor(), new CCSStanzaFilter(senderId));
        connection.login(senderId + "@gcm.googleapis.com", apiKey);
    }

    public XMPPTCPConnection getConnection()
    {
        return connection;
    }

    protected void processMessagePacket(Stanza stanza)
    {
        try
        {
            LOGGER.log(Level.INFO, "[PROCESSPACKET]: {0}", stanza.toXML());

            Message message = (Message)stanza;
            GcmPacketExtension gcmPacketExtension = (GcmPacketExtension)message.getExtension(GCM_NAMESPACE);

            String json = gcmPacketExtension.getJson();
            Map<String, Object> jsonObject = (Map<String, Object>)JSONValue.parseWithException(json);

            switch(MessageType.get(jsonObject.get(JsonKey.MESSAGE_TYPE)))
            {
                case NORMAL:
                {
                    JSONObject jSONObject = new JSONObject(jsonObject);

                    /* Enviar ACK para CCS */
                    sendMessageACK(jSONObject);

                    if(messageReceivedListener != null)
                    {
                        messageReceivedListener.onMessageReceivedJson(jSONObject);
                    }
                    else
                    {
                        receivedMessage(new JSONObject(jsonObject));
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
                    LOGGER.log(Level.WARNING, "[PROCESSPACKET]: Tipo de mensagem não reconhecido,  (%s)", jsonObject.get("message_type").toString());
                    break;
                }
            }
        }
        catch(ParseException ex)
        {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
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
     * Cria uma mensagem de ACK JSON para uma mensagem upstream recebida de um aplicativo.
     *
     * @param jSONObject Json da mensagem recebida.
     */
    private void sendMessageACK(JSONObject jSONObject)
    {
        try
        {
            send(GCMMessage.with(jSONObject.get(JsonKey.FROM), jSONObject.get(JsonKey.MESSAGE_ID)).setMessageType(JsonKey.ACK));
        }
        catch(SmackException.NotConnectedException ex)
        {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Envia uma mensagem jusante.
     *
     * @param jsonRequest Mensagem a ser enviada no formato json.
     *
     * @throws NotConnectedException
     */
    private void send(GCMMessage message) throws SmackException.NotConnectedException
    {
        connection.sendStanza(new GcmPacketExtension(message.toJSONString()).toPacket());
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
    public boolean sendMessage(GCMMessage message) throws SmackException.NotConnectedException
    {
        if(!connectionDraining)
        {
            send(message);
            return true;
        }

        LOGGER.info("Descartando mensagem uma vez que a conexão está draining.");
        return false;
    }

    /**
     * Recebi mensagem enviada ao servidor.
     * <p>
     * <p>
     * Envia uma mensagem de eco de volta ao dispositivo. Subclasses deve substituir esse método para processar corretamente as mensagens
     * upstream.
     *
     * @param jsonObject Mensagem a ser enviada no formato json.
     */
    private void receivedMessage(JSONObject jsonObject)
    {
        GCMMessage message = GCMMessage.with(jsonObject.get(JsonKey.FROM), jsonObject.get(JsonKey.MESSAGE_ID))
                .setData(jsonObject.get(JsonKey.DATA))
                .setDelayWhileIdle(false);

        try
        {
            /* Enviar uma resposta eco de volta */
            sendMessage(message);

        }
        catch(SmackException.NotConnectedException e)
        {
            LOGGER.log(Level.WARNING, "[PROCESSMESSAGEUPSTREAM]: Não conectado, mensagem de eco não é enviada.", e);
        }
    }

    protected void processMessageControl(Map<String, Object> jsonObject)
    {
        if("CONNECTION_DRAINING".equals(jsonObject.get(JsonKey.CONTROL_TYPE).toString()))
        {
            connectionDraining = true;
            LOGGER.log(Level.INFO, "[PROCESSMESSAGECONTROL]: {0}", "Conexão em Draining.");
        }
        else
        {
            LOGGER.log(Level.INFO, "[PROCESSMESSAGECONTROL]:  Tipo de controle não reconhecido: %s. Isso pode acontecer se novas funcionalidades são adicionadas ao protocolo CCS.", jsonObject.get(JsonKey.CONTROL_TYPE));
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
        LOGGER.log(Level.INFO, "[PROCESSRECEIPTACK] from: {0}, messageId: {1}", new Object[]
        {
            jsonObject.get(JsonKey.FROM), jsonObject.get(JsonKey.MESSAGE_ID)
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
        LOGGER.log(Level.INFO, "[PROCESSRECEIPTNACK] from: {0}, messageId: {1}", new Object[]
        {
            jsonObject.get(JsonKey.FROM), jsonObject.get(JsonKey.MESSAGE_ID)
        });
    }

    public void setMessageReceivedListener(MessageReceivedListener listener)
    {
        messageReceivedListener = listener;
    }

    protected final class CCSStanzaListener implements StanzaListener
    {
        private final Logger LOGGER = Logger.getLogger(CCSStanzaListener.class.getName());

        @Override
        public void processPacket(Stanza stanza) throws SmackException.NotConnectedException
        {
            LOGGER.log(Level.INFO, "[PROCESSPACKET]: {0}", stanza.toXML());
            processMessagePacket(stanza);
        }
    }
}
