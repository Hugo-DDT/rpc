package com.DDT.channelhandler.hander;

import com.DDT.RpcBootstrap;
import com.DDT.transport.message.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;

/**
 * 该类是消费者的handler，负责处理服务提供方返回的结果
 */
@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<RpcResponse> {


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse rpcResponse) throws Exception {
        // 服务提供方，给与的结果
        Object result = rpcResponse.getBody();
        // todo 需要针对code做处理
        result = result == null ? new Object() : result;

        // 从全局的挂起的请求中寻找与之匹配的待处理的 cf
        CompletableFuture<Object> completableFuture = RpcBootstrap.PENDING_REQUEST.remove(rpcResponse.getRequestId());
        if (completableFuture != null) {
            completableFuture.complete(result);
        } else {
            log.warn("未找到请求【{}】对应的挂起任务，响应体：{}", rpcResponse.getRequestId(), result);
        }

        log.info("消费者接收到服务提供方的结果：{}",result);
    }
}
