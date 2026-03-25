package com.DDT.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 服务调用方发起的请求内容
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RpcRequest{
    // 请求id
    private long requestId;

    // 请求的类型，压缩的类型，序列化的方式
    private byte requestType;
    private byte compressType;
    private byte serializeType;

    // 请求的具体消息体
    private RequestPayload requestPayload;
}
