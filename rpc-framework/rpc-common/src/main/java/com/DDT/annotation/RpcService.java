package com.DDT.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RpcService 注解用于标记一个类的字段为 RPC 服务接口的引用，表示该字段需要被自动注入对应的 RPC 服务代理对象。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcService {
}
