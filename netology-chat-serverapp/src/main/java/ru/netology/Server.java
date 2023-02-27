package ru.netology;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public void listen(int port) {
        System.out.println("Server started on port " + port + "...");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                threadPool.submit(new UserThread(serverSocket.accept()));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}