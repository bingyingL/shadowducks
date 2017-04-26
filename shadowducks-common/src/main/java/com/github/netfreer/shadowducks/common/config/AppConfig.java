package com.github.netfreer.shadowducks.common.config;


import java.util.HashSet;
import java.util.Set;

/**
 * @author: landy
 * @date: 2017-04-09 22:52
 */
public class AppConfig {
    private String serverAddress;
    private String localAddress;
    private int localPort;
    private final Set<PortContext> ports = new HashSet<PortContext>();
    private int timeout;
    private PortContext destPort;
    private final boolean fastOpen = false;

    public AppConfig() {
    }

    public PortContext getDestPort() {
        return destPort;
    }

    public void setDestPort(PortContext destPort) {
        this.destPort = destPort;
    }

    public PortContext getPortContext(int port) {
        for (PortContext one : ports) {
            if (one.getPort() == port) {
                return one;
            }
        }
        return null;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public Set<PortContext> getPorts() {
        return ports;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isFastOpen() {
        return fastOpen;
    }
}
