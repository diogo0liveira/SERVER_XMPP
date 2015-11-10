package com.diogo.oliveira.xmpp.util;

/**
 * @author Diogo Oliveira
 * @date 04/11/2015 12:12:34
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
