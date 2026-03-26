package com.DDT.proxy.handler;

import com.DDT.RpcBootstrap;
import com.DDT.compress.CompressorFactory;
import com.DDT.discovery.Registry;
import com.DDT.enumeration.RequestType;
import com.DDT.exceptions.NetworkException;
import com.DDT.NettyBootstrapInitializer;
import com.DDT.serialize.SerializerFactory;
import com.DDT.transport.message.RequestPayload;
import com.DDT.transport.message.RpcRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.server.Request;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 该类封装了客户端通信的基础逻辑，每一个代理对象的远程调用过程都封装在这个invoke方法中
 * 1、发现可用服务
 * 2、建立连接
 * 3、发送请求
 * 4、得到结果
 */
@Slf4j
public class RpcConsumerInvocationHandler implements InvocationHandler {

    private Registry registry;

    private Class<?> interfaceRef;

    public RpcConsumerInvocationHandler(Registry registry, Class<?> interfaceRef) {
        this.registry = registry;
        this.interfaceRef = interfaceRef;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 这里就会调用我们自己写的InvocationHandler
        // 1、连接注册中心
        // 2、拉取服务列表
        // 3、选择一个服务并建立连接
        // 4、发送请求，携带一些信息（接口名，参数列表，方法的名字），获得结果
        log.info("接口：{}，方法：{}，参数：{}",interfaceRef.getName(),method.getName(),args);

        // 1、发现服务，从注册中心，寻找一个可用的服务
        // 传入服务的名字,返回ip+端口
        InetSocketAddress address = registry.lookup(method.getDeclaringClass().getName());
        log.debug("服务调用方，发现了服务【{}】的可用主机【{}】.", interfaceRef.getName(),address);

        // 使用netty连接服务器，发送 调用的 服务的名字+方法名字+参数列表，得到结果
        // 定义线程池，EventLoopGroup
        // q：整个连接过程放在这里行不行，也就意味着每次调用都会产生一个新的netty连接。如何缓存我们的连接,也就意味着，每次在此处建立一个新的连接是不合适的

        // 解决方案？缓存channel，尝试从缓存中获取channel，如果未获取，则创建新的连接，并进行缓存


        // 2、获取一个可用通道
        Channel channel = getAvailableChannel(address);
        log.debug("成功获取到与服务【{}】的连接通道。", interfaceRef.getName());


        /*
         * ------------------ 封装报文 ---------------------------
         */
        // 3、封装报文
        RequestPayload requestPayload = RequestPayload.builder()
                .interfaceName(interfaceRef.getName())
                .methodName(method.getName())
                .parametersType(method.getParameterTypes())
                .parametersValue(args)
                .returnType(method.getReturnType())
                .build();


        long requestId = RpcBootstrap.ID_GENERATOR.getId();
        RpcRequest rpcRequest = RpcRequest.builder()
                .requestId(requestId)
                .compressType((CompressorFactory.getCompressor(RpcBootstrap.COMPRESS_TYPE).getCode()))
                .requestType(RequestType.REQUEST.getId())
                .serializeType((SerializerFactory.getSerializer(RpcBootstrap.SERIALIZE_TYPE).getCode()))
                .requestPayload(requestPayload)
                .build();
        /*
         * ------------------同步策略-------------------------
         */
//                ChannelFuture channelFuture = channel.writeAndFlush(new Object()).await();
        // 需要学习channelFuture的简单的api get 阻塞获取结果，getNow 获取当前的结果，如果未处理完成，返回null
//                if(channelFuture.isDone()){
//                    Object object = channelFuture.getNow();
//                } else if( !channelFuture.isSuccess() ){
//                    // 需要捕获异常,可以捕获异步任务中的异常
//                    Throwable cause = channelFuture.cause();
//                    throw new RuntimeException(cause);
//                }

        /*
         * ------------------异步策略-------------------------
         */
        // 4、写出报文
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        RpcBootstrap.PENDING_REQUEST.put(requestId, completableFuture);

        // 这里这几 writeAndFlush 写出一个请求，这个请求的实例就会进入pipeline执行出站的一系列操作
        // 我们可以想象得到，第一个出站程序一定是将 rpcRequest --> 二进制的报文
        channel.writeAndFlush(rpcRequest).addListener((ChannelFutureListener) promise -> {
            // 当前的promise将来返回的结果是什么，writeAndFlush的返回结果
            // 一旦数据被写出去，这个promise也就结束了
            // 但是我们想要的是什么？  服务端给我们的返回值，所以这里处理completableFuture是有问题的
            // 是不是应该将 completableFuture 挂起并且暴露，并且在得到服务提供方的响应的时候调用complete方法
//                    if(promise.isDone()){
//                        completableFuture.complete(promise.getNow());
//                    }

            // 只需要处理以下异常就行了
            if (!promise.isSuccess()) {
                completableFuture.completeExceptionally(promise.cause());
            }
        });
//
        // 如果没有地方处理这个 completableFuture ，这里会阻塞，等待complete方法的执行
        // q: 我们需要在哪里调用complete方法得到结果，很明显 pipeline 中最终的handler的处理结果
        // 5、获得响应结果
        return completableFuture.get(10, TimeUnit.SECONDS);
    }

    /**
     * 根据地址获取一个可用的channel，首先尝试从缓存中获取，如果没有，则创建一个新的连接，并进行缓存
     * @param address
     * @return
     */
    private Channel getAvailableChannel(InetSocketAddress address) {
        // 尝试从缓存中获取channel
        Channel channel = RpcBootstrap.CHANNEL_CACHE.get(address);

        if (channel == null) {
            // await 方法会阻塞，会等待连接成功在返回，netty还提供了异步处理的逻辑
//                 channel = NettyBootstrapInitializer.getBootstrap()
//                         .connect(address).await().channel();
            // sync和await都是阻塞当前线程，获取返回值（连接的过程是异步的，发生数据的过程是异步的）
            // 如果发生了异常，sync会主动在主线程抛出异常，await不会，异常在子线程中处理需要使用future中处理


            // 使用addListener执行的异步操作
            CompletableFuture<Channel> channelFuture = new CompletableFuture<>();
            NettyBootstrapInitializer.getBootstrap().connect(address).addListener(
                    (ChannelFutureListener) promise -> {
                        if (promise.isDone()) {
                            // 异步的，我们已经完成
                            log.debug("已经和【{}】成功建立了连接。", address);

                            channelFuture.complete(promise.channel());
                        } else if (!promise.isSuccess()) {
                            channelFuture.completeExceptionally(promise.cause());
                        }
                    }
            );

            // 阻塞获取channel
            try {
                channel = channelFuture.get(3, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                log.error("与【{}】建立连接时发生了异常。", address, e);
                throw new RuntimeException(e);
            }

            // 缓存channel
            RpcBootstrap.CHANNEL_CACHE.put(address,channel);
        }
        if (channel == null) {
            log.error("获取或建立与【{}】的通道时发生了异常。", address);
            throw new NetworkException("获取通道时发生了异常。");
        }
        return channel;
    }
}
