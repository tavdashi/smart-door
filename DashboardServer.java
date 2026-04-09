package com.smartdoor.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/**
 * DashboardServer — broadcasts door events to the HTML dashboard via WebSocket.
 *
 * No extra libraries needed — uses Java's built-in HTTP server + raw WebSocket handshake.
 *
 * How to use:
 *   DashboardServer server = new DashboardServer(8887);
 *   server.start();
 *   // then whenever a door event happens:
 *   server.broadcast("{\"type\":\"open\"}");
 *
 * The HTML dashboard connects to ws://localhost:8887
 */
public class DashboardServer {

    private final int port;
    private ServerSocket serverSocket;
    private final List<WebSocketClient> clients = new CopyOnWriteArrayList<>();
    private volatile boolean running = false;

    public DashboardServer(int port) {
        this.port = port;
    }

    /** Start the WebSocket server on a background thread */
    public void start() {
        running = true;
        Thread t = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("[DASHBOARD] WebSocket server started on ws://localhost:" + port);
                System.out.println("[DASHBOARD] Open SmartDoorDashboard.html in your browser now!");
                while (running) {
                    try {
                        Socket client = serverSocket.accept();
                        WebSocketClient ws = new WebSocketClient(client, this);
                        clients.add(ws);
                        ws.start();
                    } catch (IOException e) {
                        if (running) System.err.println("[DASHBOARD] Accept error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("[DASHBOARD] Could not start server on port " + port + ": " + e.getMessage());
            }
        }, "DashboardServer");
        t.setDaemon(true);
        t.start();
    }

    /** Broadcast a JSON message to all connected dashboard browsers */
    public void broadcast(String jsonMessage) {
        for (WebSocketClient client : clients) {
            try {
                client.sendText(jsonMessage);
            } catch (Exception e) {
                clients.remove(client);
            }
        }
    }

    /** Send a door OPEN event to the dashboard */
    public void sendDoorOpen() {
        broadcast("{\"type\":\"open\"}");
    }

    /** Send a door CLOSED event to the dashboard */
    public void sendDoorClose() {
        broadcast("{\"type\":\"close\"}");
    }

    /** Send an alert event to the dashboard */
    public void sendAlert(String level, String message) {
        broadcast("{\"type\":\"alert\",\"level\":\"" + level + "\",\"msg\":\"" + escapeJson(message) + "\"}");
    }

    /** Send a lock event to the dashboard */
    public void sendLock() {
        broadcast("{\"type\":\"locked\"}");
    }

    /** Send an unlock event to the dashboard */
    public void sendUnlock() {
        broadcast("{\"type\":\"unlocked\"}");
    }

    void removeClient(WebSocketClient client) {
        clients.remove(client);
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ── Inner WebSocket Client Handler ─────────────────────────────────────────
    static class WebSocketClient extends Thread {
        private final Socket socket;
        private final DashboardServer server;
        private OutputStream out;
        private boolean handshakeDone = false;

        WebSocketClient(Socket socket, DashboardServer server) {
            this.socket = socket;
            this.server = server;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                InputStream in = socket.getInputStream();
                out = socket.getOutputStream();

                // Read HTTP upgrade request
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder requestHeaders = new StringBuilder();
                String line;
                String wsKey = null;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    requestHeaders.append(line).append("\r\n");
                    if (line.startsWith("Sec-WebSocket-Key:")) {
                        wsKey = line.substring(line.indexOf(":") + 1).trim();
                    }
                }

                if (wsKey == null) { socket.close(); return; }

                // Send WebSocket handshake response
                String acceptKey = generateAcceptKey(wsKey);
                String response =
                    "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";
                out.write(response.getBytes(StandardCharsets.UTF_8));
                out.flush();
                handshakeDone = true;

                System.out.println("[DASHBOARD] Browser connected! Dashboard is now live.");

                // Keep connection alive — read frames (ping/close)
                byte[] buf = new byte[1024];
                while (!socket.isClosed()) {
                    int b = in.read();
                    if (b == -1) break;
                    // We don't need to process incoming frames for this use case
                }

            } catch (Exception e) {
                // Client disconnected
            } finally {
                server.removeClient(this);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        /** Send a WebSocket text frame to the browser */
        synchronized void sendText(String text) throws IOException {
            if (!handshakeDone || socket.isClosed()) return;
            byte[] payload = text.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream frame = new ByteArrayOutputStream();

            // FIN + opcode 1 (text)
            frame.write(0x81);

            // Payload length
            if (payload.length <= 125) {
                frame.write(payload.length);
            } else if (payload.length <= 65535) {
                frame.write(126);
                frame.write((payload.length >> 8) & 0xFF);
                frame.write(payload.length & 0xFF);
            } else {
                frame.write(127);
                for (int i = 7; i >= 0; i--) {
                    frame.write((int)((payload.length >> (8 * i)) & 0xFF));
                }
            }

            frame.write(payload);
            out.write(frame.toByteArray());
            out.flush();
        }

        private static String generateAcceptKey(String key) throws Exception {
            String magic = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] sha1 = md.digest(magic.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sha1);
        }
    }
}
