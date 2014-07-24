/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nokia.ssh;

import java.io.File;

/**
 * 
 * @author Miikka Andersson (miikka.1.andersson@nokia.com)
 */
public class LBConnectionSettings {
    private String hostname = "";
    private int port = 22;
    private String username = "";
    private String password = "";
    private String sshUsername = "";
    private String sshPrivateKey = "";

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return (this.username);
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public void setSshPrivateKey(String filePath) {
        this.sshPrivateKey = filePath;
    }

    public String getSshPrivateKey() {
        return (this.sshPrivateKey);
    }

    /**
     * Does this ConnectionSettings have authentication fields for public
     * key-private key authentication ?
     **/
    public boolean isKeyAuthentication() {
        try {
            return (!this.sshUsername.isEmpty() && new File(this.sshPrivateKey).canRead());
        } catch (SecurityException se) {
            return (false);
        } catch (NullPointerException npe) {
            return (false);
        }
    }

    /**
     * Does this ConnectionSettings have authentication fields for password
     * authentication ?
     **/
    public boolean isPasswordAuthentication() {
        return (!this.username.isEmpty() && !this.password.isEmpty());
    }

}