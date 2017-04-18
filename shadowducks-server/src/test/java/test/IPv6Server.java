package test;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.*;

/**
 * @author: landy
 * @date: 2017-04-18 21:40
 */
public class IPv6Server {
    public static void main(String[] args) throws InterruptedException, UnknownHostException {
        InetAddress localHost = Inet6Address.getLocalHost();
        System.out.println(localHost.getCanonicalHostName());
        System.out.println(localHost.getHostAddress());
        System.out.println(localHost.getHostName());
        new ServerBootstrap()
                .group(new NioEventLoopGroup(), new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                super.channelActive(ctx);
                                InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
                                System.out.println(address);
                                InetAddress inetAddress = address.getAddress();
                                System.out.println(address.isUnresolved());
                                System.out.println(inetAddress.getHostName());
                                System.out.println(inetAddress.getHostAddress());
                                System.out.println(inetAddress.getCanonicalHostName());
                                if (inetAddress instanceof Inet6Address) {
                                    System.out.println("IPV6#######");
                                }else{
                                    System.out.println("IPV4$$$$$$$$$");
                                }
                            }

                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                            }
                        });
                    }
                }).bind(/*localHost, */1890)
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            System.out.println("bind success");
                        } else {
                            System.out.println("bind failure!");
                            future.cause().printStackTrace();
                        }
                    }
                })
                .sync().channel().closeFuture().sync();
    }
}
