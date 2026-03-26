package com.DDT;

import com.DDT.channelhandler.hander.MethodCallHandler;
import com.DDT.channelhandler.hander.RpcRequestDecoder;
import com.DDT.channelhandler.hander.RpcResponseEncoder;
import com.DDT.discovery.Registry;
import com.DDT.discovery.RegistryConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;


import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RpcBootstrap {
    // rpcBootstrap是个单例，我们希望每个应用程序只有一个实例
    public static final RpcBootstrap rpcBootstrap = new RpcBootstrap();


    private String appName = "default";
    private ReferenceConfig referenceConfig;
    private ServiceConfig serviceConfig;
    private RegistryConfig registryConfig;
    private ProtocolConfig protocolConfig;
    private int port = 8088;


    // 注册中心
    private Registry registry;

    // 维护已经发布且暴露的服务列表 key-> interface的全限定名  value -> ServiceConfig
    public static final Map<String,ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);

    // 连接通道的缓存
    public static final Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);

    // 定义全局的对外挂起的 completableFuture
    public final static Map<Long, CompletableFuture<Object>> PENDING_REQUEST = new ConcurrentHashMap<>(128);

    // 请求id生成器
    public static final IdGenerator ID_GENERATOR = new IdGenerator(1L, 2L);

    public static String SERIALIZE_TYPE = "jdk";

    private RpcBootstrap() {
        // 构造启动引导程序，时需要做一些什么初始化的事
    }

    public static RpcBootstrap getInstance() {
        return rpcBootstrap;
    }

    /**
     * 用来定义当前应用的名字
     * @param appName 应用的名字
     * @return this当前实例
     */
    public RpcBootstrap application(String appName) {
        this.appName = appName;
        return this;
    }

    /**
     * 用来配置一个注册中心
     * @param registryConfig 注册中心
     * @return this当前实例
     */
    public RpcBootstrap registry(RegistryConfig registryConfig) {
        // 这里维护一个zookeeper实例，但是，如果这样写就会将zookeeper和当前工程耦合
        // 我们其实是更希望以后可以扩展更多种不同的实现

        // 尝试使用 registryConfig 获取一个注册中心，有点工厂设计模式的意思了
        this.registry = registryConfig.getRegistry();
        return this;
    }

    /**
     * 配置当前暴露的服务使用的协议
     * @param protocolConfig 协议的封装
     * @return this当前实例
     */
    public RpcBootstrap protocol(ProtocolConfig protocolConfig) {
        this.protocolConfig = protocolConfig;
        if(log.isDebugEnabled()){
            log.debug("当前工程使用了：{}协议进行序列化",protocolConfig.toString());
        }
        return this;
    }

    /**
     * ---------------------------服务提供方的相关api---------------------------------
     */

    /**
     * 发布服务，将接口-》实现，注册到服务中心
     * @param service 封装的需要发布的服务
     * @return this当前实例
     */
    public RpcBootstrap publish(ServiceConfig<?> service) {
        // 抽象了注册中心
        // 1、将服务注册到注册中心
        registry.register(service);

        // 1、当服务调用方，通过接口、方法名、具体的方法参数列表发起调用，提供怎么知道使用哪一个实现
        // (1) new 一个  （2）spring beanFactory.getBean(Class)  (3) 自己维护映射关系
        SERVERS_LIST.put(service.getInterface().getName(), service);
        return this;
    }

    /**
     * 批量发布
     * @param services 封装的需要发布的服务集合
     * @return this当前实例
     */
    public RpcBootstrap publish(List<ServiceConfig<?>> services) {
        for (ServiceConfig<?> service : services) {
            publish(service);
        }
        return this;
    }


    /**
     * 启动netty服务
     */
    public void start() throws InterruptedException {
        EventLoopGroup boss = new NioEventLoopGroup(2);
        EventLoopGroup worker = new NioEventLoopGroup(10);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap = bootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        // 这里我们就可以添加一些handler了，编解码器，业务处理器
                        socketChannel.pipeline()
                                .addLast(new LoggingHandler(LogLevel.DEBUG))
                                .addLast(new RpcRequestDecoder())
                                // 根据请求进行方法调用
                                .addLast(new MethodCallHandler())
                                // 
                                .addLast(new RpcResponseEncoder())
                        ;
                    }
                });

        // 4、绑定端口
        ChannelFuture channelFuture = bootstrap.bind(port).sync();

        channelFuture.channel().closeFuture().sync();
        try {
            boss.shutdownGracefully().sync();
            worker.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * ---------------------------服务调用方的相关api---------------------------------
     */
    public RpcBootstrap reference(ReferenceConfig<?> reference) {
        // 在这个方法里我们是否可以拿到相关的配置项-注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        // 1、reference需要一个注册中心
        reference.setRegistry(registry);
        return this;
    }

}
