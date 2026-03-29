package com.DDT.protection;

import java.util.concurrent.atomic.AtomicInteger;

public class CircuitBreaker {
    // 理论上：标准的断路器应该有三种状态  open close half_open，
    // 为了简单只选取两种
    private volatile boolean isOpen = false;

    // 需要搜集指标  异常的数量   比例
    // 总的请求数
    private AtomicInteger requestCount = new AtomicInteger(0);

    // 异常的请求数
    private AtomicInteger errorRequest = new AtomicInteger(0);

    // 异常的阈值
    private int maxErrorRequest;
    private float maxErrorRate;

    public CircuitBreaker(int maxErrorRequest, float maxErrorRate) {
        this.maxErrorRequest = maxErrorRequest;
        this.maxErrorRate = maxErrorRate;
    }

    // 断路器的核心方法，判断是否开启
    public boolean isBreak(){
        // 优先返回，如果已经打开了，就直接返回true
        if(isOpen){
            return true;
        }

        // 需要判断数据指标，是否满足当前的阈值
        if( errorRequest.get() > maxErrorRequest ){
            this.isOpen = true;
            return true;
        }

        if( errorRequest.get() > 0 && requestCount.get() > 0 &&
                errorRequest.get()/(float)requestCount.get() > maxErrorRate
        ) {
            this.isOpen = true;
            return true;
        }

        return false;
    }

    // 每次发生请求，获取发生异常应该进行记录
    public void recordRequest(){
        this.requestCount.getAndIncrement();
    }

    public void recordErrorRequest(){
        this.errorRequest.getAndIncrement();
    }

    /**
     * 重置熔断器
     */
    public void reset(){
        this.isOpen = false;
        this.requestCount.set(0);
        this.errorRequest.set(0);
    }
}
