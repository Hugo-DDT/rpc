package com.DDT.discovery.impl;

import com.DDT.Constant;
import com.DDT.RpcBootstrap;
import com.DDT.ServiceConfig;
import com.DDT.discovery.AbstractRegistry;
import com.DDT.exceptions.DiscoveryException;
import com.DDT.utils.NetUtils;
import com.DDT.utils.zookeeper.ZookeeperNode;
import com.DDT.utils.zookeeper.ZookeeperUtils;
import com.DDT.watch.UpAndDownWatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooKeeper;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class ZookeeperRegistry extends AbstractRegistry {

    // 维护一个zk实例
    private ZooKeeper zooKeeper;


    public ZookeeperRegistry() {
        this.zooKeeper = ZookeeperUtils.createZookeeper();
    }

    public ZookeeperRegistry(String connectString,int timeout) {
        this.zooKeeper = ZookeeperUtils.createZookeeper(connectString,timeout);
    }

    /**
     * 注册服务
     * @param service 服务的配置内容
     */
    @Override
    public void register(ServiceConfig<?> service) {

        // 服务名称的节点
        String parentNode = "/" + Constant.BASE_PROVIDERS_PATH +"/"+service.getInterface().getName();
        // 建立服务节点这个节点应该是一个持久节点
        if(!ZookeeperUtils.exists(zooKeeper,parentNode,null)){
            ZookeeperNode zookeeperNode = new ZookeeperNode(parentNode,null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.PERSISTENT);
        }



        // 创建本机的临时节点, ip:port ,
        // 服务提供方的端口一般自己设定，我们还需要一个获取ip的方法
        // ip我们通常是需要一个局域网ip，不是127.0.0.1,也不是ipv6
        // 192.168.12.121
        String node = parentNode + "/" + NetUtils.getIp() + ":" + RpcBootstrap.getInstance().getConfiguration().getPort();
        if(!ZookeeperUtils.exists(zooKeeper,node,null)){
            ZookeeperNode zookeeperNode = new ZookeeperNode(node,null);
            ZookeeperUtils.createNode(zooKeeper, zookeeperNode, null, CreateMode.EPHEMERAL);
        }

        if(log.isDebugEnabled()){
            log.debug("服务{}，已经被注册",service.getInterface().getName());
        }
    }

    /**
     * 发现服务地址
     * @param serviceName 服务名称
     * @return 服务列表
     */
    @Override
    public List<InetSocketAddress> lookup(String serviceName) {
        String parentNode = "/" + Constant.BASE_PROVIDERS_PATH +"/"+serviceName;
        List<String> address = ZookeeperUtils.getChildren(zooKeeper, parentNode, new UpAndDownWatcher());
        List<InetSocketAddress> addressList = address.stream().map(s -> {
            String[] split = s.split(":");
            return new InetSocketAddress(split[0], Integer.parseInt(split[1]));
        }).toList();

        if (addressList.size() == 0) {
            throw new DiscoveryException("没有可用的服务");
        }

        return addressList   ;
    }
}
