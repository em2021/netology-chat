package ru.netology;

import java.io.*;

public class Logger {

    private static Logger INSTANCE = null;

    private Logger() {
    }

    public static Logger getInstance() {
        if (INSTANCE == null) {
            synchronized (Logger.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Logger();
                }
            }
        }
        return INSTANCE;
    }

    public boolean Log(String message, File file) throws IOException {
        if (file.exists()) {
            try (BufferedWriter os = new BufferedWriter(new FileWriter(file, true))) {
                os.write(message);
                os.flush();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            return true;
        }
        return false;
    }
}