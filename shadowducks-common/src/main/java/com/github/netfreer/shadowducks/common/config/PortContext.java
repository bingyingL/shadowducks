package com.github.netfreer.shadowducks.common.config;

/**
 * @author: landy
 * @date: 2017-04-09 23:29
 */
public class PortContext {
    private final int port;
    private final String method;
    private final String password;

    public PortContext(int port, String method, String password) {
        this.port = port;
        this.method = method;
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public String getMethod() {
        return method;
    }

    public String getPassword() {
        return password;
    }
}
