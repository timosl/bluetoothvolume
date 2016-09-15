package de.timosl.bluetoothvolumeadjust;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Class that provides static methods for logging
 * into an internal file. so the user may send it
 * as a report later.
 * Also controls if logs should be displayed in Logcat.
 */
public class L {

    /**
     * Set this to 'true' if all logs should be written to Logcat,
     * set it to 'false' to hide the logs.
     */
    private static final boolean ENABLE_DEBUG_LOGCAT = false;

    /**
     * The {@link Log} prefix.
     */
    private static final String TAG = "bluetoothAdjust";

    /**
     * The name of the log file.
     */
    private static final String LOGFILE_NAME = "debug.log";

    /**
     * The string prepended to log messages of WARNING level.
     */
    private static final String LEVEL_WARNING = "WARNING";

    /**
     * The string prepended to log messages of INFO level.
     */
    private static final String LEVEL_INFO = "INFO";

    /**
     * The applications {@link Context}.
     */
    private static Context applicationContext;

    /**
     * Initializes the logging system with the given application {@link Context}.
     * @param context The applications {@link Context}
     */
    public static void init(Context context) {
        applicationContext = context;
    }

    /**
     * Logs the given message with the given level
     * @param level The log level
     * @param message The message
     */
    private static void log(String level, String message) {
        // Post it to Logcat if enabled
        if (ENABLE_DEBUG_LOGCAT) {
            Log.d(TAG,message);
        }

        // If logging is enabled by the user preference, append
        // this message to the log file
        if (Preferences.getEnableDebugging(applicationContext)) {
            // Locate the log file
            File debugFile  = new File(applicationContext.getFilesDir(),LOGFILE_NAME);

            // Construct the complete message
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(new Date().toString());
            messageBuilder.append(" ["+ level +"] - ");
            messageBuilder.append(message);
            messageBuilder.append("\n");

            // Write to the log file
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(debugFile,true));
                writer.append(messageBuilder.toString());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Writes an INFO log message.
     * @param message The log message
     */
    public static void i(String message) {
        log(LEVEL_INFO,message);
    }

    /**
     * Writes a WARNING log message.
     * @param message The log message
     */
    public static void w(String message) {
        log(LEVEL_WARNING,message);
    }

    /**
     * Clears the complete log file.
     */
    public static void clearLog() {
        File debugFile  = new File(applicationContext.getFilesDir(),LOGFILE_NAME);
        debugFile.delete();
    }

    /**
     * Retrieves the current content of the log file.
     * @return The content of the log file as a String
     */
    public static String getLog() {
        // Locate the log file
        File debugFile  = new File(applicationContext.getFilesDir(),LOGFILE_NAME);

        try {
            BufferedReader reader = new BufferedReader(new FileReader(debugFile));

            // Read the file line-for-line and use the StringBuilder
            // to concatenate the strings
            StringBuilder builder = new StringBuilder();
            String s;
            while((s = reader.readLine()) != null) {
                builder.append(s);
                builder.append("\n");
            }

            return builder.toString();
        } catch (Exception e) {
            // Oops, something went wrong while reading from the log file
            e.printStackTrace();
            return null;
        }
    }
}
