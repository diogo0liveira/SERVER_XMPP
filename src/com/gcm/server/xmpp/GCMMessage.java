package com.gcm.server.xmpp;

import com.gcm.server.xmpp.util.JsonKey;
import org.json.simple.JSONObject;

/**
 * @author Diogo Oliveira
 * @date 11/11/2015 10:58:16
 */
public class GCMMessage extends JSONObject
{
    private final Object message_id;
    private final Object to;

    private Object collapse_key;
    private Object data;
    private Object from;

    private boolean delivery_receipt_requested;
    private boolean delay_while_idle;
    private String message_type;
    private int time_to_live;

    private GCMMessage(String to, String message_id)
    {
        this.to = to;
        this.message_id = message_id;

        this.put(JsonKey.TO, to);
        this.put(JsonKey.MESSAGE_ID, message_id);
    }

    public static GCMMessage with(String to, String message_id)
    {
        return new GCMMessage(to, message_id);
    }

    public static GCMMessage with(Object to, Object message_id)
    {
        return new GCMMessage(to.toString(), message_id.toString());
    }

    public GCMMessage setFrom(Object from)
    {
        this.put(JsonKey.FROM, from);
        this.from = from;
        return this;
    }

    public GCMMessage setData(Object data)
    {
        this.put(JsonKey.DATA, data);
        this.data = data;
        return this;
    }

    public GCMMessage setMessageType(String message_type)
    {
        this.put(JsonKey.MESSAGE_TYPE, message_type);
        this.message_type = message_type;
        return this;
    }

    public GCMMessage setCollapseKey(Object collapse_key)
    {
        this.put(JsonKey.COLLAPSE_KEY, collapse_key);
        this.collapse_key = collapse_key;
        return this;
    }

    public GCMMessage setDelayWhileIdle(boolean delay_while_idle)
    {
        this.put(JsonKey.DELAY_WHILE_IDLE, delay_while_idle);
        this.delay_while_idle = delay_while_idle;
        return this;
    }

    public GCMMessage setDeliveryReceiptRequested(boolean delivery_receipt_requested)
    {
        this.put(JsonKey.DELIVERY_RECEIPT_REQUESTED, delivery_receipt_requested);
        this.delivery_receipt_requested = delivery_receipt_requested;
        return this;
    }

    public GCMMessage setTimeToLive(int time_to_live)
    {
        this.put(JsonKey.TIME_TO_LIVE, time_to_live);
        this.time_to_live = time_to_live;
        return this;
    }

    public Object getMessage_id()
    {
        return message_id;
    }

    public Object getTo()
    {
        return to;
    }

    public String getMessage_type()
    {
        return message_type;
    }

    public Object getCollapse_key()
    {
        return collapse_key;
    }

    public Object getData()
    {
        return data;
    }

    public Object getFrom()
    {
        return from;
    }

    public boolean isDelivery_receipt_requested()
    {
        return delivery_receipt_requested;
    }

    public boolean isDelay_while_idle()
    {
        return delay_while_idle;
    }

    public int getTime_to_live()
    {
        return time_to_live;
    }
}
