package yaroslavalexashkin.cheburping;

import android.content.Context;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    String logPath;
    FileWriter logfile;

    Logger(Context c) {
        logPath = c.getFilesDir().getAbsolutePath() + "/history.log";
    }

    void write(String s) {
        try {
            if (logfile == null)
                logfile = new FileWriter(logPath, true);
            logfile.write("[" + DateTimeFormatter.ofPattern("dd/MMM/yyyy HH:mm:ss").format(LocalDateTime.now()) + "]" + s + "\n");
            logfile.flush();
        } catch (IOException ignored) {
        }
    }

    void quit() {
        try {
            logfile.close();
        } catch (IOException ignored) {
        }
    }

    String readAll() {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(logPath));
            return new String(encoded);
        } catch (IOException e) {
            return e.getMessage();
        }
    }
}
