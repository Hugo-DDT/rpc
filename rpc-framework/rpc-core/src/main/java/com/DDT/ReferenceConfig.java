package com.DDT;

import com.DDT.discovery.Registry;
import com.DDT.proxy.handler.RpcConsumerInvocationHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

@Slf4j
public class ReferenceConfig<T> {
     private Class<T> interfaceRef;

     @Setter
     @Getter
     private Registry registry;


    /**
     * 代理设计模式，生成一个api接口的代理对象，hellorpc.sayHi("你好");
     * @return 代理对象
     */
     public T get() {

        // 使用动态代理完成一些工作
         ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
         Class[] classes = new Class[]{interfaceRef};
         InvocationHandler handler = new RpcConsumerInvocationHandler(registry, interfaceRef);

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
