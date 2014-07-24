package com.nokia;

import hudson.Extension;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Node.Mode;
import hudson.model.Slave.SlaveDescriptor;
import hudson.model.TaskListener;
import hudson.model.Descriptor.FormException;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.EphemeralNode;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import com.nokia.ssh.LBConnectionSettings;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LBSlaveManager {

    private static final Logger LOGGER = Logger.getLogger(LBSlaveManager.class
            .getName());

    private static final int IDLE_TIMEOUT_MINUTES = 1;

    public void releaseSlave(final LBSlave slave) {
        Computer.threadPoolForRemoting.submit(new Runnable(){
            @Override
            public void run() {
                final String nodeName = slave.getNodeName();
                LOGGER.info("Returning slave " + nodeName + " back to the slave pool...");
                try {
                    LBCommunication communication = new LBCommunication();
                    communication.releaseSlave(slave.getLoadbalancerURL().toString(),
                                                  nodeName);
                    LOGGER.info("Successfully released slave " + nodeName);
                } catch (IOException e) {
                    LOGGER.warning("Unable to release slave " + nodeName + ": " + e.getMessage());
                }
            }});
    }

    public Future<Node> getFutureSlave(final LBSlave slave) {
        Future<Node> futureSlave = Computer
                        .threadPoolForRemoting
                                .submit(
                                new Callable<Node>() {
                                    public Node call() throws Exception {
                                        Jenkins jenkins = Jenkins.getInstance();
                                        LOGGER.log(Level.INFO,
                                                "Adding node: " + slave.getDisplayName());
                                        jenkins.addNode(slave);
                                        return slave;
                                    }
                                });
        return futureSlave;
    }

    public LBSlave createSlave(LBDummySlave dummySlave, LoadBalancer loadBalancer, Label label)
            throws IOException, FormException {
        LOGGER.finest("Creating slave from " + dummySlave.getHost() + ": " + dummySlave.getPort());
        try {
            String labelStr = (label == null) ? "" : label.toString();
            String nodeDescription = "Microsoft slave from " + dummySlave.getHost() + ": " + dummySlave.getPort();

            String remoteFS = dummySlave.getWorkspace();
            ComputerLauncher launcher;
            if (labelStr.toLowerCase().startsWith(LBSlave.WINDOWS_LABEL_PREFIX)) {
                LOGGER.finest("Creating windows computer launcher");
                launcher = new LBWindowsComputerLauncher();
            } else {
                LOGGER.finest("Creating unix computer launcher");
                remoteFS += dummySlave.getName();
                launcher = new LBUnixComputerLauncher();
            }
            LOGGER.finest("Computer launcher created successfully");

            Mode mode;
            if (labelStr.isEmpty()) {
                mode = Mode.NORMAL;
            } else {
                mode = Mode.EXCLUSIVE;
            }

            LOGGER.finest("Creating slave retention strategy");
            RetentionStrategy<LBComputer> retentionStrategy = new LBSlaveRetentionStrategy(IDLE_TIMEOUT_MINUTES);
            LOGGER.finest("Retention strategy created successfully");

            String executors = String.valueOf(dummySlave.getExecutors());

            List<NodeProperty<?>> nodeProperties = Collections.<NodeProperty<?>> emptyList();

            LOGGER.finest("Creating LBSlave instance: " + dummySlave.getHost() + ": " + dummySlave.getPort());
            LBSlave slave = new LBSlave(dummySlave.getName(), nodeDescription, remoteFS, executors, mode,
                           labelStr, launcher, retentionStrategy, nodeProperties);

            LOGGER.finest("Creating connection settings: " + dummySlave.getHost() + ": " + dummySlave.getPort());
            LBConnectionSettings lBConnectionSettings = new LBConnectionSettings();
            lBConnectionSettings.setHostname(dummySlave.getHost());
            lBConnectionSettings.setPort(dummySlave.getPort());
            lBConnectionSettings.setPassword(dummySlave.getPassword());
            lBConnectionSettings.setUsername(dummySlave.getUsername());
            lBConnectionSettings.setSshPrivateKey(loadBalancer.getSshPrivateKey());
            lBConnectionSettings.setSshUsername(loadBalancer.getSshUsername());
            slave.setConnectionSettings(lBConnectionSettings);

            LOGGER.finest("Setting LB properties for slave " + dummySlave.getHost() + ": " + dummySlave.getPort());
            URL loadBalancerURL = new URL(loadBalancer.getUrl());
            slave.setLoadbalancerURL(loadBalancerURL);
            slave.setCurrentMaster(dummySlave.getCurrentMaster());
            slave.setStartScript(dummySlave.getStartScript());
            slave.setEndScript(dummySlave.getEndScript());
            slave.setRemoteWorkspaceDirectory(dummySlave.getWorkspace());

            LOGGER.finest("Successfully created slave " + dummySlave.getHost() + ": " + dummySlave.getPort());
            return slave;
        } catch (IOException e) {
            LOGGER.severe("Error while creating slave from dummy slave: "
                    + e.getMessage());
            throw (e);
        } catch (FormException e) {
            LOGGER.severe("Error while creating slave from dummy slave: "
                    + e.getMessage());
            throw (e);
        }
    }

    public class LBSlaveException extends Exception {
        public LBSlaveException(String string) {
            super(string);
        }
    }
}
