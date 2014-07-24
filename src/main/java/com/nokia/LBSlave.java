package com.nokia;

import hudson.Extension;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
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
import org.kohsuke.stapler.DataBoundConstructor;

import com.nokia.ssh.LBConnectionSettings;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LBSlave extends AbstractCloudSlave implements EphemeralNode {

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {
        public String getDisplayName() {
            return ("Microsoft Slave");
        }

        @Override
        public boolean isInstantiable() {
            return (false);
        }
    }
    public static final String WINDOWS_LABEL_PREFIX = "win";
    private URL loadbalancerURL;
    private LBConnectionSettings lBConnectionSettings;
    private String currentMaster = "";
    private String startScript = "";
    private String endScript = "";
    private boolean released = false;
    private String remoteWorkspaceDirectory;

    private static final Logger LOGGER = Logger.getLogger(LBSlave.class
            .getName());

    @DataBoundConstructor
    public LBSlave(String name,
                  String nodeDescription,
                  String remoteFS,
                  String numExecutors,
                  Mode mode,
                  String labelString,
                  ComputerLauncher launcher,
                  RetentionStrategy<LBComputer> retentionStrategy,
            List<? extends NodeProperty<?>> nodeProperties)
            throws FormException,
            IOException {
        super(name,
                nodeDescription,
                remoteFS,
                Util.tryParseNumber(numExecutors, 1).intValue(),
                mode,
                labelString,
                launcher,
                retentionStrategy,
                nodeProperties);
    }

    @Override
    protected synchronized void _terminate(TaskListener listener)
            throws IOException,
            InterruptedException {
        if (!released) {
            LBSlaveManager slaveManager = new LBSlaveManager();
            slaveManager.releaseSlave(this);
            released = true;
        }
    }

    @Override
    public void terminate() throws InterruptedException, IOException {
        LOGGER.info("Terminating slave: " + this.getDisplayName());
        super.terminate();
        LOGGER.info("Slave has been terminated: " + this.getDisplayName());
    }
    
    public Node asNode() {
        return (this);
    }

    @Override
    public AbstractCloudComputer<LBSlave> createComputer() {
        LOGGER.info("Creating new computer for node: " + this.getNodeName());
        return new LBComputer(this);
    }

    public LBConnectionSettings getConnectionSettings() {
        return (this.lBConnectionSettings);
    }

    /**
     * @return the currentMaster
     */
    public final String getCurrentMaster() {
        return (currentMaster);
    }

    public String getEndScript() {
        return endScript;
    }

    @Override
    public LBComputerLauncher getLauncher() {
        return ((LBComputerLauncher) super.getLauncher());
    }

    public URL getLoadbalancerURL() {
        return (loadbalancerURL);
    }

    public String getRemoteWorkspaceDirectory() {
        return remoteWorkspaceDirectory;
    }

    public String getStartScript() {
        return startScript;
    }

    public void setConnectionSettings(LBConnectionSettings lBConnectionSettings) {
        this.lBConnectionSettings = lBConnectionSettings;
    }

    public void setCurrentMaster(String currentMaster) {
        this.currentMaster = currentMaster;
    }

    public void setEndScript(String endScript) {
        this.endScript = endScript;
    }

    public void setLoadbalancerURL(URL loadbalancerURL) {
        this.loadbalancerURL = loadbalancerURL;
    }

    public void setRemoteWorkspaceDirectory(String remoteWorkspaceDirectory) {
        this.remoteWorkspaceDirectory = remoteWorkspaceDirectory;
    }

    public void setStartScript(String startScript) {
        this.startScript = startScript;
    }

    public LBSlaveInfo toSlaveInfo() {
        LBSlaveInfo si = new LBSlaveInfo();
        si.name = this.getNodeName();
        if (this.getConnectionSettings() != null) {
            si.host = this.getConnectionSettings().getHostname();
            si.port = this.getConnectionSettings().getPort();
        }
        si.executors = this.getNumExecutors();
        si.currentMaster = this.getCurrentMaster();
        si.startScript = this.getStartScript();
        si.endScript = this.getEndScript();
        si.workspace = this.getRemoteWorkspaceDirectory();
        return (si);
    }

}
