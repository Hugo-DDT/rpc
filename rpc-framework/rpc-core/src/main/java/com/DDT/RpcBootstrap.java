package com.DDT;

import com.DDT.annotation.RpcApi;
import com.DDT.channelhandler.hander.MethodCallHandler;
import com.DDT.channelhandler.hander.RpcRequestDecoder;
import com.DDT.channelhandler.hander.RpcResponseEncoder;
import com.DDT.config.Configuration;
import com.DDT.core.HeartbeatDetector;
import com.DDT.discovery.RegistryConfig;
import com.DDT.loadbalancer.LoadBalancer;
import com.DDT.transport.message.RpcRequest;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;


import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class RpcBootstrap {
    // rpcBootstrap是个单例，我们希望每个应用程序只有一个实例
    public static final RpcBootstrap rpcBootstrap = new RpcBootstrap();


    // 全局的配置中心
    private final Configuration configuration;



    // 维护已经发布且暴露的服务列表 key-> interface的全限定名  value -> ServiceConfig
    public static final Map<String,ServiceConfig<?>> SERVERS_LIST = new ConcurrentHashMap<>(16);

    // 维护已经连接的服务地址和通道的映射关系，key->服务地址  value->netty的channel对象
    public static final Map<InetSocketAddress, Channel> CHANNEL_CACHE = new ConcurrentHashMap<>(16);

    // 定义全局的对外挂起的 completableFuture
    public static final Map<Long, CompletableFuture<Object>> PENDING_REQUEST = new ConcurrentHashMap<>(128);

    // 定义一个线程变量，来存储每一个请求的上下文信息，主要是为了负载均衡算法服务的
    public static final ThreadLocal<RpcRequest> REQUEST_THREAD_LOCAL = new ThreadLocal<>();

    // 缓存每一个服务通道的心跳响应时间，用来获取最小响应服务
    public final static TreeMap<Long, Channel> ANSWER_TIME_CHANNEL_CACHE = new TreeMap<>();

    // 请求id生成器
    public static final IdGenerator ID_GENERATOR = new IdGenerator(1L, 2L);



    private RpcBootstrap() {
        // 构造启动引导程序，时需要做一些什么初始化的事
        this.configuration = new Configuration();
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
        configuration.setAppName(appName);
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
        configuration.setRegistryConfig(registryConfig);

        return this;
    }

    /**
     * 配置负载均衡策略
     * @param loadBalancer 注册中心
     * @return this当前实例
     */
    public RpcBootstrap loadBalancer(LoadBalancer loadBalancer) {
        configuration.setLoadBalancer(loadBalancer);
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
        configuration.getRegistryConfig().getRegistry().register(service);

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
                                // 将方法调用的结果进行编码，写出到客户端
                                .addLast(new RpcResponseEncoder())
                        ;
                    }
                });

        // 4、绑定端口
        ChannelFuture channelFuture = bootstrap.bind(configuration.getPort()).sync();

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

    /**
     * 配置服务调用方的相关信息，主要是为了生成代理对象时使用
     * @param reference
     * @return
     */
    public RpcBootstrap reference(ReferenceConfig<?> reference) {

        // 开启对这个服务的心跳检测
        HeartbeatDetector.detectHeartbeat(reference.getInterface().getName());
        // 在这个方法里我们是否可以拿到相关的配置项-注册中心
        // 配置reference，将来调用get方法时，方便生成代理对象
        // 1、reference需要一个注册中心
        reference.setRegistry(configuration.getRegistryConfig().getRegistry());
        return this;
    }

    /**
     * 配置序列化的方式
     * @param serializeType 序列化的方式
     */
    public RpcBootstrap serialize(String serializeType) {
        configuration.setSerializeType(serializeType);
        if(log.isDebugEnabled()){
            log.debug("我们配置了使用的序列化的方式为【{}】.",serializeType);
        }
        return this;
    }

    /**
     * 配置压缩方法
     * @param compressType 压缩方法
     * @return
     */
    public RpcBootstrap compress(String compressType) {
        configuration.setCompressType(compressType);
        if(log.isDebugEnabled()){
            log.debug("我们配置了使用的压缩算法为【{}】.",compressType);
        }
        return this;
    }

    /**
     * 通过包扫描的方式，批量发布服务
     * @param packageName
     * @return
     */
    public RpcBootstrap scan(String packageName) {
        List<String> classNames = getAllClassNames(packageName);

        List<Class<?>> classes = classNames.stream()
                .map(className -> {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).filter(clazz -> clazz.isAnnotationPresent(RpcApi.class))
                .collect(Collectors.toList());

        for (Class<?> clazz : classes) {
            // 获取他的接口
            Class<?>[] interfaces = clazz.getInterfaces();
            Object instance = null;
            try {
                instance = clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }



            for (Class<?> anInterface : interfaces) {
                ServiceConfig<?> serviceConfig = new ServiceConfig<>();
                serviceConfig.setInterface(anInterface);
                serviceConfig.setRef(instance);
                if (log.isDebugEnabled()){
                    log.debug("---->已经通过包扫描，将服务【{}】发布.",anInterface);
                }
                // 3、发布
                publish(serviceConfig);
            }

        }

        return this;
    }

    /**
     * 通过包名获取到这个包下的所有类的全限定名
     * @param packageName
     * @return
     */
    private List<String> getAllClassNames(String packageName) {

        // 通过包名获取到一个绝对路径
        // bashPath = com.DDT  -->  com/DDT
        String basePath = packageName.replaceAll("\\.", "/");
        // url = file:/G:/.../com/DDT
        URL url = ClassLoader.getSystemResource(basePath);
        if(url == null){
            throw new RuntimeException("包扫描时，发现路径不存在.");
        }
        // absolutePath = G://.../com/DDT
        String absolutePath = url.getPath();

        // 通过绝对路径进行递归扫描，获取到所有的类的全限定名
        return recursionFile(absolutePath, basePath);
    }


    private List<String> recursionFile(String absolutePath, String basePath) {
        // 根据当前路径创建文件对象（可能是目录，也可能是文件）
        File file = new File(absolutePath);
        // 用于收集扫描到的类全限定名
        List<String> classNames = new ArrayList<>();

        // 如果是目录，则继续递归扫描子文件/子目录
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    classNames.addAll(recursionFile(f.getAbsolutePath(), basePath));
                }
            }
            return classNames;
        }

        // 走到这里说明是文件，先获取绝对路径
        String absoluteFilePath = file.getAbsolutePath();
        // 只处理 .class 文件，其他文件直接忽略
        if (!absoluteFilePath.endsWith(".class")) {
            return classNames;
        }

        // 统一路径分隔符，避免 Windows/Linux 差异
        String normalizedPath = absoluteFilePath.replace("\\", "/");
        // 找到包路径在绝对路径中的起始位置
        int start = normalizedPath.indexOf(basePath);
        // 没找到说明不在目标包下，直接返回
        if (start < 0) {
            return classNames;
        }

        // 截取包路径后的内容并转成类全限定名，同时去掉 .class 后缀
        String className = normalizedPath
                .substring(start)
                .replace("/", ".")
                .replaceAll("\\.class$", "");

        classNames.add(className);
        return classNames;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

}
