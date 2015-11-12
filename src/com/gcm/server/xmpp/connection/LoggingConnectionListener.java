package com.gcm.server.xmpp.connection;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;

/**
 * @author Diogo Oliveira
 * @date 06/11/2015 11:56:33
 */
public class LoggingConnectionListener implements ConnectionListener
{
    private static final Logger LOGGER = Logger.getLogger(LoggingConnectionListener.class.getName());

    @Override
    public void connected(XMPPConnection xmppConnection)
    {
        LOGGER.info("[CONNECTED]: Conectado.");
    }

    @Override
    public void reconnectionSuccessful()
    {
        LOGGER.info("[RECONNECTIONSUCCESSFUL]: Reconectando.");
    }

    @Override
    public void reconnectionFailed(Exception e)
    {
        LOGGER.log(Level.INFO, "[RECONNECTIONFAILED]: Reconexão falhou. ", e);
    }

    @Override
    public void reconnectingIn(int seconds)
    {
        LOGGER.log(Level.INFO, "[RECONNECTINGIN]: Reconectando em %d segundos", seconds);
    }

    @Override
    public void connectionClosedOnError(Exception e)
    {
        LOGGER.log(Level.INFO, "[CONNECTIONCLOSEDONERROR]: Conexão fechada, houver erro. ", e);
    }

    @Override
    public void connectionClosed()
    {
        LOGGER.info("[CONNECTIONCLOSED]: Conexão fechada.");
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean debuggable)
    {
        LOGGER.info("[AUTHENTICATED]: Autenticado.");
    }
}
