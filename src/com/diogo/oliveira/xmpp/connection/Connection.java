package com.diogo.oliveira.xmpp.connection;

import com.diogo.oliveira.xmpp.stanza.CCSStanzaFilter;
import com.diogo.oliveira.xmpp.stanza.CCSStanzaInterceptor;
import com.diogo.oliveira.xmpp.stanza.CCSStanzaListener;
import java.io.IOException;
import javax.net.ssl.SSLSocketFactory;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import static com.diogo.oliveira.xmpp.util.Constants.GCM_PORT;
import static com.diogo.oliveira.xmpp.util.Constants.GCM_SERVER;

/**
 * @author Diogo Oliveira
 * @date 06/11/2015 11:54:52
 */
public abstract class Connection extends CCSStanzaListener
{
    private final XMPPTCPConnection connection;

    public XMPPTCPConnection getConnection()
    {
        return connection;
    }

    public Connection(String senderId, String apiKey) throws SmackException, IOException, XMPPException
    {
        super();

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
        super.setConnection(connection);

        /* TESTAR */
        Roster roster = Roster.getInstanceFor(connection);
        roster.setRosterLoadedAtLogin(false);

        connection.connect();
        connection.addConnectionListener(new LoggingConnectionListener());
        connection.addAsyncStanzaListener(super.getListener(), new CCSStanzaFilter(senderId));
        connection.addPacketInterceptor(new CCSStanzaInterceptor(), new CCSStanzaFilter(senderId));
        connection.login(senderId + "@gcm.googleapis.com", apiKey);
    }
}
