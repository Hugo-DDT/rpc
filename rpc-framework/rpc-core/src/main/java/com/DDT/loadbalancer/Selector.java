package com.DDT.loadbalancer;

import java.net.InetSocketAddress;

/**
 * 负载均衡选择器接口，定义了获取服务节点和重新平衡的方法
 */
public interface Selector {
    /**
     * 根据服务列表执行一种算法获取一个服务节点
     * @return 具体的服务节点
     */
    InetSocketAddress getNext();
}
