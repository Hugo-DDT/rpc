package com.DDT;

import com.DDT.channelhandler.ConsumerChannelInitializer;
import com.DDT.channelhandler.hander.MySimpleChannelInboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

/**
 * 提供bootstrap单例
 */
public class NettyBootstrapInitializer {
    @Getter
    public static final Bootstrap bootstrap = new Bootstrap();

    static {
        // 初始化bootstrap
        // 1、配置线程模型
        // 2、选择初始化一个什么样的channel
        bootstrap.group(new NioEventLoopGroup())
            .channel(NioSocketChannel.class)
            .handler(new ConsumerChannelInitializer());
    }

    private NettyBootstrapInitializer() {
    }

}
