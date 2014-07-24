/**
 * 
 */
package com.nokia;

import hudson.model.TaskListener;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.jcraft.jsch.JSchException;
import com.nokia.LBSlaveManager.LBSlaveException;
import com.nokia.ssh.LBConnectionSettings;
import com.nokia.ssh.SSHClient;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;

/**
 * @author ttyppo
 * 
 */
public class LBUnixComputerLauncher
        extends LBComputerLauncher {

    private static final Logger LOG = Logger
            .getLogger(LBUnixComputerLauncher.class.getName());

    /**
     * @return the log
     */
    public static final Logger getLog() {
        return LOG;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.nokia.NokiaComputerLauncher#launch(com.nokia.NokiaComputer,
     * java.io.PrintStream, hudson.model.TaskListener)
     */
    @Override
    protected void launch(LBComputer computer,
                         TaskListener listener)
            throws IOException, InterruptedException {
        LBSlaveInfo slaveInfo = computer.getSlaveInfo();
        LBConnectionSettings connectionSettings = computer.getConnectionSettings();
        String command = "/bin/bash -x "
                + slaveInfo.getStartScript() + " "
                     + slaveInfo.getCurrentMaster() + " "
                             + slaveInfo.getWorkspace() + " "
                             + slaveInfo.getName() + " "
                             + computer.getJnlpMac() + " > /dev/null 2>&1 &";
        SSHClient sshclient = new SSHClient();
        LOG.log(Level.INFO,
                "connection settings: "
                        + connectionSettings.getHostname()
                        + ": " + connectionSettings.getPort()
                        + ", "
                        + connectionSettings.getSshUsername());
        listener.getLogger().println("connection settings: "
                + connectionSettings.getHostname()
                + ": " + connectionSettings.getPort()
                + ", "
                + connectionSettings.getSshUsername());
        LOG.log(Level.INFO, "exec command: " + command);
        listener.getLogger().println("exec command: " + command);
        /** Process connectionSettings for the shell command **/
        try {
            sshclient.execCommand(command,
                             connectionSettings,
                             true);
        } catch (IOException e) {
            LOG.severe(e.getMessage());
            throw new IOException("Error while launching SSH Unix slave "
                    + computer.getDisplayName(), e);
        } catch (JSchException e) {
            LOG.severe(e.getMessage());
            throw new IOException("Error while launching SSH Unix slave "
                    + computer.getDisplayName(), e);
        } catch (InterruptedException e) {
            LOG.severe(e.getMessage());
            throw e;
        }
    }

    @Override
    protected void beforeDisconnect(LBComputer computer,
                                   TaskListener listener) {

    }

    @Override
    protected void afterDisconnect(LBComputer computer, TaskListener listener) {
        LBSlaveInfo slaveInfo = computer.getSlaveInfo();
        LBConnectionSettings connectionSettings = computer.getConnectionSettings();
        if (StringUtils.isNotEmpty(slaveInfo.getEndScript())) {
            try {
                LOG.info("Cleaning workspace on slave machine "
                        + computer.getDisplayName() + "...");
                listener.getLogger().println("Cleaning workspace on slave machine "
                        + computer.getDisplayName() + "...");

                String command = "/bin/bash -x "
                        + slaveInfo.getEndScript() + " "
                        + slaveInfo.getCurrentMaster() + " "
                        + slaveInfo.getWorkspace() + " "
                        + slaveInfo.getName() + " > /dev/null 2>&1 &";

                SSHClient sshclient = new SSHClient();
                LOG.log(Level.INFO,
                        "connection settings: "
                                + connectionSettings.getHostname()
                                + ": " + connectionSettings.getPort()
                                + ", "
                                + connectionSettings.getSshUsername());
                listener.getLogger().println("connection settings: "
                        + connectionSettings.getHostname()
                        + ": " + connectionSettings.getPort()
                        + ", "
                        + connectionSettings.getSshUsername());
                LOG.log(Level.INFO, "exec command: " + command);
                listener.getLogger().println("exec command: " + command);
                /** Process connectionSettings for the shell command **/
                sshclient.execCommand(command,
                             connectionSettings,
                             true);
            } catch (IOException e) {
                LOG.severe("Error while executing SSH command to run end script on Unix slave "
                        + computer.getDisplayName() + ":" + e.getMessage());
                listener.getLogger().println("Error while executing SSH command to run end script on Unix slave "
                        + computer.getDisplayName() + ":" + e.getMessage());
                e.printStackTrace();
            } catch (JSchException e) {
                LOG.severe("Error while executing SSH command to run end script on Unix slave "
                        + computer.getDisplayName() + ": " + e.getMessage());
                listener.getLogger().println("Error while executing SSH command to run end script on Unix slave "
                        + computer.getDisplayName() + ": " + e.getMessage());
                e.printStackTrace();
            } catch (InterruptedException e) {
                LOG.severe("Interrupted while executing SSH command to run end script on Unix slave "
                        + computer.getDisplayName() + ": " + e.getMessage());
                listener.getLogger().println("Interrupted while executing SSH command to run end script on Unix slave "
                        + computer.getDisplayName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
