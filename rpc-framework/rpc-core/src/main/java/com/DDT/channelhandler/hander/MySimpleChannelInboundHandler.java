package com.DDT.channelhandler.hander;

import com.DDT.RpcBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

/**
 * 该类是消费者的handler，负责处理服务提供方返回的结果
 */
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf msg) throws Exception {
        // 服务提供方，给与的结果
        String result = msg.toString(Charset.defaultCharset());
        // 从全局的挂起的请求中寻找与之匹配的待处理的 cf
        CompletableFuture<Object> completableFuture = RpcBootstrap.PENDING_REQUEST.get(1L);
        completableFuture.complete(result);
    }
}
