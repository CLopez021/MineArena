package com.clopez021.mine_arena.speech_recognition;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.awt.*;
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
 * Singleton client for managing Web Speech API integration with Minecraft.
 * Provides voice recognition capabilities through a browser-based sidecar interface.
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
    
    /**
     * Gets or creates a VoiceSidecar instance for the specified player.
     * 
     * @param playerId The UUID of the player
     * @return The VoiceSidecar instance for this player
     */
    public static VoiceSidecar getInstance(UUID playerId) {
        return instances.computeIfAbsent(playerId, VoiceSidecar::new);
    }
    
    /**
     * Removes the VoiceSidecar instance for the specified player.
     * 
     * @param playerId The UUID of the player
     */
    public static void removeInstance(UUID playerId) {
        VoiceSidecar instance = instances.remove(playerId);
        if (instance != null && instance.isRunning) {
            instance.stop();
        }
    }
    
    /**
     * Gets all active VoiceSidecar instances.
     * 
     * @return Map of player UUIDs to VoiceSidecar instances
     */
    public static Map<UUID, VoiceSidecar> getAllInstances() {
        return new HashMap<>(instances);
    }
    
    /**
     * Starts the voice recognition sidecar for this player.
     * 
     * @param initialSpells List of spell phrases to recognize
     * @param lang Language code (e.g., "en-US")
     * @param commandHandler Callback to handle recognized speech commands
     * @throws Exception if startup fails
     */
    public void start(List<String> initialSpells, String lang, Consumer<SpeechCommand> commandHandler) throws Exception {
        if (isRunning) {
            return; // Already running
        }
        
        this.commandHandler = commandHandler;
        
        wsPort = pickFreePort();
        httpPort = pickFreePort();

        ws = new WsBridge(wsPort, playerId, this::handleSpeechCommand);
        ws.start();

        http = HttpServer.create(new InetSocketAddress("127.0.0.1", httpPort), 0);
        
        // Serve /index.html from classpath
        http.createContext("/", this::handleHttpRequest);
        http.start();

        // Print URL for manual navigation
        String url = "http://127.0.0.1:" + httpPort + "/index.html?wsPort=" + wsPort + "&playerId=" + playerId;
        System.out.println("Voice recognition URL: " + url);

        // Push initial config
        sendConfig(lang, initialSpells);
        
        isRunning = true;
        System.out.println("VoiceSidecar started for player " + playerId + " on ports HTTP:" + httpPort + " WS:" + wsPort);
    }
    
    /**
     * Stops the voice recognition sidecar.
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        try { 
            if (ws != null) ws.stop(1000); 
        } catch (Exception ignored) {}
        
        try { 
            if (http != null) http.stop(0); 
        } catch (Exception ignored) {}
        
        isRunning = false;
        System.out.println("VoiceSidecar stopped for player " + playerId);
    }
    
    /**
     * Updates the spell configuration.
     * 
     * @param lang Language code
     * @param spells List of spell phrases
     */
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
    
    /**
     * Tells the browser to start listening (still requires user click for mic permission).
     */
    public void sendStart() { 
        if (!isRunning || ws == null) return;
        ws.broadcast("{\"type\":\"start\",\"playerId\":\"" + playerId + "\"}"); 
    }
    
    /**
     * Tells the browser to stop listening.
     */
    public void sendStop() { 
        if (!isRunning || ws == null) return;
        ws.broadcast("{\"type\":\"stop\",\"playerId\":\"" + playerId + "\"}"); 
    }
    
    /**
     * Gets the player ID associated with this sidecar.
     * 
     * @return The player UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Checks if the sidecar is currently running.
     * 
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    private void handleHttpRequest(HttpExchange exchange) throws IOException {
        String path = Optional.ofNullable(exchange.getRequestURI().getPath()).orElse("/");
        if ("/".equals(path)) path = "/index.html";

        if ("/index.html".equals(path)) {
            byte[] body = readResourceBytes("/speech_recognition/index.html");
            if (body == null) { 
                send404(exchange); 
                return; 
            }

            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) { 
                os.write(body); 
            }
        } else {
            send404(exchange);
        }
    }
    
    private void handleSpeechCommand(SpeechCommand command) {
        if (commandHandler != null) {
            commandHandler.accept(command);
        } else {
            System.out.printf("Player %s: CAST: %s (heard: \"%s\", conf=%.2f)%n", 
                playerId, command.getSpell(), command.getHeard(), command.getConfidence());
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
        try (var os = ex.getResponseBody()) { 
            os.write(body); 
        }
    }

    private static byte[] readResourceBytes(String path) throws IOException {
        try (InputStream in = VoiceSidecar.class.getResourceAsStream(path)) {
            if (in == null) return null;
            return in.readAllBytes();
        }
    }

    // ==== WebSocket server (localhost only) ====
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

        @Override 
        public void onOpen(WebSocket conn, ClientHandshake hs) { 
            WebSocket existing = client.getAndSet(conn);
            if (existing != null && existing.isOpen()) {
                // Close existing connection if there was one
                existing.close();
            }
            System.out.println("Speech client connected for player " + playerId);
        }
        
        @Override 
        public void onClose(WebSocket conn, int code, String reason, boolean remote) { 
            client.compareAndSet(conn, null);
            System.out.println("Speech client disconnected for player " + playerId);
        }
        
        @Override 
        public void onError(WebSocket conn, Exception ex) { 
            System.err.println("WebSocket error for player " + playerId + ": " + ex.getMessage());
        }
        
        @Override 
        public void onStart() { 
            System.out.println("WebSocket server started for player " + playerId);
        }

        @Override 
        public void onMessage(WebSocket conn, String message) {
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
                    
                    SpeechCommand command = new SpeechCommand(
                        playerId, spell, heard, matchKind, distance, confidence, timestamp
                    );
                    
                    commandHandler.accept(command);
                }
            } catch (Exception e) {
                System.err.println("Error processing speech command for player " + playerId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        public void broadcast(String json) {
            WebSocket conn = client.get();
            if (conn != null && conn.isOpen()) {
                conn.send(json);
            }
        }
    }
} 