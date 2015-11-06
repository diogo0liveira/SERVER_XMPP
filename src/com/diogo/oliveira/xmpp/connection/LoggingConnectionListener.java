package com.diogo.oliveira.xmpp.connection;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;

/**
 *
 * @author Diogo Oliveira
 * @date 06/11/2015 11:56:33
 */
public class LoggingConnectionListener implements ConnectionListener
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
