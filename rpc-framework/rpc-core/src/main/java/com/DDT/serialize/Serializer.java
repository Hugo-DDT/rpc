package com.DDT.serialize;

/**
 * 序列化器的抽象
 */
public interface Serializer {

    /**
     * 序列化方法
     * @param obj 待序列化的对象实例
     * @return 序列化后的字节数组
     */
    public byte[] serialize(Object obj);

    /**
     * 反序列化方法
     * @param bytes 带反序列化的字节数组
     * @param clazz 目标类的class对象
     * @return 反序列化后的对象实例
     * @param <T> 目标类泛型
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
