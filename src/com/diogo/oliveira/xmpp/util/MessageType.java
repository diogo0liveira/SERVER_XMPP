package com.diogo.oliveira.xmpp.util;

/**
 *
 * @author diogo
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
            if(intervalo.value.equals(value))
            {
                return intervalo;
            }
        }

        return UNRECOGNIZED;
    }
}
