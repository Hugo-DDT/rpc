package com.DDT;

import com.DDT.discovery.Registry;
import com.DDT.proxy.handler.RpcConsumerInvocationHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * ReferenceConfig是一个用于配置和获取RPC服务代理对象的类。它封装了RPC服务接口的相关信息，并通过动态代理生成对应的代理对象，以便客户端可以通过该代理对象调用远程服务。
 * 1、interfaceRef：表示RPC服务接口的Class对象，用于指定要调用的远程服务接口。
 * 2、registry：表示服务注册中心的实例，用于获取远程服务的地址和相关信息。
 * 3、group：表示服务分组信息，用于区分不同的服务版本或环境。
 * 4、get()方法：通过动态代理生成RPC服务接口的代理对象，客户端可以通过该代理对象调用远程服务的方法。该方法内部使用了RpcConsumerInvocationHandler作为InvocationHandler来处理代理对象的方法调用逻辑。
 * @param <T>
 */
@Slf4j
public class ReferenceConfig<T> {
     private Class<T> interfaceRef;

     @Setter
     @Getter
     private Registry registry;

    // 分组信息
    @Setter
    @Getter
    private String group;


    /**
     * 代理设计模式，生成一个api接口的代理对象，hellorpc.sayHi("你好");
     * @return 代理对象
     */
     public T get() {

        // 使用动态代理完成一些工作
         ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
         Class[] classes = new Class[]{interfaceRef};
         InvocationHandler handler = new RpcConsumerInvocationHandler(registry, interfaceRef, group);

         // 使用动态代理生成代理对象
         Object helloProxy = Proxy.newProxyInstance(classLoader, classes,handler);
         return (T)helloProxy;
     }

    public Class<T> getInterface() {
        return interfaceRef;
    }

    public void setInterface(Class<T> interfaceRef) {
        this.interfaceRef = interfaceRef;
    }

}
