package com.DDT.loadbalancer.impl;

import com.DDT.RpcBootstrap;
import com.DDT.loadbalancer.AbstractLoadBalancer;
import com.DDT.loadbalancer.Selector;
import com.DDT.transport.message.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * 一致性hash负载均衡算法的实现
 * 1、首先我们需要构建一个hash环，hash环上存储服务器节点的hash值和对应的服务器地址
 * 2、当有请求过来时，我们需要根据请求的一些特征（比如请求的id）进行hash运算，得到一个hash值
 * 3、我们需要在hash环上找到一个节点，这个节点的hash值是大于等于请求hash值的，如果没有找到这样的节点，我们就从hash环的头部开始寻找
 * 4、找到这个节点后，我们就可以将请求发送到这个节点对应的服务器上
 * 5、当有服务器节点上线或者下线时，我们需要重新构建hash环，或者在hash环上添加或者删除对应的节点
 * 6、为了提高hash环的均匀性，我们可以为每个服务器节点添加多个虚拟节点，这些虚拟节点的hash值是根据服务器节点的地址和一个索引生成的，这样可以使得hash环上的节点分布更加均匀，减少请求的倾斜现象
 * 7、需要注意的是，一致性hash算法虽然可以减少请求的倾斜现象，但是在服务器节点上线或者下线时，仍然会有部分请求需要重新分配到新的节点上，这是不可避免的，但是相比于其他负载均衡算法，一致性hash算法可以最大程度地减少这种情况的发生，保证系统的稳定性和性能。
 */

@Slf4j
public class ConsistentHashBalancer extends AbstractLoadBalancer {
    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new ConsistentHashSelector(serviceList,128);
    }

    /**
     * 一致性hash的具体算法实现
     */
    private static class ConsistentHashSelector implements Selector{

        // hash环用来存储服务器节点
        private SortedMap<Integer,InetSocketAddress> circle= new TreeMap<>();
        // 虚拟节点的个数
        private int virtualNodes;

        public ConsistentHashSelector(List<InetSocketAddress> serviceList,int virtualNodes) {
            // 我们应该尝试将节点转化为虚拟节点，进行挂载
            this.virtualNodes = virtualNodes;
            for (InetSocketAddress inetSocketAddress : serviceList) {
                // 需要把每一个节点加入到hash环中
                addNodeToCircle(inetSocketAddress);
            }
        }

        @Override
        public InetSocketAddress getNext() {
            // 1、hash环已经建立好了，接下来需要对请求的要素做处理我们应该选择什么要素来进行hash运算
            // 有没有办法可以获取，到具体的请求内容  --> threadLocal
            RpcRequest rpcRequest = RpcBootstrap.REQUEST_THREAD_LOCAL.get();

            // 我们想根据请求的一些特征来选择服务器  id
            String requestId = Long.toString(rpcRequest.getRequestId());

            // 请求的id做hash，字符串默认的hash不太好
            int hash = hash(requestId);

            // 判断该hash值是否能直接落在一个服务器上，和服务器的hash一样
            if( !circle.containsKey(hash)){
                // 寻找理我最近的一个节点
                SortedMap<Integer, InetSocketAddress> tailMap = circle.tailMap(hash);
                hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
            }

            return circle.get(hash);
        }

        /**
         * 将每个节点挂载到hash环上
         * @param inetSocketAddress 节点的地址
         */
        private void addNodeToCircle(InetSocketAddress inetSocketAddress) {
            // 为每一个节点生成匹配的虚拟节点进行挂载
            for (int i = 0; i < virtualNodes; i++) {
                int hash = hash(inetSocketAddress.toString() + "-" + i);
                // 关在到hash环上
                circle.put(hash,inetSocketAddress);
                if(log.isDebugEnabled()){
                    log.debug("hash为[{}]的节点已经挂载到了哈希环上.",hash);
                }
            }
        }

        private void removeNodeFromCircle(InetSocketAddress inetSocketAddress) {
            // 为每一个节点生成匹配的虚拟节点进行挂载
            for (int i = 0; i < virtualNodes; i++) {
                int hash = hash(inetSocketAddress.toString() + "-" + i);
                // 关在到hash环上
                circle.remove(hash);
            }
        }

        /**
         * 具体的hash算法, todo 小小的遗憾，这样也是不均匀的
         * @param s
         * @return
         */
        private int hash(String s) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            byte[] digest = md.digest(s.getBytes());
            // md5得到的结果是一个字节数组，但是我们想要int 4个字节

            int res = 0;
            for (int i = 0; i < 4; i++) {
                res = res << 8;
                if( digest[i] < 0 ){
                    res = res | (digest[i] & 255);
                } else {
                    res = res | digest[i];
                }
            }
            return res;
        }



        private String toBinary(int i){
            String s = Integer.toBinaryString(i);
            int index = 32 -s.length();
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < index; j++) {
                sb.append(0);
            }
            sb.append(s);
            return sb.toString();
        }
    }
}
