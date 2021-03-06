package main;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Singleton utility class used for logging user interactions with the program.
 */
public class LogUtility {
    /**
     * Name of the rename logger.
     */
    public static final String RENAME_LOGGER_NAME = "renameLogger";
    /**
     * Name of the action logger.
     */
    public static final String ACTION_LOGGER_NAME = "actionLogger";
    private static LogUtility logUtility;
    /**
     * Logs tag related events
     */
    private Logger actionLogger;
    /**
     * Logs renaming events
     */
    private Logger renameLogger;

    /**
     * Instantiates the singleton LogUtility and loggers
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private LogUtility() throws IOException {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT [%4$-2s: %5$s] %n");
        renameLogger = initializeLogger(RENAME_LOGGER_NAME);
        actionLogger = initializeLogger(ACTION_LOGGER_NAME);
    }

    /**
     * Get the singleton instance of LogUtility
     *
     * @return the instance
     */
    static LogUtility getInstance() {
        if (logUtility == null) {
            try {
                logUtility = new LogUtility();
            } catch (IOException e) {
                System.out.println("Could not create log file(s).");
            }
        }
        return logUtility;
    }

    /**
     * Initializes a Logger object based on the loggerName passed in
     *
     * @param loggerName name of the Logger to be initialized
     * @return the initialized Logger
     * @throws IOException thrown if file I/O causes any errors
     */
    private Logger initializeLogger(String loggerName) throws IOException {
        String loggerFileName = loggerName + ".txt";
        Logger logger = Logger.getLogger(loggerName);
        File loggerFile = new File(loggerFileName);
        if (!loggerFile.createNewFile()) {
            System.out.println(String.format("Appending to existing %s.",
                    loggerFileName));
        }
/*        try {
            Files.createFile(Paths.get(filePath));
        } catch (IOException e1) {
            System.out.println(String.format("Could not create %s.",
                    loggerFileName));
        }*/
        FileHandler handler = new FileHandler(loggerFileName, true);
        SimpleFormatter formatter = new SimpleFormatter();
        handler.setFormatter(formatter);
        logger.addHandler(handler);
        return logger;
    }

    /**
     * Logs an add tag event
     *
     * @param tagName   name of tag that's added
     * @param imageName name of the image where the add tag takes place
     */
    void logAddTag(String tagName, String imageName) {
        String message = String.format("Created new Tag %s for Image %s",
                tagName, imageName);
        actionLogger.log(Level.INFO, message);
    }

    /**
     * Logs an delete tag event
     *
     * @param tagName   name of tag that's added
     * @param imageName name of the image where the add tag takes place
     */
    void logDeleteTag(String tagName, String imageName) {
        String message = String.format("Deleted Tag %s from Image %s",
                tagName, imageName);
        actionLogger.log(Level.INFO, message);
    }

    /**
     * Logs an image revert name event
     *
     * @param oldName the name the image currently has
     * @param newName the name to revert to
     */
    void logRevertName(String oldName, String newName) {
        String message = String.format("Reverted name from %s to %s",
                oldName, newName);
        renameLogger.log(Level.INFO, message);
        actionLogger.log(Level.INFO, message);
    }

    /**
     * Logs an image rename event
     *
     * @param oldName the old name of the image
     * @param newName the new name of the image
     */
    void logImageRename(String oldName, String newName) {
        String message = String.format("Changed name from %s to %s", oldName,
                newName);
        renameLogger.log(Level.INFO, message);
    }

    /**
     * Logs moving an image.
     *
     * @param oldDir Old directory of Image.
     * @param newDir New directory of Image.
     */
    void logMoveImage(String oldDir, String newDir) {
        String message = String.format("Moved image from %s to %s", oldDir,
                newDir);
        actionLogger.log(Level.INFO, message);
    }
}
