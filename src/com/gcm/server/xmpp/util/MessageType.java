package com.gcm.server.xmpp.util;

/**
 * @author Diogo Oliveira
 * @date 06/11/2015 11:07:16
 */
public enum MessageType
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
            if(intervalo.value == value)
            {
                return intervalo;
            }
        }

        return UNRECOGNIZED;
    }
}
