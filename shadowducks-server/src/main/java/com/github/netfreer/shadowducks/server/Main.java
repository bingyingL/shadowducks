package com.github.netfreer.shadowducks.server;

import com.github.netfreer.shadowducks.common.config.AppConfig;
import com.github.netfreer.shadowducks.common.config.ConfigUtil;
import com.github.netfreer.shadowducks.common.config.PortContext;
import com.github.netfreer.shadowducks.common.handler.TcpSecurityHandler;
import com.github.netfreer.shadowducks.common.handler.UdpSecurityHandler;
import com.github.netfreer.shadowducks.server.handler.ShadowSocksServerHandler;
import com.github.netfreer.shadowducks.server.handler.UdpForwardHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @author: landy
 * @date: 2017-04-09 22:47
 */
public class Main {
    private final Logger log = LoggerFactory.getLogger(getClass());


    public static void main(String[] args) {
        AppConfig config = ConfigUtil.loadConfig(args);
        config.setServerAddress("0.0.0.0");
        config.setTimeout(30*1000);
        config.getPorts().add(new PortContext(2999, "aes-256-cfb", "password"));
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
                    .childOption(ChannelOption.AUTO_READ, false)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            PortContext portContext = config.getPortContext(ch.localAddress().getPort());
                            ch.pipeline().addLast(new TcpSecurityHandler(portContext));
                            ch.pipeline().addLast(new ShadowSocksServerHandler(config));
                        }
                    });
            for (PortContext tuple : config.getPorts()) {
                try {
                    Channel channel = tcpBootstrap.bind(config.getServerAddress(), tuple.getPort()).sync().channel();
                    log.info("Start listen tcp port {} on address {}, method: {}, password: {}.", config.getServerAddress(),
                            tuple.getPort(), tuple.getMethod(), tuple.getPassword());
                    initChannelAttribute(tuple, channel);
                } catch (Exception e) {
                    throw new IllegalStateException("can't bind tcp port " + tuple.getPort() + " on address " + config.getServerAddress(), e);
                }
            }
            Bootstrap udpBootstrap = new Bootstrap().group(work)
                    .channel(NioDatagramChannel.class)
                    .handler(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelActive(ChannelHandlerContext ctx) throws Exception {
                            Channel ch = ctx.channel();
                            PortContext portContext = config.getPortContext(((InetSocketAddress) ch.localAddress()).getPort());
                            ch.pipeline().addLast(new UdpSecurityHandler(portContext));
//                            ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                            ch.pipeline().addLast(new UdpForwardHandler());
                            ch.pipeline().remove(this);
                            ctx.fireChannelActive();
                        }
                    });

            for (PortContext tuple : config.getPorts()) {
                try {
                    Channel channel = udpBootstrap.bind(config.getServerAddress(), tuple.getPort()).sync().channel();
                    log.info("Start listen udp port {} on address {}, method: {}, password: {}.", config.getServerAddress(),
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
