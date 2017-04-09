package com.github.landyking.shadowducks.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.util.ReferenceCountUtil;

import java.util.Iterator;
import java.util.Map;

public class HttpRecordHandler extends ChannelInboundHandlerAdapter {
    private EmbeddedChannel httpRecordChannel = new EmbeddedChannel(new HttpRequestDecoder());
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf copy = ((ByteBuf) msg).copy();
        httpRecordChannel.writeInbound(copy);
        Object http = httpRecordChannel.readInbound();
        if (http != null) {
            if (http instanceof HttpRequest) {
                HttpRequest request = (HttpRequest) http;
                if(request.getDecoderResult().isSuccess()){
                    System.out.println("uri:"+request.getUri());
                    Iterator<Map.Entry<String, String>> headerIter = request.headers().iterator();
                    while (headerIter.hasNext()) {
                        Map.Entry<String, String> one = headerIter.next();
                        System.out.println(one.getKey() + "=" + one.getValue());
                    }
                }else{
                    request.getDecoderResult().cause().printStackTrace();
                }

            }
            ReferenceCountUtil.release(http);
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        httpRecordChannel.close();
        super.channelInactive(ctx);
    }
}
