package com.github.landyking.shadowducks.server.pojo;

/**
 * @author: landy
 * @date: 2016/05/02 10:35
 * note:
 */
public class SSHead
{
    private final int type;
    private final String host;
    private final int port;

    public SSHead(int type, String host, int port) {
        this.type = type;
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SSHead ssHead = (SSHead) o;

        if (port != ssHead.port) return false;
        return host.equals(ssHead.host);

    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + port;
        return result;
    }
}
