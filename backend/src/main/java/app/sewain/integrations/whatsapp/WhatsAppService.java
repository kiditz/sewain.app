package app.sewain.integrations.whatsapp;

import app.sewain.integrations.IntegrationAccess;
import app.sewain.integrations.OutboundWhatsAppProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
class WhatsAppService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final WhatsAppGateway gateway;
    private final SimpMessagingTemplate socket;
    record Session(UUID id,String tenant,boolean enabled) {}
    record Status(boolean configured,boolean installed,boolean enabled,UUID sessionId,String state,String qr,String phone) {}
    record Message(UUID id,String direction,String status,String peer,String body,Instant occurredAt) {}
    record Messages(List<Message> items,int page, long total) {}

    @EventListener(ApplicationReadyEvent.class)
    public void recoverPendingMessages() {
        var tenants=jdbc.queryForList("SELECT tenant FROM public.whatsapp_sessions",String.class);
        for (var tenant:tenants) jdbc.update("UPDATE "+schema(tenant)+".integration_whatsapp_messages SET status='UNKNOWN' WHERE direction='OUTBOUND' AND status='SENDING'");
    }
    Status status() {
        var session=session(IntegrationAccess.tenant());
        if (session==null) return new Status(gateway.configured(),false,false,null,"NOT_CONNECTED",null,null);
        if (!session.enabled()) return new Status(gateway.configured(),true,false,session.id(),"DISCONNECTED",null,null);
        try {
            var value=gateway.snapshot(session.id());
            return new Status(true,true,true,session.id(),value.path("state").asString("STARTING"),
                    textOrNull(value,"qr"),textOrNull(value,"phone"));
        } catch (ResponseStatusException error) { return new Status(gateway.configured(),true,true,session.id(),"UNAVAILABLE",null,null); }
    }
    Status connect() {
        if (!gateway.configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"WhatsApp gateway belum dikonfigurasi");
        var tenant=IntegrationAccess.tenant();
        var current=transactions.execute(tx -> {
            jdbc.update("INSERT INTO public.whatsapp_sessions(session_id,tenant) VALUES (?,?) ON CONFLICT(tenant) DO NOTHING",UUID.randomUUID(),tenant);
            var session=session(tenant);
            jdbc.update("UPDATE public.whatsapp_sessions SET enabled=true WHERE session_id=?",session.id());
            install(tenant,true);
            return session;
        });
        gateway.connect(Objects.requireNonNull(current).id());
        return status();
    }
    void disconnect() {
        var tenant=IntegrationAccess.tenant(); var current=session(tenant);
        if (current==null) return;
        // Disable delivery before terminating. Keep the ID so a failed termination can be retried.
        transactions.executeWithoutResult(tx -> {
            jdbc.update("UPDATE public.whatsapp_sessions SET enabled=false WHERE session_id=?",current.id());
            install(tenant,false);
        });
        gateway.disconnect(current.id());
        notifyTenant(current);
    }
    private void install(String tenant,boolean enabled) {
        jdbc.update("INSERT INTO "+schema(tenant)+".integration_installations(id,code,enabled,configuration) VALUES (?,'whatsapp',?,'{}'::jsonb) ON CONFLICT(code) DO UPDATE SET enabled=excluded.enabled,updated_at=now()",UUID.randomUUID(),enabled);
    }
    private Session session(String tenant) {
        return jdbc.query("SELECT * FROM public.whatsapp_sessions WHERE tenant=?",(rs,n)->new Session(rs.getObject("session_id",UUID.class),rs.getString("tenant"),rs.getBoolean("enabled")),tenant).stream().findFirst().orElse(null);
    }
    Message send(OutboundWhatsAppProvider.Text text) {
        var tenant=IntegrationAccess.tenant(); var session=session(tenant);
        if (session==null || !session.enabled()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Hubungkan WhatsApp terlebih dahulu");
        var table=schema(tenant)+".integration_whatsapp_messages";
        var peer=text.to().replaceFirst("^\\+","")+"@c.us";
        var id=UUID.randomUUID();
        int reserved=jdbc.update("INSERT INTO "+table+"(id,session_id,request_id,direction,status,peer,body) VALUES (?,?,?,'OUTBOUND','SENDING',?,?) ON CONFLICT(request_id) DO NOTHING",id,session.id(),text.requestId(),peer,text.body());
        if (reserved==0) {
            var previous=jdbc.query("SELECT * FROM "+table+" WHERE request_id=?",(rs,n)->map(rs),text.requestId()).getFirst();
            if (!previous.peer().equals(peer) || !previous.body().equals(text.body())) throw new ResponseStatusException(HttpStatus.CONFLICT,"requestId sudah dipakai untuk pesan berbeda");
            return previous;
        }
        try {
            var result=gateway.send(session.id(),peer,text.body());
            var external=result.path("message").path("id").path("_serialized").asString("");
            jdbc.update("UPDATE "+table+" SET status='SENT',external_id=? WHERE id=?",external.isBlank()?null:cut(external,255),id);
        } catch (Exception error) {
            // A timeout may happen after WhatsApp accepted a message. Do not automatically resend.
            jdbc.update("UPDATE "+table+" SET status='UNKNOWN' WHERE id=?",id);
        }
        notifyTenant(session);
        return jdbc.query("SELECT * FROM "+table+" WHERE id=?",(rs,n)->map(rs),id).getFirst();
    }
    Messages messages(int page) {
        if (page<0 || page>10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Halaman tidak valid");
        var table=schema(IntegrationAccess.tenant())+".integration_whatsapp_messages";
        var total=jdbc.queryForObject("SELECT count(*) FROM "+table,Long.class);
        return new Messages(jdbc.query("SELECT * FROM "+table+" ORDER BY occurred_at DESC,id DESC LIMIT 30 OFFSET ?",(rs,n)->map(rs),page*30),page,total==null?0:total);
    }
    void callback(UUID id,String type,JsonNode data) {
        var matches=jdbc.query("SELECT * FROM public.whatsapp_sessions WHERE session_id=? AND enabled=true",(rs,n)->new Session(id,rs.getString("tenant"),true),id);
        if (matches.isEmpty()) return;
        var session=matches.getFirst();
        if ("message".equals(type)) {
            var message=data.path("message");
            var external=message.path("id").path("_serialized").asString("");
            var peer=message.path("from").asString("");
            var body=message.path("body").asString("");
            if (external.isBlank() || external.length()>255 || !peer.matches("[0-9]{1,30}@(c\\.us|lid)") ||
                    message.path("fromMe").asBoolean(false) || !"chat".equals(message.path("type").asString("")) || body.isBlank()) return;
            jdbc.update("INSERT INTO "+schema(session.tenant())+".integration_whatsapp_messages(id,session_id,external_id,direction,status,peer,body) VALUES (?,?,?,'INBOUND','RECEIVED',?,?) ON CONFLICT(session_id,external_id) DO NOTHING",UUID.randomUUID(),id,external,peer,cut(body,4096));
        }
        notifyTenant(session);
    }
    private void notifyTenant(Session session) {
        // A notification is only a hint; clients reload via authenticated tenant-scoped HTTP.
        var users=jdbc.queryForList("SELECT mu.user_id FROM public.merchant_users mu JOIN public.merchants m ON m.id=mu.merchant_id WHERE m.schema=?",UUID.class,session.tenant());
        for (var user:users) {
            try { socket.convertAndSendToUser(user.toString(),"/queue/whatsapp",Map.of("sessionId",session.id())); }
            catch (Exception ignored) { /* Snapshot on reconnect recovers missed notifications. */ }
        }
    }
    private static Message map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Message(rs.getObject("id",UUID.class),rs.getString("direction"),rs.getString("status"),rs.getString("peer"),rs.getString("body"),rs.getTimestamp("occurred_at").toInstant());
    }
    private static String schema(String tenant) {
        if (tenant==null || !tenant.matches("[a-zA-Z_][a-zA-Z0-9_]*") || tenant.equals("public")) throw new IllegalArgumentException("Invalid tenant");
        return "\""+tenant+"\"";
    }
    private static String cut(String value,int max) { return value.substring(0,Math.min(max,value.length())); }
    private static String textOrNull(JsonNode node,String field) { return node.path(field).isString()?node.path(field).asString():null; }
}
