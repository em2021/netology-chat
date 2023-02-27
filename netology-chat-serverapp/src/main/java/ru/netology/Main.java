package ru.netology;

import java.io.*;
import java.util.NoSuchElementException;

public class Main {

    private final static String CONFIG_PATH = "./config";
    private final static String SETTINGS_FILENAME = "settings.txt";
    private final static int PORT;

    static {
        try {
            PORT = init(CONFIG_PATH, SETTINGS_FILENAME);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {

        Server server = new Server();
        System.out.println("Starting server on port " + PORT + "...");
        server.listen(PORT);
    }


    private static int init(String configPath, String settingsFileName) throws Exception {
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
        int port = -1;
        if (settingsFile.exists()) {
            System.out.printf("%s%s%s%n", "File \"", settingsFileName, "\" found successfully...");
            port = getPort(settingsFile);
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
        return port;
    }

    private static int getPort(File settings) throws NumberFormatException {
        int port = -1;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(settings))) {
            do {
                String line = bufferedReader.readLine();
                if (line.startsWith("port:")) {
                    String p = line.substring(line.indexOf(" ") + 1);
                    return port = Integer.parseInt(p);
                }
            } while (bufferedReader.ready());
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return port;
    }
}