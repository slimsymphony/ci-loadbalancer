/**
 * 
 */
package com.nokia;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.AbortException;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

/**
 * @author ttyppo
 * 
 */
public abstract class LBComputerLauncher extends ComputerLauncher {

    private static final Logger LOG = Logger.getLogger(LBComputerLauncher.class
            .getName());
    private long SLAVE_LAUNCH_TIMEOUT = 15 * 60 * 1000;

    @Override
    public void launch(final SlaveComputer computer, final TaskListener listener)
            throws IOException, InterruptedException {
        if (computer == null || !(computer instanceof LBComputer)) {
            return;
        }
        long launchStart = System.currentTimeMillis();
        // Let's do the launch in another thread
        Future<Boolean> connectionActivity = Computer.threadPoolForRemoting.submit(new Callable<Boolean>() {
            public Boolean call() {
                try {
                    launch((LBComputer) computer, listener);
                    return Boolean.TRUE;
                } catch (IOException e) {
                    LOG.severe("Slave " + computer.getDisplayName() + " launching failed! " + e.getMessage());
                    listener.getLogger().println("Slave " + computer.getDisplayName() + " launching failed! " + e.getMessage());
                } catch (InterruptedException e) {
                    LOG.severe("Slave " + computer.getDisplayName() + " launching interrupted! " + e.getMessage());
                    listener.getLogger().println("Slave " + computer.getDisplayName() + " launching interrupted! " + e.getMessage());
                }
                return Boolean.FALSE;
            }
        });
        // wait for computer to get online
        Boolean launchOk = null;
        listener.getLogger().println("Waiting for Slave " + computer.getDisplayName() + " to be launched...");
        while (!computer.isOnline()) {
            if (launchOk == null && connectionActivity.isDone()) {
                try {
                    launchOk = connectionActivity.get();
                    if (launchOk) {
                        listener.getLogger().println("Slave launched successfully. Waiting for Slave " + computer.getDisplayName() + " to become online...");
                    } else {
                        String message = "Slave " + computer.getDisplayName() + " launching failed!";
                        LOG.severe(message);
                        listener.getLogger().println(message);
                        throw new AbortException(message);
                    }
                } catch (ExecutionException e) {
                    String message = "Slave " + computer.getDisplayName() + " launching failed! " + e.getMessage();
                    LOG.severe(message);
                    listener.getLogger().println(message);
                    throw new AbortException(message);
                }
            }
            if (System.currentTimeMillis() - launchStart > SLAVE_LAUNCH_TIMEOUT) {
                String message = "Slave " + computer.getDisplayName() 
                    + " launching failed! Slave did not become online within " + SLAVE_LAUNCH_TIMEOUT / 60000 + " minutes";
                LOG.severe(message);
                listener.getLogger().println(message);
                throw new AbortException(message);
            }
            Thread.sleep(1000);
        }
        computer.setAcceptingTasks(true);
        LOG.info("Slave " + computer.getDisplayName() + " has been connected");
        listener.getLogger().println("Slave " + computer.getDisplayName() + " has been connected");
    }

    /**
     * Stage 2 of the launch.
     */
    protected abstract void launch(LBComputer computer,
                                        TaskListener listener)
            throws IOException,
            InterruptedException;

    @Override
    public void beforeDisconnect(SlaveComputer computer,
                                TaskListener listener) {
        super.beforeDisconnect(computer, listener);
        if (computer == null || !(computer instanceof LBComputer)) {
            return;
        }
        beforeDisconnect((LBComputer) computer, listener);
    }

    protected abstract void beforeDisconnect(LBComputer computer,
                                                  TaskListener listener);

    @Override
    public void afterDisconnect(SlaveComputer computer,
                               TaskListener listener) {
        super.afterDisconnect(computer, listener);
        if (computer == null || !(computer instanceof LBComputer)) {
            return;
        }
        LBComputer lbc = (LBComputer) computer;
        lbc.setAcceptingTasks(false); // just to be sure
        if (!lbc.workspaceCleanRequired()) {
            LOG.info("Slave " + lbc.getDisplayName()
                    + " workspace has already been cleaned");
            return;
        }
        try {
            LOG.info("Waiting for slave " + lbc.getDisplayName()
                    + " to go offline...");
            listener.getLogger().println("Waiting for slave " + lbc.getDisplayName()
                    + " to go offline...");
            computer.waitUntilOffline();
            LOG.info("Slave " + lbc.getDisplayName()
                    + " is offline");
            listener.getLogger().println("Slave " + lbc.getDisplayName()
                    + " is offline");
        } catch (InterruptedException e1) {
            LOG.log(Level.WARNING, "Interrupted while waiting slave " + lbc.getDisplayName()
                    + " to go offline");
            listener.getLogger().println("Interrupted while waiting slave " + lbc.getDisplayName()
                    + " to go offline");
        }
        afterDisconnect(lbc, listener);
    }

    protected abstract void afterDisconnect(LBComputer computer,
                                                 TaskListener listener);
}
