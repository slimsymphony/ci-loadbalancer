/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nokia.ssh;

import com.jcraft.jsch.*;
import com.nokia.LBSlaveManager;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Miikka Andersson (miikka.1.andersson@nokia.com)
 */
public class SSHClient {

    // Status codes
    public final static int STATUS_SUCCESS = 0;
    public final static int STATUS_INVALID_HOST = 1;
    public final static int STATUS_INVALID_PASS = 2;
    public final static int STATUS_UNKNOWN_ERROR = 99;

    // Defaults
    public final static int DEFAULT_PORT = 22;

    private final static int BUFFER = 1024;
    private static final Logger LOGGER = Logger.getLogger(SSHClient.class.getName());
    private PrintWriter writer = null;
    private StringBuilder output = new StringBuilder();
    private JSch client = new JSch();

    public int execCommand(String command,
                          LBConnectionSettings settings,
                          boolean showOutput)
            throws JSchException,
             IOException, InterruptedException {
        int ret = 0;

        // Clear output from previous execution
        output = new StringBuilder();

        Session session = null;
        Channel channel = null;
        try {
            // Check private key authentication
            if (settings.isKeyAuthentication())
            {
                // Set host name
                JSch.setConfig("StrictHostKeyChecking", "no");
                client.setKnownHosts(settings.getHostname());
                // Set private key
                client.addIdentity(settings.getSshPrivateKey());
                // Session connection data
                session = client.getSession(settings.getSshUsername(),
                                           settings.getHostname(),
                                           settings.getPort());
            } else
            {
                // Fall back to traditional authentication
                session = getSession(settings);
            }
            session.connect();
            if (showOutput &&
                    writer != null) {
                writer.println("Executing command: " + command);
            }
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            InputStream in = channel.getInputStream();

            channel.connect();
            handleInputStream(in,
                               channel,
                               showOutput);
            ret = channel.getExitStatus();
        } catch (JSchException ex) {
            LOGGER.log(Level.SEVERE, "", ex);
            throw ex;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "", e);
            throw e;
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "", e);
            throw e;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
        
        return (ret);
    }

    /**
     * Returns the console output of previous command execution
     * 
     * @return String output
     */
    public String getOutput() {
        return output.toString();
    }

    public static boolean testCredentials(final LBConnectionSettings connSettings)
            throws JSchException {
        Session session = null;

        JSch.setConfig("StrictHostKeyChecking", "no");
        JSch jsch = new JSch();
        ChannelExec channel = null;
        try {
            jsch.setKnownHosts(connSettings.getHostname());
            jsch.addIdentity(connSettings.getSshPrivateKey());
            session = jsch.getSession(connSettings.getSshUsername(),
                                   connSettings.getHostname());
            session.connect();
            channel = (ChannelExec) session.openChannel("exec");
            // channel.setEnv("key","value");
            channel.setInputStream(null);
            // InputStream in=channel.getInputStream();
            channel.connect();
        } catch (JSchException je) {
            throw (je);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
        return (true);
    }

    public void setPrintWriter(PrintWriter writer) {
        this.writer = writer;
    }

    private void handleInputStream(InputStream in,
                                    Channel channel,
                                    boolean displayOutput) throws IOException, InterruptedException {
        byte[] tmp = new byte[BUFFER];
        while (true) {
            try {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, BUFFER);
                    if (i < 0) {
                        break;
                    }
                    if (writer != null && displayOutput) {
                        writer.print(new String(tmp, 0, i));
                    }
                    output.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    break;
                }
                Thread.sleep(1000);
            } catch (InterruptedException ee) {
                LOGGER.log(Level.SEVERE, "", ee);
                throw ee;
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "", ex);
                throw ex;
            }
        }
    }

    public static int getStatus(JSchException ex) {
        int status;
        if (ex.getMessage().toLowerCase().contains("unknownhost")) {
            status = STATUS_INVALID_HOST;
        } else if (ex.getMessage().toLowerCase().contains("auth fail")) {
            status = STATUS_INVALID_PASS;
        } else {
            status = STATUS_UNKNOWN_ERROR;
        }
        return (status);
    }

    private Session getSession(final LBConnectionSettings settings)
            throws JSchException {
        JSch client = new JSch();
        Session session = null;
        session = client.getSession(settings.getUsername(),
                                   settings.getHostname(),
                                   settings.getPort());

        UserInfo userInfo = new UserInfo() {
            public String getPassphrase() {
                return (null);
            }

            public String getPassword() {
                return (settings.getPassword());
            }

            public boolean promptPassword(String prompt) {
                return (true);
            }

            public boolean promptPassphrase(String prompt) {
                return (false);
            }

            public boolean promptYesNo(String prompt) {
                return (false);
            }

            public void showMessage(String string) {
                return;
            }
        };
        session.setUserInfo(userInfo);

        // Let's ignore key checking as we are working in intranet
        session.setConfig("StrictHostKeyChecking", "no");
        return (session);
    }
}
