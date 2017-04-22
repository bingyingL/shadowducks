package test;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.util.ReferenceCountUtil;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author: landy
 * @date: 2017-4-22 14:59
 */
public class WritableChangeTest {
    public static void main(String[] args) throws InterruptedException {
        new WritableChangeTest().start();
    }

    private int destServerPort = 23456;
    private int forwardPort = 12345;

    private void start() throws InterruptedException {
        startLimitDestServer(100);
        startForwardStation();
        startSendClient();
    }

    private void startForwardStation() throws InterruptedException {
        new ServerBootstrap().group(new NioEventLoopGroup(1), new NioEventLoopGroup(1))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            Channel destChannel;
                            Channel originalChannel;

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                originalChannel = ctx.channel();
                                ChannelFuture connectFuture = new Bootstrap()
                                        .group(ctx.channel().eventLoop())
                                        .channel(NioSocketChannel.class)
                                        .handler(new ChannelInboundHandlerAdapter() {
                                            @Override
                                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                                if (destChannel.unsafe().outboundBuffer().totalPendingWriteBytes() > 0) {
                                                    destChannel.flush();
                                                }
                                            }

                                            @Override
                                            public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
                                                if (ctx.channel().isWritable()) {
                                                    System.out.println(">>>forward station : set auto read true");
                                                    originalChannel.config().setAutoRead(true);
                                                } else {
                                                    System.out.println("!!!forward station : set auto read false");
                                                    originalChannel.config().setAutoRead(false);
                                                }
                                            }
                                        }).connect("127.0.0.1", destServerPort);
                                connectFuture.addListener(new ChannelFutureListener() {
                                    @Override
                                    public void operationComplete(ChannelFuture future) throws Exception {
                                        if (future.isSuccess()) {
                                            System.out.println("%%% forward station connect dest server success");
                                        } else {
                                            System.out.println("### forward station connect dest server failure: " + future.cause().getMessage());
                                            future.channel().close();
                                        }
                                    }
                                });
                                destChannel = connectFuture.channel();
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                if (destChannel != null) {
                                    destChannel.write(msg);
                                } else {
                                    System.out.println("!!!!dest channel is null !!!!!!");
                                }
                            }

                            int count = 0;

                            @Override
                            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                if (destChannel != null) {
                                    if (destChannel.isActive()) {
                                        destChannel.flush();
                                        System.out.print("@");
                                        count++;
                                        if (count % 50 == 0) {
                                            System.out.println();
                                        }
                                    }
                                } else {
                                    System.out.println("!!!!dest channel is null !!!!!!");
                                }
                            }
                        });
                    }
                }).bind(forwardPort).await();
        System.out.println("forward station start success!");
    }

    private void startSendClient() throws InterruptedException {
        new Bootstrap()
                .group(new NioEventLoopGroup(1))
                .channel(NioSocketChannel.class)
                .handler(new ChannelInboundHandlerAdapter() {
                    public Channel channel;

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        channel = ctx.channel();
                        writeData();
                    }

                    @Override
                    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
                        if (ctx.channel().isWritable()) {
                            writeData();
                        } else {

                        }
                    }

                    private long totalData = 0;

                    private void writeData() {
                        System.out.println("send client begin send data");
                        while (channel.isWritable()) {
                            int dataSize = 100 * 1024;
                            ByteBuf msg = channel.alloc().ioBuffer(dataSize);
                            for (int i=0;i<dataSize;i++) {
                                msg.writeByte(i);
                            }
                            channel.write(msg);
                            totalData += dataSize;
                        }
                        channel.flush();
                        System.out.println("send client stop write data, total send: " + totalData);
                    }
                }).connect("127.0.0.1", forwardPort).await();
        System.out.println("send client connect forward station success!!!");
    }

    private void startLimitDestServer(final int kb) throws InterruptedException {
        new ServerBootstrap()
                .group(new NioEventLoopGroup(1), new NioEventLoopGroup(1))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        final ChannelTrafficShapingHandler trafficShapingHandler = new ChannelTrafficShapingHandler(0, kb * 1024, 1000);
                        ch.pipeline().addLast(trafficShapingHandler);
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ReferenceCountUtil.release(msg);
                            }
                        });
                        ch.eventLoop().scheduleWithFixedDelay(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("dest server traffic : " + trafficShapingHandler.trafficCounter().toString());
                            }
                        }, 1, 5, TimeUnit.SECONDS);
                    }
                }).bind(destServerPort).await();
        System.out.println("dest server start success!");
    }
}
