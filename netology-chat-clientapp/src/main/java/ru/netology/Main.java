package ru.netology;

import java.io.*;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private enum SETTING_TYPE {HOST_NAME, PORT}

    private static final String CONFIG_PATH = "./config";
    private static final String SETTINGS_FILENAME = "settings.txt";
    private static String HOST_NAME;
    private static int PORT;
    private static final String LOGS_PATH = "./logs";
    private static final String LOG_FILENAME = "file.log";
    private static File log;
    private static Logger logger;
    private static String userName;
    private static boolean userNameRequested = false;
    private static boolean userNameSent = false;
    private static boolean userNameResetRequested = false;
    private static String info = "Welcome to ChatBox!\n" +
            "To exit or disconnect type \"/exit\" at any time and press ENTER";
    private static final Scanner scanner = new Scanner(System.in);
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(2);

    public static void main(String[] args) throws Exception {
        System.out.println(info);
        init(CONFIG_PATH, SETTINGS_FILENAME);
        String name = requestUserName();
        if ("/exit".equals(name)) {
            System.out.println("Bye!");
        } else {
            setUserName(name);
            try (Socket socket = new Socket(HOST_NAME, PORT);
                 OutputStream out = new BufferedOutputStream(socket.getOutputStream());
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                System.out.println("Connection to " + HOST_NAME + ": " + PORT + " established successfully...");
                if (initLogFiles(LOGS_PATH, LOG_FILENAME)) {
                    log = new File(LOGS_PATH, LOG_FILENAME);
                }
                logger = Logger.getInstance();
                //writing thread
                threadPool.submit(() -> {
                    System.out.println("Writer thread started successfully in thread " + Thread.currentThread().getName());
                    try (scanner; out) {
                        synchronized (out) {
                            while (!userNameRequested) {
                                out.wait();
                            }
                            out.write((userName + "\r\n").getBytes());
                            out.flush();
                            userNameSent = true;
                            out.notify();
                        }
                        while (true) {
                            String input;
                            if (scanner.hasNext()) {
                                if (userNameResetRequested) {
                                    synchronized (scanner) {
                                        userNameSent = false;
                                        scanner.wait();
                                    }
                                    out.write((userName + "\r\n").getBytes());
                                    out.flush();
                                    userNameSent = true;
                                }
                                input = scanner.nextLine();
                                out.write((input + "\r\n").getBytes());
                                out.flush();
                                if ("/exit".equals(input)) {
                                    break;
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("Writing thread finished...");
                });

                //reading thread
                threadPool.submit(() -> {
                    System.out.println("Reader thread started successfully in thread " + Thread.currentThread().getName());
                    try (in; out) {
                        String input;
                        while ((input = in.readLine()) != null) {
                            System.out.println(input);
                            if (input.equals("Please, give us a second to check your name...")) {
                                synchronized (out) {
                                    userNameRequested = true;
                                    out.notify();
                                }
                            } else if (input.matches("User name [.*] is already taken")) {
                                userNameResetRequested = true;
                                synchronized (scanner) {
                                    resetUserName();
                                    scanner.notify();
                                }
                            } else {
                                logger.Log((input + "\r\n"), log);
                            }
                        }
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                    System.out.println("Reading thread finished...");
                });
                while (!socket.isClosed()) {
                }
                threadPool.shutdownNow();
                System.out.println("Connection to " + HOST_NAME + ": " + PORT + " closed...");
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void init(String configPath, String settingsFileName) throws Exception {
        System.out.println("Initializing settings...");
        File configDir = new File(configPath);
        File settingsFile;
        if (configDir.exists()) {
            System.out.printf("%s%s%s%n", "Directory \"", configPath, "\" found successfully...");
            settingsFile = new File(configDir, settingsFileName);
        } else {
            System.out.printf("%s%s%s%n", "Missing", configPath, "\" directory...");
            throw new FileNotFoundException();
        }
        int port;
        String host;
        if (settingsFile.exists()) {
            System.out.printf("%s%s%s%n", "File \"", settingsFileName, "\" found successfully...");
            port = Integer.parseInt(getSettings(settingsFile, SETTING_TYPE.PORT));
            host = getSettings(settingsFile, SETTING_TYPE.HOST_NAME);
        } else {
            System.out.printf("%s%s%s%n", "Missing \"", settingsFileName, "\" file...");
            throw new FileNotFoundException();
        }
        if (port > 1023 && port < 65535) {
            System.out.println("Port number found successfully...");
        } else {
            System.out.println("Missing port number...");
            throw new NoSuchElementException();
        }
        if (host != null) {
            System.out.println("Host name found successfully...");
        } else {
            System.out.println("Missing host name...");
            throw new NoSuchElementException();
        }
        PORT = port;
        HOST_NAME = host;
    }

    private static String getSettings(File settings, SETTING_TYPE type) throws NumberFormatException {
        String searchType;
        String result = null;
        if (type == SETTING_TYPE.PORT) {
            result = "-1";
            searchType = "port:";
        } else {
            searchType = "host:";
        }
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(settings))) {
            do {
                String line = bufferedReader.readLine();
                if (line.startsWith(searchType)) {
                    result = line.substring(line.indexOf(" ") + 1);
                    return result;
                }
            } while (bufferedReader.ready());
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return result;
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

    private static String requestUserName() {
        System.out.println("Please enter your name and press ENTER...");
        String input;
        while (true) {
            input = scanner.nextLine();
            input.trim();
            if (input.length() < 1) {
                System.out.println("Name entered is not valid. Please, try again...");
            } else {
                break;
            }
        }
        return input;
    }

    private static void setUserName(String name) {
        userName = name;
    }

    private static boolean resetUserName() {
        setUserName(requestUserName());
        return true;
    }
}