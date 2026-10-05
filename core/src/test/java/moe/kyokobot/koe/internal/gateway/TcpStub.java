package moe.kyokobot.koe.internal.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Local TCP server standing in for a voice gateway that never gets as far as a TLS handshake.
 */
final class TcpStub implements AutoCloseable {
    enum Behavior {
        /** Accepts and reads whatever the client sends, never responds. */
        SILENT,
        /** Closes the connection right after accepting it. */
        DROP,
        /** Responds with plaintext HTTP instead of TLS. */
        GARBAGE
    }

    private final ServerSocket server;
    private final Behavior behavior;
    private final List<Socket> sockets = new CopyOnWriteArrayList<>();
    final AtomicInteger accepted = new AtomicInteger();
    final AtomicInteger closedByClient = new AtomicInteger();

    TcpStub(Behavior behavior) throws IOException {
        this.behavior = behavior;
        this.server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        var thread = new Thread(this::acceptLoop, "TcpStub-accept");
        thread.setDaemon(true);
        thread.start();
    }

    String endpoint() {
        return server.getInetAddress().getHostAddress() + ":" + server.getLocalPort();
    }

    /**
     * @return an endpoint nothing listens on
     */
    static String refusingEndpoint() throws IOException {
        try (var socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getInetAddress().getHostAddress() + ":" + socket.getLocalPort();
        }
    }

    private void acceptLoop() {
        while (!server.isClosed()) {
            try {
                var socket = server.accept();
                accepted.incrementAndGet();
                sockets.add(socket);

                switch (behavior) {
                    case DROP:
                        socket.close();
                        break;
                    case GARBAGE:
                        socket.getOutputStream().write("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                        socket.getOutputStream().flush();
                        drainInBackground(socket);
                        break;
                    case SILENT:
                        drainInBackground(socket);
                        break;
                }
            } catch (IOException ignored) {
                // closed
            }
        }
    }

    private void drainInBackground(Socket socket) {
        var thread = new Thread(() -> {
            try (InputStream in = socket.getInputStream()) {
                var buf = new byte[4096];
                while (in.read(buf) != -1) {
                    // discard
                }
                closedByClient.incrementAndGet();
            } catch (IOException ignored) {
                closedByClient.incrementAndGet();
            }
        }, "TcpStub-drain");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void close() throws IOException {
        server.close();
        for (var socket : sockets) {
            socket.close();
        }
    }
}
