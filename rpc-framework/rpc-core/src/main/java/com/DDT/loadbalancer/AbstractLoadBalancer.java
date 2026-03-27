package com.DDT.loadbalancer;

import com.DDT.RpcBootstrap;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractLoadBalancer implements LoadBalancer{

    // 服务名和负载均衡选择器的映射做缓存，一个服务对应一个选择器
    private Map<String, Selector> cache = new ConcurrentHashMap<>();

    @Override
    public InetSocketAddress selectServiceAddress(String serviceName) {
        // 从缓存中获取选择器，如果没有则创建一个新的选择器并缓存
        Selector selector = cache.get(serviceName);

        // 如果没有找到选择器，说明是第一次访问这个服务，需要从注册中心获取服务列表并创建选择器
        if (selector == null) {

            // 对负载均衡器，内部维护服务列表作为缓存
            List<InetSocketAddress> serviceList = RpcBootstrap.getInstance().getRegistry().lookup(serviceName);

            // 根据服务列表创建一个选择器，具体的负载均衡算法由子类实现
            selector = getSelector(serviceList);

            // 缓存选择器，后续访问同一服务时直接使用缓存的选择器
            cache.put(serviceName, selector);
        }

        // 使用选择器获取一个服务地址
        return selector.getNext();
    }

    /**
     * 由子类拓展实现具体的负载均衡算法，根据服务列表创建一个选择器
     * @param serviceList 服务列表
     * @return 负载均衡选择器
     */
    protected abstract Selector getSelector(List<InetSocketAddress> serviceList);
}
