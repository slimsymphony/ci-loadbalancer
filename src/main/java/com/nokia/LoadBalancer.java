package com.nokia;

import com.jcraft.jsch.JSchException;
import com.nokia.ssh.LBConnectionSettings;
import com.nokia.ssh.SSHClient;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Label;
import hudson.model.labels.LabelAtom;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

//todo: use proper logging
public class LoadBalancer extends Cloud {

    private String url = "";
    private String sshPrivateKey = "";
    private String sshUsername = "";
    private static final Logger LOG = Logger.getLogger(LoadBalancer.class.getName());

    @DataBoundConstructor
    public LoadBalancer(String url,
                       String sshUsername,
                       String sshPrivateKey) {
        // change name
        super(url);
        setUrl(url);
        setSshUsername(sshUsername);
        setSshPrivateKey(sshPrivateKey);
        LOG.log(Level.INFO, "Microsoft LoadBalancer plugin instantiated. (" + url
                + ")");
        if (Jenkins.getInstance() != null) {
            Jenkins.getInstance().getLabelAtoms().add(new LabelAtom("s40"));
        }
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return (this.url);
    }

    public void setSshPrivateKey(String keyFile) {
        this.sshPrivateKey = keyFile;
    }

    public String getSshPrivateKey() {
        return (this.sshPrivateKey);
    }

    public void setSshUsername(String username) {
        this.sshUsername = username;
    }

    public String getSshUsername() {
        return (this.sshUsername);
    }

    @Override
    public Collection<PlannedNode> provision(Label label,
                                            int i) {
        LOG.log(Level.INFO,
                "provision(" + label + ", " + i + ")");
        Collection<PlannedNode> slaves = new ArrayList<PlannedNode>();
        try {
            LBCommunication communication = new LBCommunication();
            slaves = communication.provisionFromLoadBalancer(this, label, i);
        } catch (IOException e) {
            LOG.severe("Unable to provision slaves from Load Balancer server: "
                            + e.getMessage());
            e.printStackTrace();
        } catch (FormException e) {
            LOG.severe("Unable to provision slaves from Load Balancer server: "
                    + e.getMessage());
            e.printStackTrace();
        }
        return slaves;
    }

    @Override
    public boolean canProvision(Label label) {
        LOG.log(Level.FINE, "canProvision(" + label + ")");
        // return Communication.loadBalancerHasAvailableSlave(url, label);
        return (false);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {
        public String getDisplayName() {
            return ("LoadBalancer");
        }

        public FormValidation doTestServerConnection(@QueryParameter String url)
                throws IOException {
            return (LBCommunication.testServerConnection(url));
        }

        public FormValidation doTestSshConnection(@QueryParameter String url,
                                               @QueryParameter String sshUsername,
                                               @QueryParameter String sshPrivateKey)
                throws IOException {
            LBConnectionSettings connSettings = new LBConnectionSettings();
            connSettings.setHostname(new URL(Jenkins.getInstance().getRootUrl()).getHost());
            connSettings.setSshUsername(sshUsername);
            connSettings.setSshPrivateKey(sshPrivateKey);
            try {
                SSHClient.testCredentials(connSettings);
            } catch (JSchException je) {
                return (FormValidation.error(je.toString()));
            }
            return (FormValidation.ok("Ssh private key connection to " + connSettings.getHostname() + " succesfull"));
        }
    }
}