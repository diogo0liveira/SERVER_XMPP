package com.gcm.server.xmpp.connection;

import com.gcm.server.xmpp.CCSServer;
import com.gcm.server.xmpp.MessageReceivedListener;
import com.gcm.server.xmpp.XMPPMessage;
import com.gcm.server.xmpp.stanza.CCSStanzaFilter;
import com.gcm.server.xmpp.stanza.CCSStanzaInterceptor;
import com.gcm.server.xmpp.util.JsonKey;
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

import static com.gcm.server.xmpp.XMPPMessage.Type.CONTROL;
import static com.gcm.server.xmpp.XMPPMessage.Type.NACK;
import static com.gcm.server.xmpp.XMPPMessage.Type.RECEIPT;
import static com.gcm.server.xmpp.XMPPMessage.Type.UNRECOGNIZED;
import static com.gcm.server.xmpp.util.Constants.GCM_NAMESPACE;
import static com.gcm.server.xmpp.util.Constants.GCM_PORT;
import static com.gcm.server.xmpp.util.Constants.GCM_SERVER;

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
                .setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible)
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
            Message message = (Message)stanza;
            GcmPacketExtension gcmPacketExtension = (GcmPacketExtension)message.getExtension(GCM_NAMESPACE);

            String json = gcmPacketExtension.getJson();
            JSONObject jSONObject = (JSONObject)JSONValue.parseWithException(json);

            switch(XMPPMessage.Type.get(jSONObject.get(JsonKey.MESSAGE_TYPE)))
            {
                case NORMAL:
                {
                    /* Enviar ACK para CCS */
                    sendACK(jSONObject);

                    if(messageReceivedListener != null)
                    {
                        messageReceivedListener.onMessageReceivedJson(jSONObject);
                    }
                    else
                    {
                        processMessage(jSONObject);
                    }

                    break;
                }
                case ACK:
                {
                    /* Processa mensagem ACK */
                    processReceiptACK(jSONObject);
                    break;
                }
                case NACK:
                {
                    /* Processa mensagem NACK */
                    processReceiptNACK(jSONObject);
                    break;
                }
                case CONTROL:
                {
                    /* Processa mensagem CONTROL */
                    processMessageControl(jSONObject);
                    break;
                }
                case RECEIPT:
                {
                    /* Enviar ACK para CCS */
                    sendACK(jSONObject);
                    break;
                }
                case UNRECOGNIZED:
                default:
                {
                    LOGGER.log(Level.WARNING, "[PROCESSPACKET]: Tipo de mensagem não reconhecido,  (%s)", jSONObject.get(JsonKey.MESSAGE_TYPE).toString());
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
     * Criar uma mensagem de ACK JSON para uma mensagem upstream recebida de um aplicativo.
     *
     * @param jSONObject Json da mensagem recebida.
     */
    private void sendACK(JSONObject jSONObject)
    {
        try
        {
            connection.sendStanza(new GcmPacketExtension(XMPPMessage.ack(jSONObject.get(JsonKey.FROM), jSONObject.get(JsonKey.MESSAGE_ID)).toJSONString()).toPacket());
        }
        catch(SmackException.NotConnectedException ex)
        {
            LOGGER.log(Level.SEVERE, "[SENDACK]: (com.server.xmpp.connection.Connection).", ex);
        }
    }

    /**
     * Envia uma mensagem jusante.
     *
     * @param jsonRequest Mensagem a ser enviada no formato json.
     *
     * @throws NotConnectedException
     */
    private void send(XMPPMessage message) throws SmackException.NotConnectedException
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
    public boolean sendMessage(XMPPMessage message) throws SmackException.NotConnectedException
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
    private void processMessage(JSONObject jsonObject)
    {
        XMPPMessage message = XMPPMessage.with(jsonObject.get(JsonKey.FROM), jsonObject.get(JsonKey.MESSAGE_ID))
                .setData(jsonObject.get(JsonKey.DATA))
                .setAction(JsonKey.ECHO);

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
        if(("CONNECTION_DRAINING").equals(jsonObject.get(JsonKey.CONTROL_TYPE).toString()))
        {
            connectionDraining = true;
            LOGGER.log(Level.INFO, "[PROCESSMESSAGECONTROL]: {0}", "Conexão em Draining.");

            new CCSServer().createNewConnection();
            connectionDraining = CCSServer.getInstance().getConnection().isConnected();
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
            LOGGER.log(Level.INFO, "[PROCESSPACKET]: XML - {0}", stanza.toXML());
            processMessagePacket(stanza);
        }
    }
}
