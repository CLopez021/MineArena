package com.clopez021.mine_arena.client.speech_recognition;

import com.clopez021.mine_arena.client.VoiceSidecarUi;
import com.clopez021.mine_arena.packets.PacketHandler;
import com.clopez021.mine_arena.packets.RecognizedSpeechPacket;
import com.clopez021.mine_arena.speech_recognition.SpeechCommand;
import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Client-side sidecar that serves the minimal proxy page and bridges WS messages.
 */
public final class VoiceSidecar {
    private static final Map<UUID, VoiceSidecar> instances = new ConcurrentHashMap<>();

    private final UUID playerId;
    private final Gson gson = new Gson();
    private HttpServer http;
    private WsBridge ws;
    private int httpPort;
    private int wsPort;
    private boolean isRunning = false;
    private Consumer<SpeechCommand> commandHandler;

    private VoiceSidecar(UUID playerId) {
        this.playerId = playerId;
    }

    public static VoiceSidecar getInstance(UUID playerId) {
        return instances.computeIfAbsent(playerId, VoiceSidecar::new);
    }

    public static void removeInstance(UUID playerId) {
        VoiceSidecar instance = instances.remove(playerId);
        if (instance != null && instance.isRunning) {
            instance.stop();
        }
    }

    public static Map<UUID, VoiceSidecar> getAllInstances() {
        return new HashMap<>(instances);
    }

    public void start(List<String> initialSpells, String lang, Consumer<SpeechCommand> commandHandler) throws Exception {
        if (isRunning) return;

        this.commandHandler = commandHandler;

        wsPort = pickFreePort();
        httpPort = pickFreePort();

        ws = new WsBridge(wsPort, playerId, this::handleSpeechCommand);
        ws.start();

        http = HttpServer.create(new InetSocketAddress("127.0.0.1", httpPort), 0);
        http.createContext("/", this::handleHttpRequest);
        http.start();

        // Open browser prompt on the client
        String url = "http://127.0.0.1:" + httpPort + "/index.html?wsPort=" + wsPort + "&playerId=" + playerId;
        VoiceSidecarUi.promptAndOpen(url);

        // Push initial config
        sendConfig(lang, initialSpells);

        isRunning = true;
    }

    public void stop() {
        if (!isRunning) return;

        try { if (ws != null) ws.stop(1000); } catch (Exception ignored) {}
        try { if (http != null) http.stop(0); } catch (Exception ignored) {}

        isRunning = false;
    }

    public void sendConfig(String lang, List<String> spells) {
        if (!isRunning || ws == null) return;

        JsonObject cfg = new JsonObject();
        cfg.addProperty("type", "config");
        cfg.addProperty("playerId", playerId.toString());
        if (lang != null) cfg.addProperty("lang", lang);
        if (spells != null) {
            JsonArray arr = new JsonArray();
            spells.forEach(arr::add);
            cfg.add("spells", arr);
        }
        ws.broadcast(gson.toJson(cfg));
    }

    public void sendStart() {
        if (!isRunning || ws == null) return;
        ws.broadcast("{\"type\":\"start\",\"playerId\":\"" + playerId + "\"}");
    }

    public void sendStop() {
        if (!isRunning || ws == null) return;
        ws.broadcast("{\"type\":\"stop\",\"playerId\":\"" + playerId + "\"}");
    }

    public UUID getPlayerId() { return playerId; }
    public boolean isRunning() { return isRunning; }

    private void handleHttpRequest(HttpExchange exchange) throws IOException {
        String path = Optional.ofNullable(exchange.getRequestURI().getPath()).orElse("/");
        if ("/".equals(path)) path = "/index.html";

        if ("/index.html".equals(path)) {
            byte[] body = readResourceBytes("/speech_recognition/index.html");
            if (body == null) { send404(exchange); return; }

            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) { os.write(body); }
        } else {
            send404(exchange);
        }
    }

    private void handleSpeechCommand(SpeechCommand command) {
        if (commandHandler != null) {
            commandHandler.accept(command);
        } else {
            // Default: forward to server via packet
            PacketHandler.INSTANCE.send(new RecognizedSpeechPacket(
                    command.getSpell(),
                    command.getHeard(),
                    command.getMatchKind(),
                    command.getConfidence(),
                    command.getTimestamp()
            ), net.minecraftforge.network.PacketDistributor.SERVER.noArg());
        }
    }

    private static int pickFreePort() throws IOException {
        try (var s = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) {
            return s.getLocalPort();
        }
    }

    private static void send404(HttpExchange ex) throws IOException {
        byte[] body = "Not found".getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(404, body.length);
        try (var os = ex.getResponseBody()) { os.write(body); }
    }

    private static byte[] readResourceBytes(String path) throws IOException {
        try (InputStream in = VoiceSidecar.class.getResourceAsStream(path)) {
            if (in == null) return null;
            return in.readAllBytes();
        }
    }

    public static final class WsBridge extends WebSocketServer {
        private final AtomicReference<WebSocket> client = new AtomicReference<>();
        private final Gson gson = new Gson();
        private final UUID playerId;
        private final Consumer<SpeechCommand> commandHandler;

        public WsBridge(int port, UUID playerId, Consumer<SpeechCommand> commandHandler) throws UnknownHostException {
            super(new InetSocketAddress("127.0.0.1", port));
            this.playerId = playerId;
            this.commandHandler = commandHandler;
        }

        @Override public void onOpen(WebSocket conn, ClientHandshake hs) {
            WebSocket existing = client.getAndSet(conn);
            if (existing != null && existing.isOpen()) existing.close();
        }
        @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) { client.compareAndSet(conn, null); }
        @Override public void onError(WebSocket conn, Exception ex) { ex.printStackTrace(); }
        @Override public void onStart() { }

        @Override public void onMessage(WebSocket conn, String message) {
            try {
                JsonObject msg = JsonParser.parseString(message).getAsJsonObject();
                String type = msg.get("type").getAsString();
                if ("command".equals(type)) {
                    String spell = msg.get("spell").getAsString();
                    String heard = msg.get("heard").getAsString();
                    String matchKind = msg.has("matchKind") ? msg.get("matchKind").getAsString() : "unknown";
                    double distance = msg.has("distance") ? msg.get("distance").getAsDouble() : 0.0;
                    double confidence = msg.has("confidence") && !msg.get("confidence").isJsonNull()
                            ? msg.get("confidence").getAsDouble() : -1;
                    long timestamp = msg.has("ts") ? msg.get("ts").getAsLong() : System.currentTimeMillis();

                    SpeechCommand command = new SpeechCommand(playerId, spell, heard, matchKind, distance, confidence, timestamp);
                    commandHandler.accept(command);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void broadcast(String json) {
            WebSocket conn = client.get();
            if (conn != null && conn.isOpen()) conn.send(json);
        }
    }
} 