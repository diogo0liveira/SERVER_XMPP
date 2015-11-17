package com.gcm.server.xmpp;

import com.gcm.server.xmpp.util.JsonKey;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author Diogo Oliveira
 * @date 11/11/2015 10:58:16
 */
public class XMPPMessage extends JSONObject
{
    private final Object message_id;
    private final Object to;

    private Object collapse_key;
    private Object data;
    private Object from;

    private boolean delivery_receipt_requested;
    private boolean delay_while_idle;
    private boolean dry_run;

    private String message_type;
    private String action;

    private int time_to_live;
    private Priority priority;

    /**
     *
     * @param to         Especifica o destinatário de uma mensagem. O valor deve ser um de token registo.
     * @param message_id Identificador unico da mensagem.
     */
    private XMPPMessage(Object to, Object message_id)
    {
        this.to = to;
        this.message_id = message_id;

        this.put(JsonKey.TO, to);
        this.put(JsonKey.MESSAGE_ID, message_id);
    }

    public static ACK ack(Object from, Object message_id)
    {
        return new ACK(from, message_id, JsonKey.ACK);
    }

    public static XMPPMessage with(Object to, Object message_id)
    {
        return new XMPPMessage(to, message_id);
    }

    public XMPPMessage setFrom(Object from)
    {
        this.put(JsonKey.FROM, from);
        this.from = from;
        return this;
    }

    /**
     * Este parâmetro, quando definido como verdadeiro, permite aos desenvolvedores testar um pedido sem enviar uma mensagem.
     * <p>
     * O valor padrão é false.
     *
     * @param dry_run (dry_run).
     *
     * @return Retorna a autal instância de XMPPMessage.
     */
    public XMPPMessage setDryRun(boolean dry_run)
    {
        this.put(JsonKey.DRY_RUN, dry_run);
        this.dry_run = dry_run;
        return this;
    }

    public XMPPMessage setData(Object data)
    {
        if(data instanceof String)
        {
            if(!isJsonString(String.valueOf(data)))
            {
                Map<String, Object> map = (Map<String, Object>)this.get(JsonKey.DATA);

                if(map == null)
                {
                    map = new HashMap<>();
                }

                map.put(JsonKey.DATA, data);
                data = map;
            }
        }

        this.put(JsonKey.DATA, data);
        this.data = data;
        return this;
    }

    /**
     * Define a prioridade da mensagem. Os valores válidos são "normal" e "high".
     * <p>
     * Por padrão, as mensagens são enviadas com prioridade normal. Prioridade normal otimiza o consumo de bateria do aplicativo cliente, e
     * deve ser usada quando não é necessária a entrega imediata. Para mensagens com prioridade normal, o aplicativo poderá receber a
     * mensagem com atraso não especificado.
     * <p>
     * Quando uma mensagem é enviada com prioridade alta, ele é enviado imediatamente, eo aplicativo pode acordar um dispositivo de dormir e
     * abrir uma conexão de rede para o servidor.
     * <p>
     * O valor padrão é normal.
     * <p>
     * Para obter mais informações, consulte:
     *
     * @see  <a href="https://developers.google.com/cloud-messaging/concept-options?hl=pt-br#setting-the-priority-of-a-message">Definir a
     * prioridade de uma mensagem</a>
     *
     * @param priority (priority).
     *
     * @return Retorna a autal instância de XMPPMessage.
     */
    public XMPPMessage setPriority(Priority priority)
    {
        this.put(JsonKey.PRIORITY, priority.value);
        this.priority = priority;
        return this;
    }

    public XMPPMessage setMessageType(String message_type)
    {
        this.put(JsonKey.MESSAGE_TYPE, message_type);
        this.message_type = message_type;
        return this;
    }

    /**
     * Este parâmetro identifica um grupo de mensagens (por exemplo, com collapse_key: "Actualização Disponível") que pode ser recolhido, de
     * modo que apenas a última mensagem é enviada quando a entrega pode ser retomada. Este destina-se a evitar o envio de muitas das mesmas
     * mensagens quando o dispositivo voltar a ficar online ou se torna ativo.
     * <p>
     * Note que não há nenhuma garantia da ordem em que as mensagens são enviadas.
     * <p>
     * <strong>Nota:</strong> É permitido no máximo de 4 chaves diferentes a qualquer momento. Isso significa que um servidor de conexão GCM
     * pode armazenar simultaneamente 4 mensagens diferentes por cliente. Se exceder este número, não há garantia de qual das 4 chaves
     * colapso o servidor irá manter.
     *
     * @param collapse_key texto "para collapse_key".
     *
     * @return Retorna a autal instância de XMPPMessage.
     */
    public XMPPMessage setCollapseKey(Object collapse_key)
    {
        this.put(JsonKey.COLLAPSE_KEY, collapse_key);
        this.collapse_key = collapse_key;
        return this;
    }

    /**
     * Quando esse parâmetro é definido como true, indica que a mensagem não deve ser enviada até que o dispositivo se torna ativo.
     * <p>
     * O valor padrão é false.
     *
     * @param delay_while_idle delay_while_idle
     *
     * @return Retorna a autal instância de XMPPMessage.
     */
    public XMPPMessage setDelayWhileIdle(boolean delay_while_idle)
    {
        this.put(JsonKey.DELAY_WHILE_IDLE, delay_while_idle);
        this.delay_while_idle = delay_while_idle;
        return this;
    }

    /**
     * Este parâmetro permite a confirmação para o servidor de aplicativo de envio de mensagens.
     * <p>
     * Quando esse parâmetro é definido como true, CCS envia um recibo de entrega quando o dispositivo confirma que recebeu a mensagem.
     * <p>
     * O valor padrão é false.
     *
     * @param delivery_receipt_requested delivery_receipt_requested
     *
     * @return Retorna a autal instância de XMPPMessage.
     */
    public XMPPMessage setDeliveryReceiptRequested(boolean delivery_receipt_requested)
    {
        this.put(JsonKey.DELIVERY_RECEIPT_REQUESTED, delivery_receipt_requested);
        this.delivery_receipt_requested = delivery_receipt_requested;
        return this;
    }

    /**
     * Esse parâmetro especifica quanto tempo (em segundos), a mensagem deve ser mantido em armazenamento GCM se o dispositivo está offline.
     * O tempo máximo de viver suportado é de 4 semanas, eo valor padrão é de 4 semanas.
     *
     * @param time_to_live em segundos.
     *
     * @return Retorna a autal instância de XMPPMessage.
     */
    public XMPPMessage setTimeToLive(int time_to_live)
    {
        this.put(JsonKey.TIME_TO_LIVE, time_to_live);
        this.time_to_live = time_to_live;
        return this;
    }

    public XMPPMessage setAction(String action)
    {
        if((action != null) && (!action.trim().isEmpty()))
        {
            Map<String, Object> map = (Map<String, Object>)data;

            if(map == null)
            {
                map = new HashMap<>();
            }

            map.put(JsonKey.ACTION, action);
            this.put(JsonKey.DATA, map);
            this.action = action;
        }

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

    public boolean isDry_run()
    {
        return dry_run;
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
        if(data != null)
        {
            Map<String, Object> map = (Map<String, Object>)this.get(JsonKey.DATA);
            return (((map != null) && map.containsKey(JsonKey.DATA)) ? map.get(JsonKey.DATA) : null);
        }
        else
        {
            return null;
        }
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

    public Priority getPriority()
    {
        return priority;
    }

    public String getAction()
    {
        return action;
    }

    public boolean isJsonString(String str)
    {
        try
        {
            new JSONParser().parse(String.valueOf(str));
            return true;
        }
        catch(ParseException ex)
        {
            System.err.println(ex.getMessage());
            return false;
        }
    }

    public enum Type
    {
        ACK("ack"), NACK("nack"), NORMAL(null), CONTROL("control"), RECEIPT("receipt"), UNRECOGNIZED("unrecognized");

        public String value;

        Type(String value)
        {
            this.value = value;
        }

        public static Type get(Object value)
        {
            if(value == NORMAL.value)
            {
                return NORMAL;
            }
            else
            {
                for(Type intervalo : Type.values())
                {
                    if(intervalo.value != null)
                    {
                        if(intervalo.value.equals(value))
                        {
                            return intervalo;
                        }
                    }
                }

                return UNRECOGNIZED;
            }
        }
    }

    public enum Priority
    {
        NORMAL("normal"), HIGH("high");

        public String value;

        Priority(String value)
        {
            this.value = value;
        }

        public static Priority get(Object value)
        {
            for(Priority intervalo : Priority.values())
            {
                if(intervalo.value.equals(value))
                {
                    return intervalo;
                }
            }

            return null;
        }
    }

    public static class ACK extends JSONObject
    {
        private final String message_type;
        private final Object message_id;
        private final Object from;

        ACK(Object from, Object message_id, String message_type)
        {
            this.from = from;
            this.message_id = message_id;
            this.message_type = message_type;

            this.put(JsonKey.FROM, from);
            this.put(JsonKey.MESSAGE_ID, message_id);
            this.put(JsonKey.MESSAGE_TYPE, message_type);
        }

        public String getMessage_type()
        {
            return message_type;
        }

        public Object getMessage_id()
        {
            return message_id;
        }

        public Object getFrom()
        {
            return from;
        }
    }
}
