package com.DDT.core;


import com.DDT.NettyBootstrapInitializer;
import com.DDT.RpcBootstrap;
import com.DDT.compress.CompressorFactory;
import com.DDT.discovery.Registry;
import com.DDT.enumeration.RequestType;
import com.DDT.serialize.SerializerFactory;
import com.DDT.transport.message.RpcRequest;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 心跳检测器
 */
@Slf4j
public class HeartbeatDetector {



    public static void detectHeartbeat(String serviceName) {
        // 获取注册中心
        Registry registry = RpcBootstrap.getInstance().getConfiguration().getRegistryConfig().getRegistry();
        // 从服务中心获取服务列表，进行心跳检测
        List<InetSocketAddress> addresses = registry.lookup(serviceName);

        // 将连接进行缓存
        for (InetSocketAddress address : addresses) {
            try {
                // 缓存中没有的服务地址才进行连接，已经存在的服务地址说明之前已经连接过了，不需要重复连接
                if(!RpcBootstrap.CHANNEL_CACHE.containsKey(address)){
                    Channel channel = NettyBootstrapInitializer.getBootstrap().connect(address).sync().channel();
                    RpcBootstrap.CHANNEL_CACHE.put(address, channel);
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // 开启一个线程，定时进行心跳检测
        Thread thread = new Thread(() ->
            new Timer().scheduleAtFixedRate(new MyTimerTask(), 0, 2000)
        , "rpc-HeartbeatDetector-thread");
        thread.setDaemon(true);
        thread.start();
    }

    // 心跳检测定时任务
    private static class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            // 将响应时长的map清空
            RpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.clear();

            // 遍历所有的channel,对每一个服务地址进行心跳检测，获取响应时间，并将响应时间和channel进行缓存
            Map<InetSocketAddress, Channel> cache = RpcBootstrap.CHANNEL_CACHE;
            for (Map.Entry<InetSocketAddress, Channel> entry: cache.entrySet()){

                // 定义一个重试的次数
                int tryTimes = 3;
                while (tryTimes > 0) {
                    Channel channel = entry.getValue();

                    long start = System.currentTimeMillis();
                    // 构建一个心跳请求
                    RpcRequest rpcRequest = RpcRequest.builder()
                            .requestId(RpcBootstrap.ID_GENERATOR.getId())
                            .compressType(CompressorFactory.getCompressor(RpcBootstrap.getInstance().getConfiguration().getCompressType()).getCode())
                            .requestType(RequestType.HEART_BEAT.getId())
                            .serializeType(SerializerFactory.getSerializer(RpcBootstrap.getInstance().getConfiguration().getSerializeType()).getCode())
                            .timeStamp(start)
                            .build();

                    // 4、写出报文
                    CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                    // 将 completableFuture 暴露出去
                    RpcBootstrap.PENDING_REQUEST.put(rpcRequest.getRequestId(), completableFuture);

                    channel.writeAndFlush(rpcRequest).addListener((ChannelFutureListener) promise -> {
                        if (!promise.isSuccess()) {
                            completableFuture.completeExceptionally(promise.cause());
                        }
                    });

                    Long endTime = 0L;
                    try {
                        // 阻塞方法，get()方法如果得不到结果，就会一直阻塞
                        // 我们想不一直阻塞可以添加参数
                        completableFuture.get(1, TimeUnit.SECONDS);
                        endTime = System.currentTimeMillis();
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {


                        // 一旦发生问题，需要优先重试
                        tryTimes --;
                        log.error("和地址为【{}】的主机连接发生异常.正在进行第【{}】次重试......",
                                channel.remoteAddress(), 3 - tryTimes);

                        // 将重试的机会用尽，将失效的地址移出服务列表
                        if(tryTimes == 0){
                            RpcBootstrap.CHANNEL_CACHE.remove(entry.getKey());
                        }

                        // 尝试等到一段时间后重试
                        try {
                            // 随机重试时间，避免所有的重试都在同一时间进行，造成雪崩效应
                            Thread.sleep(10*(new Random().nextInt(5)));
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                        continue;
                    }
                    Long time = endTime - start;

                    // 使用treemap进行缓存
                    RpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.put(time,channel);
                    log.debug("和[{}]服务器的响应时间是[{}].",entry.getKey(),time);
                    break;
                }
            }

            log.info("-----------------------响应时间的treemap----------------------");
            for (Map.Entry<Long,Channel> entry:RpcBootstrap.ANSWER_TIME_CHANNEL_CACHE.entrySet() ){
                if(log.isDebugEnabled()){
                    log.debug("[{}]--->channelId:[{}]",entry.getKey(),entry.getValue().id());
                }
            }
        }
    }
}
