package com.nokia;

/**
 * Created by IntelliJ IDEA. User: miikka Date: 12/9/11 Time: 9:52 AM To change
 * this template use File | Settings | File Templates.
 */
public class LBSlaveInfo {

    private boolean exit = false;
    private String sercretKey = "";
    public String name = "";
    public String host = "";
    public int port = 22;
    public int executors = 1;
    public String currentMaster = "";
    public String workspace = "";
    public String startScript = "";
    public String endScript = "";

    @Override
    public String toString() {
        return "SlaveInfo{" +
                "name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", port='" + port + '\'' +
                ", executors='" + executors + '\'' +
                ", workspace='" + workspace + '\'' +
                ", startScript='" + startScript + '\'' +
                ", endScript='" + endScript + '\'' +
                '}';
    }

    /**
     * @return the name
     */
    public final String getName() {
        return name;
    }

    /**
     * @return the host
     */
    public final String getHost() {
        return host;
    }

    /**
     * @return the port
     */
    public final int getPort() {
        return port;
    }

    /**
     * @return the executors
     */
    public final int getExecutors() {
        return executors;
    }

    /**
     * @return the currentMaster
     */
    public final String getCurrentMaster() {
        return currentMaster;
    }

    /**
     * @return the workspace
     */
    public final String getWorkspace() {
        return workspace;
    }

    /**
     * @return the startScript
     */
    public final String getStartScript() {
        return startScript;
    }

    /**
     * @return the endScript
     */
    public final String getEndScript() {
        return endScript;
    }

    /**
     * @param exit
     *            the exit to set
     */
    public void setExit(boolean exit) {
        this.exit = exit;
    }

    /**
     * @return the exit
     */
    public boolean isExit() {
        return exit;
    }

    /**
     * 
     * @return
     */
    public String getSercretKey() {
        return sercretKey;
    }

    /**
     * 
     * @param sercretKey
     */
    public void setSercretKey(String sercretKey) {
        this.sercretKey = sercretKey;
    }
}
