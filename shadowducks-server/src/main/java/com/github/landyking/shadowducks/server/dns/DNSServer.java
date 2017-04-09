package com.github.landyking.shadowducks.server.dns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class DNSServer  {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public static final String dnsServer = "127.0.0.1";
    public static final int dnsPort = 10053;
    private FakeDNSContainer fakeDNSContainer;

    public FakeDNSContainer getFakeDNSContainer() {
        return fakeDNSContainer;
    }

    public void start() throws Exception {
        fakeDNSContainer = new FakeDNSContainer();
//        handler=new DNSHandler(fakeDNSContainer);
        logger.info("start dns server on port {}", dnsPort);
//        acceptor = new NioDatagramAcceptor();

        InetSocketAddress socketAddress = new InetSocketAddress(dnsPort);
//        acceptor.setDefaultLocalAddress(socketAddress);
//        acceptor.setHandler(handler);
//        acceptor.getSessionConfig().setReuseAddress(true);
//        acceptor.bind();
    }

    public void destroy() throws Exception {
        logger.info("destroy dns server on port {}", dnsPort);
//        acceptor.dispose();
    }
}
