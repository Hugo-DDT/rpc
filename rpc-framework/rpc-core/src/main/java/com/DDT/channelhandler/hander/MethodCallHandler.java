package com.DDT.channelhandler.hander;

import com.DDT.RpcBootstrap;
import com.DDT.ServiceConfig;
import com.DDT.core.ShutDownHolder;
import com.DDT.enumeration.RequestType;
import com.DDT.enumeration.RespCode;
import com.DDT.protection.RateLimiter;
import com.DDT.protection.TokenBuketRateLimiter;
import com.DDT.transport.message.RequestPayload;
import com.DDT.transport.message.RpcRequest;
import com.DDT.transport.message.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * 该类是服务端的核心处理器，负责处理客户端发送过来的请求，并且进行方法调用，最后将结果返回给客户端
 * 1、首先我们需要获取到客户端发送过来的请求报文，这个报文中包含了我们需要调用的方法的相关信息，比如接口名、方法名、参数类型、参数值等
 * 2、我们需要根据这些信息找到对应的服务实现类，并且通过反射调用对应的方法，得到方法的返回值
 * 3、最后我们需要将方法的返回值封装成一个响应报文，并且发送回客户端
 */
@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<RpcRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) {
        // 1、先封装部分响应
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setRequestId(rpcRequest.getRequestId());
        rpcResponse.setCompressType(rpcRequest.getCompressType());
        rpcResponse.setSerializeType(rpcRequest.getSerializeType());

        // 2、获取通道
        Channel channel = channelHandlerContext.channel();

        // 3、查看关闭的挡板是否打开，如果挡板已经打开，返回一个错误的响应
        if( ShutDownHolder.BAFFLE.get() ){
            rpcResponse.setCode(RespCode.BECOLSING.getCode());
            channel.writeAndFlush(rpcResponse);
            return;
        }

        // 4、计数器加一
        ShutDownHolder.REQUEST_COUNTER.increment();

        // 5、完成限流相关的操作

        SocketAddress socketAddress = channel.remoteAddress();
        Map<SocketAddress, RateLimiter> everyIpRateLimiter = RpcBootstrap.getInstance().getConfiguration().getEveryIpRateLimiter();
        RateLimiter rateLimiter = everyIpRateLimiter.get(socketAddress);
        if (rateLimiter == null) {
            rateLimiter = new TokenBuketRateLimiter(10, 10);
            everyIpRateLimiter.put(socketAddress, rateLimiter);
        }
        boolean allowRequest = rateLimiter.allowRequest();

        // 6、处理请求的逻辑
        // 限流
        if (!allowRequest) {
            // 需要封装响应并且返回了
            rpcResponse.setCode(RespCode.RATE_LIMIT.getCode());
            // 处理心跳
        } else if (rpcRequest.getRequestType() == RequestType.HEART_BEAT.getId()) {
            // 需要封装响应并且返回
            rpcResponse.setCode(RespCode.SUCCESS_HEART_BEAT.getCode());
            // 正常调用
        } else {
            /** ---------------具体的调用过程--------------**/
            // （1）、获取负载内容
            RequestPayload requestPayload = rpcRequest.getRequestPayload();

            // （2）、根据负载内容进行方法调用
            try {
                Object result = callTargetMethod(requestPayload);
                if (log.isDebugEnabled()) {
                    log.debug("请求【{}】已经在服务端完成方法调用。", rpcRequest.getRequestId());
                }
                // （3）、封装响应   我们是否需要考虑另外一个问题，响应码，响应类型
                rpcResponse.setCode(RespCode.SUCCESS.getCode());
                rpcResponse.setBody(result);
            } catch (Exception e){
                log.error("编号为【{}】的请求在调用过程中发生异常。",rpcRequest.getRequestId(),e);
                rpcResponse.setCode(RespCode.FAIL.getCode());
            }
        }

        // 7、写出响应
        channel.writeAndFlush(rpcResponse);

        // 8、计数器减一
        ShutDownHolder.REQUEST_COUNTER.decrement();
    }

    private Object callTargetMethod(RequestPayload requestPayload) {
        String interfaceName = requestPayload.getInterfaceName();
        String methodName = requestPayload.getMethodName();
        Class<?>[] parametersType = requestPayload.getParametersType();
        Object[] parametersValue = requestPayload.getParametersValue();

        // 寻找到匹配的暴露出去的具体的实现
        ServiceConfig<?> serviceConfig = RpcBootstrap.SERVERS_LIST.get(interfaceName);
        Object refImpl = serviceConfig.getRef();

        // 通过反射调用 1、获取方法对象  2、执行invoke方法
        Object returnValue;
        try {
            Class<?> aClass = refImpl.getClass();
            Method method = aClass.getMethod(methodName, parametersType);
            returnValue = method.invoke(refImpl, parametersValue);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            log.error("调用服务【{}】的方法【{}】时发生了异常。",interfaceName,methodName,e);
            throw new RuntimeException(e);
        }
        return returnValue;
    }
}
