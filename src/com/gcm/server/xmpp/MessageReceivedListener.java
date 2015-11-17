package com.server.xmpp;

import org.json.simple.JSONObject;

/**
 * Listener usado para receber uma mensagem que foi enviada ao servido. Em seguida uma mensagem ACK será enviada ao servido CCS.
 *
 * @author Diogo Oliveira
 * @date 11/11/2015 09:20:14
 */
public interface MessageReceivedListener
{
    void onMessageReceivedJson(JSONObject jSONObject);
}
