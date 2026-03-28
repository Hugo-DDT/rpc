package com.DDT.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 请求载荷，封装了接口全类名、方法名、参数类型、参数值、返回值类型等信息
 * 请求调用方所请求的接口方法的描述
 * hellorpc.sayHi("你好");
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestPayload implements Serializable {
    // 接口全类名 -- com.DDT.HelloRpc
    private String interfaceName;

    // 方法名 -- sayHi
    private String methodName;

    // 参数类型，用来重载方法 -- {java.lang.String}
    private Class<?>[] parametersType;

    // 具体参数，用来执行方法调用 -- “你好”
    private Object[] parametersValue;

    // 返回值的封装 -- {java.long.String}
    private Class<?> returnType;
}
