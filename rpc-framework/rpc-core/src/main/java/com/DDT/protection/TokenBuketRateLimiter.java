package com.DDT.protection;

import lombok.extern.slf4j.Slf4j;

/**
 * 令牌桶算法的限流器实现
 * 1、令牌桶算法的核心思想是：系统以固定的速率向一个桶中添加令牌，每个请求需要获取一个令牌才能被处理，如果桶中没有令牌，请求就会被拒绝或者等待直到有令牌可用。
 */
@Slf4j
public class TokenBuketRateLimiter implements RateLimiter {

    // 令牌桶的令牌，如果没了要怎么办？ 按照一定的速率给令牌桶加令牌,如每秒加500个，不能超过总数
    // 可以用定时任务去加--> 启动一个定时任务，每秒执行一次 tokens+500 不能超过 capacity
    // 对于单机版的限流器可以有更简单的操作，每一个有请求要发送的时候给他加一下就好了

    // 令牌数，>0 有令牌，能放行，放行就减一；==0,无令牌  阻拦
    private int tokens;

    // 限流的本质就是，令牌数
    private final int capacity;

    // 每秒钟放多少个令牌
    private final int rate;

    // 上一次放令牌的时间
    private Long lastTokenTime;

    public TokenBuketRateLimiter(int capacity, int rate) {
        this.capacity = capacity;
        this.rate = rate;
        lastTokenTime = System.currentTimeMillis();
        tokens = capacity;
    }

    /**
     * 判断请求是否可以放行
     * @return true 放行  false  拦截
     */
    public synchronized boolean allowRequest() {
        // 1、给令牌桶添加令牌
        // 计算从现在到上一次的时间间隔需要添加的令牌数
        Long currentTime = System.currentTimeMillis();
        long timeInterval = currentTime - lastTokenTime;
        // 如果间隔时间超过一秒，放令牌
        if(timeInterval >= 1000 / rate){
            int needAddTokens = (int)(timeInterval * rate / 1000);
            System.out.println("needAddTokens = " + needAddTokens);
            // 给令牌桶添加令牌
            tokens = Math.min(capacity, tokens + needAddTokens);
            System.out.println("tokens = " + tokens);

            // 标记最后一个放入令牌的时间
            this.lastTokenTime = System.currentTimeMillis();
        }

        // 2、自己获取令牌,如果令牌桶中有令牌则放行，否则拦截
        if(tokens > 0){
            tokens --;
            System.out.println("---------------请求被放行---------------");
            return true;
        } else {
            System.out.println("---------------请求被拦截---------------");
            return false;
        }

    }
}
