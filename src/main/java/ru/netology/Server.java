package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final String BAD_REQUEST_400 = "400 Bad Request";
    private static final String NOT_FOUND_404 = "404 Not Found";
    private static final String OK_200 = "200 OK";
    private final int MAX_THREADS = 64;
    private final ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
    private final int port;
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");


    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                processRequests(serverSocket.accept());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processRequests(Socket socket) {
        pool.submit(() -> processSingleRequest(socket));
    }

    private void processSingleRequest(Socket socket) {
        try (
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");
            byte[] content = null;
            String contentType = null;

            if (parts.length != 3) {
                writeResponse(out, BAD_REQUEST_400, content, contentType);
                return;
            }

            final var path = parts[1];
            if (!validPaths.contains(path)) {
                writeResponse(out, NOT_FOUND_404, content, contentType);
                return;
            }

            final var filePath = Path.of(".", "public", path);
            contentType = Files.probeContentType(filePath);

            if (path.equals("/classic.html")) {
                var template = Files.readString(filePath);
                content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                writeResponse(out, OK_200, content, contentType);
                return;
            }
            writeResponse(out, OK_200, Files.readAllBytes(filePath), contentType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeResponse(BufferedOutputStream out, String code, byte[] content, String contentType) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 " + code + "\r\n");
        if (content != null) {
            sb.append("Content-Type: " + contentType + "\r\n");
            sb.append("Content-Length: " + content.length + "\r\n");
        } else {
            sb.append("Content-Length: 0\r\n");
        }
        sb.append("Connection: close\r\n" + "\r\n");
        out.write(sb.toString().getBytes());
        if (content != null) {
            out.write(content);
        }
        out.flush();
    }
}