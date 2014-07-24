package com.nokia;

/**
 * Created by IntelliJ IDEA. User: miikka Date: 12/9/11 Time: 9:52 AM To change
 * this template use File | Settings | File Templates.
 */
public class LBDummySlave {

    private String name = "";
    private String username = "";
    private String password = "";
    private String host = "";
    private int port = 22;
    private int executors = 1;
    private String currentMaster = "ousim003.europe.nokia.com:9898";
    public String workspace = "";
    public String startScript = "";
    public String endScript = "";

    @Override
    public String toString() {
        return "DummySlave{" +
                "name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
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
     * @return the username
     */
    public final String getUsername() {
        return username;
    }

    /**
     * @return the password
     */
    public final String getPassword() {
        return password;
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

}
