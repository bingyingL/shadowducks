package com.github.landyking.shadowducks.server;

import com.github.landyking.shadowducks.common.config.AppConfig;
import com.github.landyking.shadowducks.common.config.ConfigUtil;
import com.github.landyking.shadowducks.common.config.PortContext;
import com.github.landyking.shadowducks.common.handler.TcpSecurityHandler;
import com.github.landyking.shadowducks.common.handler.UdpSecurityHandler;
import com.github.landyking.shadowducks.server.handler.SSHeadDecoder;
import com.github.landyking.shadowducks.server.handler.TcpForwardHandler;
import com.github.landyking.shadowducks.server.handler.UdpForwardHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: landy
 * @date: 2017-04-09 22:47
 */
public class Main {
    private final Logger log = LoggerFactory.getLogger(getClass());


    public static void main(String[] args) {
        AppConfig config = ConfigUtil.loadConfig(args);
        config.setServerAddress("0.0.0.0");
        config.setTimeout(300);
        config.getPorts().add(new PortContext(1999, "aes-256-cfb", "password"));
        new Main().start(config);
    }

    private void start(final AppConfig config) {
        if (config.getPorts().isEmpty()) {
            log.error("Not config any port !");
            System.exit(2);
        }
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup work = new NioEventLoopGroup();
        try {
            ServerBootstrap tcpBootstrap = new ServerBootstrap().group(boss, work)
                    .channel(NioServerSocketChannel.class)
//                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            PortContext portContext = config.getPortContext(ch.localAddress().getPort());
                            ch.pipeline().addLast(new TcpSecurityHandler(portContext));
                            ch.pipeline().addLast(new SSHeadDecoder(null));
                            ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                            ch.pipeline().addLast(new TcpForwardHandler());
                        }
                    });
            for (PortContext tuple : config.getPorts()) {
                try {
                    Channel channel = tcpBootstrap.bind(config.getServerAddress(), tuple.getPort()).sync().channel();
                    log.info("Start listen tcp port {} on address {} , method:{} , password:{} .", config.getServerAddress(),
                            tuple.getPort(), tuple.getMethod(), tuple.getPassword());
                    initChannelAttribute(tuple, channel);
                } catch (Exception e) {
                    throw new IllegalStateException("can't bind tcp port " + tuple.getPort() + " on address " + config.getServerAddress(), e);
                }
            }
            Bootstrap udpBootstrap = new Bootstrap().group(work)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION,true)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        protected void initChannel(NioDatagramChannel ch) throws Exception {
                            log.info("#################");
//                            PortContext portContext = config.getPortContext(ch.localAddress().getPort());
//                            ch.pipeline().addLast(new UdpSecurityHandler(portContext));
//                            ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
//                            ch.pipeline().addLast(new UdpForwardHandler());
                        }
                    });

            for (PortContext tuple : config.getPorts()) {
                try {
                    Channel channel = udpBootstrap.bind(config.getServerAddress(), tuple.getPort()).sync().channel();
                    log.info("Start listen udp port {} on address {} , method:{} , password:{} .", config.getServerAddress(),
                            tuple.getPort(), tuple.getMethod(), tuple.getPassword());
                    initChannelAttribute(tuple, channel);
                } catch (Exception e) {
                    throw new IllegalStateException("can't bind udp port " + tuple.getPort() + " on address " + config.getServerAddress(), e);
                }
            }
            log.info("start server success !");
            boss.terminationFuture().sync();
        } catch (Exception e) {
            log.error("start server failure !", e);
        } finally {
            log.info("shutdown server");
            boss.shutdownGracefully();
            work.shutdownGracefully();
        }
    }

    private void initChannelAttribute(PortContext tuple, Channel channel) {
    }
}
