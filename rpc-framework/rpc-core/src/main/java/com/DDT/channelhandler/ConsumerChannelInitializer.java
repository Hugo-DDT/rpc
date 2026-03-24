package com.DDT.channelhandler;

import com.DDT.channelhandler.hander.MySimpleChannelInboundHandler;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

/**
 * 该类是消费者的channelInitializer，负责初始化消费者的channel
 */
public class ConsumerChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        // 添加一些handler了，编解码器，业务处理器
        socketChannel.pipeline().addLast(new MySimpleChannelInboundHandler() {

        });
    }
}
