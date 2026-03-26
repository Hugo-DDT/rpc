package com.DDT.channelhandler.hander;

import com.DDT.RpcBootstrap;
import com.DDT.ServiceConfig;
import com.DDT.enumeration.RespCode;
import com.DDT.transport.message.RequestPayload;
import com.DDT.transport.message.RpcRequest;
import com.DDT.transport.message.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

@Slf4j
public class MethodCallHandler extends SimpleChannelInboundHandler<RpcRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) {
        // 1、获取负载内容
        RequestPayload requestPayload = rpcRequest.getRequestPayload();

        // 2、根据负载内容进行方法调用
        Object object = callTargetMethod(requestPayload);
        log.info("请求【{}】已经在服务端完成调用", rpcRequest.getRequestId());

        // todo 3、封装响应
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setBody(object);
        rpcResponse.setCode(RespCode.SUCCESS.getCode());
        rpcResponse.setRequestId(rpcRequest.getRequestId());
        rpcResponse.setCompressType(rpcRequest.getCompressType());
        rpcResponse.setSerializeType(rpcRequest.getSerializeType());


        // 4、写出响应
        channelHandlerContext.channel().writeAndFlush(rpcResponse);
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
