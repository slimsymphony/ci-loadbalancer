/**
 * 
 */
package com.nokia;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;

import com.nokia.ssh.LBConnectionSettings;

import hudson.model.Node;
import hudson.slaves.AbstractCloudComputer;

/**
 * @author ttyppo
 * 
 */
public class LBComputer extends AbstractCloudComputer<LBSlave> {

    private static final Logger LOGGER = Logger.getLogger(LBComputer.class
            .getName());
    private LBConnectionSettings connectionSettings;
    private LBSlaveInfo slaveInfo;
    private boolean workspaceCleanStarted = false;

    /**
     * @param slave
     */
    public LBComputer(LBSlave slave) {
        super(slave);
        this.slaveInfo = slave.toSlaveInfo();
        this.connectionSettings = slave.getConnectionSettings();
    }

    @Override
    protected LBComputerLauncher grabLauncher(Node node) {
        return ((LBSlave) node).getLauncher();
    }

    @Override
    protected Future<?> _connect(boolean forceReconnect) {
        LOGGER.log(Level.INFO,
                "Trying to connect: " + this.getDisplayName());
        Future<?> connectingActivity = super._connect(false);
        LOGGER.log(Level.INFO,
                    "Received connecting activity: " + this.getDisplayName());
        return connectingActivity;
    }

    @Override
    protected void setNode(Node node) {
        if (this.nodeName == null) {
            super.setNode(node);
        }
    }

    /**
     * When the slave is deleted, free the node.
     */
    @Override
    public HttpResponse doDoDelete() throws IOException {
        checkPermission(DELETE);
        try {
            if (getNode() != null) {
                getNode().terminate();
            } else {
                doDoDisconnect("Computer was deleted and node was null. Disconnecting the computer");
            }
            return new HttpRedirect("..");
        } catch (InterruptedException e) {
            return HttpResponses.error(500, e);
        } catch (ServletException e) {
            return HttpResponses.error(500, e);
        }
    }
    
    public LBConnectionSettings getConnectionSettings() {
        return connectionSettings;
    }

    public LBSlaveInfo getSlaveInfo() {
        return slaveInfo;
    }
    
    public synchronized boolean workspaceCleanRequired() {
        if (!workspaceCleanStarted) {
            workspaceCleanStarted = true;
            return true;
        }
        return false;
    }

}
