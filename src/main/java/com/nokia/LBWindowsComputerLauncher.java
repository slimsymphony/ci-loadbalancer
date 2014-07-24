/**
 * 
 */
package com.nokia;

import hudson.model.TaskListener;
import hudson.slaves.SlaveComputer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.nokia.LBSlaveManager.LBSlaveException;
import com.nokia.ssh.SSHClient;

/**
 * @author ttyppo
 * 
 */
public class LBWindowsComputerLauncher extends LBComputerLauncher {

    private static final Logger LOG = Logger
            .getLogger(LBWindowsComputerLauncher.class.getName());

    private Gson gson = new Gson();

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
            TaskListener listener) throws IOException, InterruptedException {

        LOG.info("Launching slave.jar on windows slave machine "
                + computer.getDisplayName() + "...");
        listener.getLogger().println("Launching slave.jar on windows slave machine "
                + computer.getDisplayName() + "...");
        LBSlaveInfo slave = computer.getSlaveInfo();
        slave.setExit(false);
        slave.setSercretKey(computer.getJnlpMac());
        writeSlave(slave);
    }

    @Override
    protected void beforeDisconnect(LBComputer computer, TaskListener listener) {

    }

    @Override
    protected void afterDisconnect(LBComputer computer, TaskListener listener) {
        LOG.info("Cleaning workspace on windows slave machine "
                + computer.getDisplayName() + "...");
        listener.getLogger().println("Cleaning workspace on windows slave machine "
                + computer.getDisplayName() + "...");
        LBSlaveInfo slave = computer.getSlaveInfo();
        slave.setExit(true);
        slave.setSercretKey(computer.getJnlpMac());
        try {
            writeSlave(slave);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            LOG.log(Level.WARNING, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            LOG.log(Level.WARNING, e.getMessage());
        }
    }

    private void writeSlave(LBSlaveInfo slave) throws UnknownHostException, IOException  {
        Socket client = null;
        JsonWriter writer = null;
        try {
            client = new Socket(slave.getHost(), slave.getPort());
            // PrintWriter out = new PrintWriter(client.getOutputStream(), true);
            // BufferedReader in = new BufferedReader(new
            // InputStreamReader(client.getInputStream()));
            writer = new JsonWriter(new OutputStreamWriter(
                    client.getOutputStream()));
            writer.beginArray();
            gson.toJson(slave, LBSlaveInfo.class, writer);
            writer.endArray();
            writer.close();
        } catch (UnknownHostException ex) {
            LOG.severe(ex.getMessage());
            throw ex;
        } catch (IOException e) {
            LOG.severe(e.getMessage());
            throw e;
        } finally {
            if (writer != null) {
                writer.close();
            }
            if (client != null) {
                client.close();
            }
        }
    }
}
