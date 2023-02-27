package ru.netology;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class UserThread implements Runnable, Consumer<String> {

    private final Socket socket;
    private final BufferedReader in;
    private final OutputStream out;
    private String userName = null;
    private final String info = "You are successfully connected to ChatBox!\r\n" +
            "Please, give us a second to check your name...\r\n";
    private final String byeMessage = "Bye";
    private ChatBox chatBox;

    public UserThread(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new BufferedOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        System.out.println("New connection accepted in thread " + Thread.currentThread().getName() + " ...");
        try (in; out) {
            writeToUser(info);
            while (!socket.isClosed()) {
                String input;
                while ((input = in.readLine()) != null) {
                    System.out.println(input);
                    if (input.equals("/exit")) {
                        disconnect();
                    } else if (input.matches("((.*)\\e(.*))*|\\h*|\\s*|\\v*|")) {
                        writeToUser("Illegal name\r\n");
                    } else {
                        setUserName(input);
                        this.chatBox = ChatBox.getInstance();
                        if (chatBox.join(this)) {
                            listen();
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void disconnect() throws IOException {
        System.out.println("Connection in thread " + Thread.currentThread().getName() + " closed...");
        socket.close();
    }

    private void writeToUser(String message) throws IOException {
        out.write(message.getBytes());
        out.flush();
    }

    private void listen() throws IOException {
        String input;
        while ((input = in.readLine()) != null) {
            if (input.equals("/exit")) {
                chatBox.exit(this);
                disconnect();
                break;
            }
            chatBox.sendMessage(input, this);
        }
    }

    //Consumer interface implementation
    //Accepts messages from users
    @Override
    public void accept(String s) {
        try {
            writeToUser(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }
}
