package com.DDT.serialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SerializerWrapper是一个包装类，用于封装序列化器的相关信息，包括序列化器的编码、类型和实例。
 * 这个类的主要作用是提供一个统一的接口来管理不同类型的序列化器，使得在使用过程中可以方便地根据编码或类型来获取对应的序列化器实例。
 * 通过使用SerializerWrapper，可以实现对不同序列化器的统一管理和调用，增强代码的灵活性和可维护性。
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SerializerWrapper {
    private byte code;
    private String type;
    private Serializer serializer;
}