package util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsoleLog implements Log {
    private static Logger logger;

    public ConsoleLog(String tag) {
        logger = Logger.getLogger(tag);
    }

    @Override
    public void e(String msg) {
        logger.log(Level.SEVERE, msg);
    }

    @Override
    public void e(String msg, Throwable e) {
        logger.log(Level.SEVERE, msg, e);
    }

    @Override
    public void i(String msg) {
        logger.log(Level.INFO, msg);
    }

    @Override
    public void w(String msg) {
        logger.log(Level.WARNING, msg);
    }

    @Override
    public void v(String msg) {
        logger.log(Level.ALL, msg);
    }
}
