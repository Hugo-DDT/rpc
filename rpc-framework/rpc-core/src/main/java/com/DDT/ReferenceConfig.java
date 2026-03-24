package com.DDT;

import com.DDT.discovery.Registry;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;

@Slf4j
public class ReferenceConfig<T> {
     private Class<T> interfaceRef;

     @Setter
     @Getter
     private Registry registry;


    /**
     * 代理设计模式，生成一个api接口的代理对象，helloYrpc.sayHi("你好");
     * @return 代理对象
     */
     public T get() {

        // 使用动态代理完成一些工作
         ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
         Class[] classes = new Class[]{interfaceRef};

         // 使用动态代理，生成一个接口的实现类，这个实现类是我们自己写的InvocationHandler
         Object helloProxy = Proxy.newProxyInstance(classLoader, classes, (proxy, method, args) -> {
             // 这里就会调用我们自己写的InvocationHandler
             // 1、连接注册中心
             // 2、拉取服务列表
             // 3、选择一个服务并建立连接
             // 4、发送请求，携带一些信息（接口名，参数列表，方法的名字），获得结果
             log.info("接口：{}，方法：{}，参数：{}",interfaceRef.getName(),method.getName(),args);

             // 1、发现服务，从注册中心，寻找一个可用的服务
             // 传入服务的名字,返回ip+端口
             InetSocketAddress address = registry.lookup(method.getDeclaringClass().getName());
             if(log.isDebugEnabled()){
                 log.debug("服务调用方，发现了服务【{}】的可用主机【{}】.",
                         interfaceRef.getName(),address);
             }
             // 2、使用Netty连接服务器，发送 调用的 服务的名字+方法名字+参数列表，得到结果

             return null;
         });
         return (T)helloProxy;
     }

    public Class<T> getInterface() {
        return interfaceRef;
    }

    public void setInterface(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }

}
