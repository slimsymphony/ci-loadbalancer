package com.nokia;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import hudson.model.Descriptor.FormException;
import hudson.model.Label;
import hudson.model.Node;
import hudson.slaves.NodeProvisioner.PlannedNode;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA. User: miikka Date: 12/1/11 Time: 2:14 PM To change
 * this template use File | Settings | File Templates.
 */
public class LBCommunication {

    private Gson gson = new Gson();

    private static final Logger LOG = Logger.getLogger(LBCommunication.class.getName());

    // todo: proper support for labels
    // todo: rethink the "api", this is currently pure s***. use some rest
    // library.
    public boolean loadBalancerHasAvailableSlave(String masterID,
                                                Label label)
            throws IOException {
        try {
            if (!masterID.endsWith("/")) {
                masterID += "/";
            }
            String master = Jenkins.getInstance().getRootUrl();
            masterID += "?type=available&labels=" + label + "&master=" + master;
            LOG.log(Level.INFO,
                    "master(" + masterID + "): checked for " + label + " slaves.");
            URL url = new URL(masterID);
            return (hasSlaves(url));
        } catch (IOException ex) {
            LOG.severe(ex.getMessage());
            throw (ex);
        }
    }

    public boolean releaseSlave(String loadbalancerURL,
                               String id)
            throws IOException {
        LOG.log(Level.INFO, "master(" + loadbalancerURL + "): released slave: "
                + id);
        try {
            if (!loadbalancerURL.endsWith("/")) {
                loadbalancerURL += "/";
            }
            loadbalancerURL += "?type=release&release_id=" + id;
            LOG.log(Level.INFO, "master(" + loadbalancerURL + "): released "
                    + id);
            URL url = new URL(loadbalancerURL);
            return (release(url));
        } catch (IOException ex) {
            LOG.severe(ex.getMessage());
            throw ex;
        }
    }

    // TODO: proper support for labels
    public Collection<PlannedNode> provisionFromLoadBalancer(LoadBalancer loadBalancer, Label label,
                                                            int numberOfExecutors)
            throws IOException, FormException {
        LOG.log(Level.INFO,
                "master(" + Jenkins.getInstance().getRootUrl() + "): requested for " + numberOfExecutors + " " + label + " slaves.");
        List<PlannedNode> slaves = new ArrayList<PlannedNode>();
        String parameterizedLbUrl = loadBalancer.getUrl();
        if (!parameterizedLbUrl.endsWith("/")) {
            parameterizedLbUrl += "/";
        }
        try {
            parameterizedLbUrl += "?type=provision&labels=" + label +
                            "&executors=" + numberOfExecutors +
                            "&master=" + Jenkins.getInstance().getRootUrl();
            List<LBSlave> lBSlaves = getSlaves(new URL(parameterizedLbUrl),
                    loadBalancer, label);
            for (LBSlave slave : lBSlaves) {
                PlannedNode node = getPlannedNode(slave);
                slaves.add(node);
            }
        } catch (IOException ex) {
            LOG.severe(ex.getMessage());
            throw (ex);
        } catch (FormException e) {
            LOG.severe(e.getMessage());
            throw (e);
        }
        return slaves;
    }

    private PlannedNode getPlannedNode(final LBSlave slave) {
        LOG.log(Level.INFO,
                "Getting planned node: " + slave.getDisplayName());
        return new PlannedNode(slave.getDisplayName(),
                            getFutureSlave(slave),
                            slave.getNumExecutors());
    }

    private Future<Node> getFutureSlave(final LBSlave slave) {
        LBSlaveManager slaveManager = new LBSlaveManager();
        return slaveManager.getFutureSlave(slave);
    }

    // todo: create generic method for all json handling / use a lib
    private List<LBSlave> getSlaves(URL url,
                                   LoadBalancer cloud,
                                   Label label) throws FormException, IOException {
        LOG.log(Level.FINEST, "Requesting slaves from " + url + " with label " + label);
        /** Initiate connection to load balancer **/
        LBSlaveManager slaveManager = new LBSlaveManager();
        List<LBSlave> slaves = new ArrayList<LBSlave>();
        HttpURLConnection conn = null;
        JsonReader reader = null;
        try {
            LOG.log(Level.FINEST, "Opening connection to " + url);
            conn = (HttpURLConnection)url.openConnection();
            conn.connect();
            conn.setConnectTimeout(120 * 1000);
            conn.setReadTimeout(120 * 1000);
            reader = new JsonReader(
                             new InputStreamReader(
                                      conn.getInputStream()));
            LOG.log(Level.FINEST, "Starting to read result array from " + url);
            reader.beginArray();

            /** Read jsonSlave contents **/
            while (reader.hasNext()) {
                /** Read contents from json to dummy jsonSlave **/
                LOG.log(Level.FINEST, "Reading content to json slave...");
                LBDummySlave jsonSlave = gson.fromJson(reader,
                                                  LBDummySlave.class);
                List<Node> jenkinsNodes = Jenkins.getInstance().getNodes();

                if (jsonSlave == null || jsonSlave.getName() == null || jsonSlave.getName().isEmpty()) {
                    LOG.log(Level.WARNING, "Slave construction failed! Received slave or slave name was null.");
                    continue;
                }
                // Check that slave and it's name are valid and unique
                if (hasSameNameSlave(jsonSlave, slaves) ||
                        hasSameNameNode(jsonSlave, jenkinsNodes)) {
                    LOG.log(Level.WARNING,
                            "Slave construction failed! Slave with name " + jsonSlave.getName() + " already exists");
                    continue;
                }

                /** Translate to-be-added jsonSlave from dummy jsonSlave **/
                LOG.log(Level.FINEST, "Creating slave from json slave...");
                LBSlave addedSlave = slaveManager.createSlave(jsonSlave, cloud, label);
                LOG.log(Level.FINEST, "Slave created successfully!");
                /** Add the jsonSlave **/
                // check that jsonSlave is valid and has valid name
                if (addedSlave != null && !StringUtils.isEmpty(addedSlave.getNodeName())) {
                    slaves.add(addedSlave);
                    LOG.log(Level.FINEST, "Added slave " + addedSlave.getNodeName() + " to list");
                } else {
                    LOG.log(Level.WARNING,
                            "Slave construction failed!");
                }
            }
        } catch (IOException e) {
            LOG.severe("Error while creating slave from dummy slave: "
                                + e.getMessage());
            throw e;
        } catch (FormException e) {
            LOG.severe("Error while creating slave from dummy slave: "
                    + e.getMessage());
            throw e;
        } finally {
            /** Close connection and clean up **/
            if (reader != null) {
                reader.endArray();
                reader.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return (slaves);
    }

    private boolean hasSameNameSlave(LBDummySlave current,
                                    List<LBSlave> slaves) {
        String name = current.getName();
        for (LBSlave s : slaves) {
            // Check if the name of this slave matches previously added slave
            if (name.equals(s.getNodeName())) {
                return (true);
            }
        }
        return (false);
    }

    private boolean hasSameNameNode(LBDummySlave current,
                                   List<Node> nodes) {
        String name = current.getName();
        for (Node n : nodes) {
            if (name.equals(n.getNodeName()))
            // The name of this slave matches previously added slave
            {
                return (true);
            }
        }
        return (false);
    }

    // todo: create generic method for all json handling / use a lib
    private boolean hasSlaves(URL url) throws IOException {
        Boolean hasSlaves = false;
        HttpURLConnection conn = null;
        JsonReader reader = null;
        try {
            conn = (HttpURLConnection)url.openConnection();
            conn.connect();
            conn.setConnectTimeout(120 * 1000);
            conn.setReadTimeout(120 * 1000);
            reader = new JsonReader(
                             new InputStreamReader(
                                     conn.getInputStream()));
            reader.beginArray();
            while (reader.hasNext()) {
                hasSlaves = gson.fromJson(reader, Boolean.class);
            }
        } catch (IOException e) {
            LOG.severe("Error while creating slave from dummy slave: "
                                + e.getMessage());
            throw e;
        } finally {
            /** Close connection and clean up **/
            if (reader != null) {
                reader.endArray();
                reader.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return hasSlaves;
    }

    // todo: create generic method for all json handling / use a lib
    private boolean release(URL url) throws IOException {

        Boolean bool = false;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)url.openConnection();
            conn.connect();
            conn.setConnectTimeout(120 * 1000);
            conn.setReadTimeout(120 * 1000);
            String response = conn.getResponseMessage();
            LOG.info("Response message received after releasing the slave: " + response);
        } catch (IOException e) {
            LOG.severe("I/O exception while releasing slave using URL " + url);
            e.printStackTrace();
            throw e;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return (bool);
    }

    public static FormValidation testServerConnection(String url) {
        HttpURLConnection connection = null;
        try {
            LOG.log(Level.INFO,
                    "master: testing LoadBalancer " + url);
            connection = (HttpURLConnection) new URL(url)
                    .openConnection();
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                return (FormValidation
                        .error("This LoadBalancer Requires Authentication! (server reply 403)"));
            }
            String v = connection.getHeaderField("X-LoadBalancer");
            if (v == null) {
                return (FormValidation
                        .error("This URL doesn't look like LoadBalancer."));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return (FormValidation.error(e, "Failed to connect to: " + url));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return (FormValidation.ok("Jenkins.. we have a lift off!"));
    }
}