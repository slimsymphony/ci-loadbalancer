package com.nokia;

import hudson.slaves.RetentionStrategy;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import static hudson.util.TimeUnit2.MINUTES;
import static java.util.logging.Level.WARNING;

/**
 * Created by IntelliJ IDEA. User: miikka Date: 1/3/12 Time: 3:12 PM To change
 * this template use File | Settings | File Templates.
 */
public class LBSlaveRetentionStrategy extends
        RetentionStrategy<LBComputer> {
    int idleMinutes;
    private static final Logger LOGGER = Logger
            .getLogger(LBSlaveRetentionStrategy.class.getName());
    public static boolean disabled = Boolean
            .getBoolean(LBSlaveRetentionStrategy.class.getName()
                    + ".disabled");

    public LBSlaveRetentionStrategy(int idleMinutes) {
        super();
        this.idleMinutes = idleMinutes;
    }

    @Override
    public long check(LBComputer c) {
        LOGGER.info("Check computer: " + c.getDisplayName());
        if ((c.isIdle() || c.isOffline()) 
                && !disabled 
                && !c.isConnecting()) {
            final long idleMilliseconds = System.currentTimeMillis()
                    - c.getIdleStartMilliseconds();
            if (idleMilliseconds > MINUTES.toMillis(idleMinutes)) {
                c.setAcceptingTasks(false);
                LOGGER.info("Disconnecting " + c.getName());
                try {
                    // release(c);
                    if (c.getNode() != null) {
                        LOGGER.info("Terminating node " + c.getNode().getDisplayName());
                        c.getNode().terminate();
                    } else {
                        LOGGER.info("Node was null, disconnecting computer " + c.getDisplayName());
                        c.doDoDisconnect("Computer idle time exceeded and node was null. Disconnecting the computer");
                    }
                } catch (InterruptedException e) {
                    LOGGER.log(WARNING, "Failed to terminate " + c.getName(), e);
                    e.printStackTrace();
                } catch (IOException e) {
                    LOGGER.log(WARNING, "Failed to terminate " + c.getName(), e);
                    e.printStackTrace();
                } catch (ServletException e) {
                    LOGGER.log(WARNING, "Failed to terminate " + c.getName(), e);
                    e.printStackTrace();
                }
            }
        }
        return 1;
    }

    @Override
    public void start(LBComputer c) {
        if (!c.isConnecting()) {
            c.connect(false);
        }
    }

}
