package com.github.netfreer.shadowducks.client;

import com.github.netfreer.shadowducks.common.utils.AttrKeys;
import com.github.netfreer.shadowducks.client.handler.SocksServerHandler;
import com.github.netfreer.shadowducks.common.config.AppConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: landy
 * @date: 2017-04-26 22:28
 */
public class DucksClient {
    private final Logger log = LoggerFactory.getLogger(DucksClient.class);

    public void start(final AppConfig config) {
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
//                    .childOption(ChannelOption.AUTO_READ, false)
//                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(512 * 1024, 1024 * 1024))
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
//                            PortContext portContext = config.getPortContext(ch.localAddress().getPort());
//                            ch.pipeline().addLast(DucksFactory.getChannelHandler(portContext, true));
                            ch.attr(AttrKeys.CHANNEL_BEGIN_TIME).set(System.currentTimeMillis());
                            ch.pipeline().addLast(new SocksPortUnificationServerHandler(),
                                    SocksServerHandler.INSTANCE);
                        }
                    });
            tcpBootstrap.bind(config.getLocalAddress(), config.getLocalPort()).sync().channel();
            log.info("Start listen tcp port {} on address {}", config.getServerAddress(), config.getLocalPort());
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
}
