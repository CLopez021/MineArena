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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Client-side sidecar that serves the minimal proxy page and bridges WS messages.
 * Singleton per client.
 */
public final class VoiceSidecar {
	private static VoiceSidecar instance;

	private final Gson gson = new Gson();
	private HttpServer http;
	private WsBridge ws;
	private int httpPort;
	private int wsPort;
	private boolean isRunning = false;

	private VoiceSidecar() {
	}

	public static VoiceSidecar getInstance() {
		if (instance == null) instance = new VoiceSidecar();
		return instance;
	}

    public void start(Map<String, String> phraseToName, String lang) throws Exception {
		if (isRunning) return;

		wsPort = pickFreePort();
		httpPort = pickFreePort();

		ws = new WsBridge(wsPort, this::handleWebSocketMessage);
		ws.start();

		http = HttpServer.create(new InetSocketAddress("127.0.0.1", httpPort), 0);
		http.createContext("/", this::handleHttpRequest);
		http.start();

		// Open browser prompt on the client
		UUID playerId = getCurrentPlayerId();
		String url = "http://127.0.0.1:" + httpPort + "/index.html?wsPort=" + wsPort + "&playerId=" + playerId;
		VoiceSidecarUi.promptAndOpen(url);

        // Push initial config
        sendConfig(lang, phraseToName);

		isRunning = true;
	}

	public void stop() {
		if (!isRunning) return;

		try { if (ws != null) ws.stop(1000); } catch (Exception ignored) {}
		try { if (http != null) http.stop(0); } catch (Exception ignored) {}

		isRunning = false;
	}

    public void sendConfig(String lang, Map<String, String> phraseToName) {
        if (!isRunning || ws == null) return;

		UUID playerId = getCurrentPlayerId();
		JsonObject cfg = new JsonObject();
		cfg.addProperty("type", "config");
		cfg.addProperty("playerId", playerId.toString());
        if (lang != null) cfg.addProperty("lang", lang);
        if (phraseToName != null) {
            JsonArray arr = new JsonArray();
            for (Map.Entry<String, String> e : phraseToName.entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("phrase", e.getKey());
                o.addProperty("name", e.getValue());
                arr.add(o);
            }
            cfg.add("spells", arr);
        }
        ws.broadcast(gson.toJson(cfg));
    }

	public void sendStart() {
		if (!isRunning || ws == null) return;
		UUID playerId = getCurrentPlayerId();
		ws.broadcast("{\"type\":\"start\",\"playerId\":\"" + playerId + "\"}");
	}

	public void sendStop() {
		if (!isRunning || ws == null) return;
		UUID playerId = getCurrentPlayerId();
		ws.broadcast("{\"type\":\"stop\",\"playerId\":\"" + playerId + "\"}");
	}

	public boolean isRunning() { return isRunning; }

	private UUID getCurrentPlayerId() {
		var player = net.minecraft.client.Minecraft.getInstance().player;
		return player != null ? player.getUUID() : new UUID(0L, 0L);
	}

	private void handleWebSocketMessage(String message) {
		try {
			JsonObject msg = JsonParser.parseString(message).getAsJsonObject();
			String type = msg.get("type").getAsString();
			
			if ("spellCast".equals(type)) {
				// Parse the spell cast message
				String spell = msg.get("spell").getAsString();

				UUID playerId = getCurrentPlayerId();
				SpeechCommand command = new SpeechCommand(playerId, spell);
				handleSpeechCommand(command);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
		// Always forward to server via packet
		PacketHandler.INSTANCE.send(new RecognizedSpeechPacket(
				command.getSpell()
		), net.minecraftforge.network.PacketDistributor.SERVER.noArg());
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
		private final Consumer<String> messageHandler;

		public WsBridge(int port, Consumer<String> messageHandler) throws UnknownHostException {
			super(new InetSocketAddress("127.0.0.1", port));
			this.messageHandler = messageHandler;
		}

		@Override public void onOpen(WebSocket conn, ClientHandshake hs) {
			WebSocket existing = client.getAndSet(conn);
			if (existing != null && existing.isOpen()) existing.close();
		}
		@Override public void onClose(WebSocket conn, int code, String reason, boolean remote) { client.compareAndSet(conn, null); }
		@Override public void onError(WebSocket conn, Exception ex) { ex.printStackTrace(); }
		@Override public void onStart() { }

		@Override public void onMessage(WebSocket conn, String message) {
			messageHandler.accept(message);
		}

		public void broadcast(String json) {
			WebSocket conn = client.get();
			if (conn != null && conn.isOpen()) conn.send(json);
		}
	}
} 
