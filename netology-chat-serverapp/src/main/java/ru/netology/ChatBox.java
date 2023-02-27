package ru.netology;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class ChatBox {

    private static ChatBox INSTANCE = null;
    private static final String LOGS_PATH = "./logs";
    private static final String LOG_FILENAME = "file.log";
    private static File log;
    private static Logger logger;
    private static ConcurrentMap<Consumer<String>, String> members;
    private static final String ADMIN_NAME = "admin";
    private static final String GREETING_MESSAGE = "Welcome to ChatBox";
    private static final String BYE_MESSAGE = "Bye";

    private ChatBox() {
    }

    public static ChatBox getInstance() throws IOException {
        if (INSTANCE == null) {
            synchronized (ChatBox.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ChatBox();
                    if (initLogFiles(LOGS_PATH, LOG_FILENAME)) {
                        log = new File(LOGS_PATH, LOG_FILENAME);
                    }
                    logger = Logger.getInstance();
                    members = new ConcurrentHashMap<>();
                }
            }
        }
        return INSTANCE;
    }

    private static boolean initLogFiles(String path, String file) throws IOException {
        File logDir = new File(path);
        File log = new File(logDir, file);
        if (!logDir.exists()) {
            if (logDir.mkdir()) {
                System.out.printf("%s%s%s%n", "Directory \"", logDir.getName(), "\" created successfully");
            } else {
                System.out.printf("%s%s%s%n", "Could not create \"", logDir.getName(), "\" directory");
            }
        } else {
            System.out.printf("%s%s%s%n", "Directory \"", logDir.getName(), "\" already exists");
        }
        if (log.exists()) {
            System.out.printf("%s%s%s%n", "File \"", log.getName(), "\" file already exists");
            return true;
        } else {
            if (log.createNewFile()) {
                System.out.printf("%s%s%s%n", "File \"", log.getName(), "\" created successfully");
                return true;
            } else {
                System.out.printf("%s%s%s%n", "Could not create file \"", log.getName(), "\"");
            }
        }
        return false;
    }

    public boolean join(UserThread user) throws IOException {
        String userName = user.getUserName();
        if (!members.containsValue(userName)) {
            String result = members.put(user, userName);
            if (result == null) {
                System.out.println(userName + " joined chat");
                notifyMembersExcept(formatMessage(ADMIN_NAME, userName + " joined chat"), user);
                sendPrivateMessage(formatMessage(ADMIN_NAME, GREETING_MESSAGE + ", " + userName), user);
                return true;
            }
        }
        user.accept("User name \"" + user.getUserName() + "\" is already taken\n");
        return false;
    }

    public boolean exit(UserThread user) throws IOException {
        sendPrivateMessage(formatMessage(ADMIN_NAME, BYE_MESSAGE + ", " + user.getUserName()), user);
        boolean result = members.remove(user, user.getUserName());
        if (result) {
            System.out.println(user.getUserName() + " left chat");
            notifyAllMembers(formatMessage(ADMIN_NAME, user.getUserName() + " left chat"));
        }
        return result;
    }

    private String formatMessage(String sender, String message) {
        return String.format("%s %s: %s%s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")),
                sender,
                message,
                "\r\n");
    }

    public boolean sendMessage(String message, UserThread userThread) throws IOException {
        String formattedMessage = formatMessage(userThread.getUserName(), message);
        members.keySet().stream()
                .filter(s -> !s.equals(userThread))
                .forEach((s) -> s.accept(formattedMessage));
        logger.Log(formattedMessage, log);
        if (members.keySet().size() == 1) {
            sendPrivateMessage(formatMessage(ADMIN_NAME,
                    "You are the only chat member at the moment"), userThread);
        }
        return true;
    }

    public boolean sendPrivateMessage(String message, UserThread userThread) throws IOException {
        members.keySet().stream()
                .filter(s -> s.equals(userThread))
                .forEach((s) -> s.accept(message));
        logger.Log(message, log);
        return true;
    }

    private void notifyAllMembers(String message) throws IOException {
        members.keySet().forEach((s) -> s.accept(message));
        logger.Log(message, log);
    }

    private void notifyMembersExcept(String message, UserThread userThread) throws IOException {
        members.keySet().stream()
                .filter(s -> !s.equals(userThread))
                .forEach((s) -> s.accept(message));
        logger.Log(message, log);
    }
}