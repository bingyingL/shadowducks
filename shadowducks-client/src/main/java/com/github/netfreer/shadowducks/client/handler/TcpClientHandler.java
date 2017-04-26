package com.github.netfreer.shadowducks.client.handler;

import com.github.netfreer.shadowducks.common.config.AppConfig;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author: landy
 * @date: 2017-04-26 22:24
 */
public class TcpClientHandler extends ChannelInboundHandlerAdapter {
    private final AppConfig config;

    public TcpClientHandler(AppConfig config) {
        this.config=config;
    }
}
