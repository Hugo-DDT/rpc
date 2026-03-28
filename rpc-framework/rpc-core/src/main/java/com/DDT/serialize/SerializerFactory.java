package com.DDT.serialize;

import com.DDT.config.ObjectWrapper;
import com.DDT.serialize.impl.HessianSerializer;
import com.DDT.serialize.impl.JdkSerializer;
import com.DDT.serialize.impl.JsonSerializer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化器工厂类，使用工厂方法模式创建ObjectWrapperr实例。
 * 该工厂类维护了一个缓存，存储了不同类型的序列化器实例，以便在需要时快速获取对应的序列化器。
 * 通过使用SerializerFactory，用户可以根据序列化类型或编码来获取相应的ObjectWrapper实例，从而实现对不同序列化器的统一管理和调用。
 * 这种设计模式提高了代码的灵活性和可维护性，使得在添加新的序列化器时只需要在工厂类中进行简单的注册，而不需要修改其他部分的代码。
 */
@Slf4j
public class SerializerFactory {
    private final static ConcurrentHashMap<String, ObjectWrapper<Serializer>> SERIALIZER_CACHE = new ConcurrentHashMap<>(8);
    private final static ConcurrentHashMap<Byte, ObjectWrapper<Serializer>> SERIALIZER_CACHE_CODE = new ConcurrentHashMap<>(8);

    static {
        ObjectWrapper<Serializer> jdk = new  ObjectWrapper<>((byte) 1, "jdk", new JdkSerializer());
        ObjectWrapper<Serializer> json = new  ObjectWrapper<>((byte) 2, "json", new JsonSerializer());
        ObjectWrapper<Serializer> hessian = new  ObjectWrapper<>((byte) 3, "hessian", new HessianSerializer());
        SERIALIZER_CACHE.put("jdk",jdk);
        SERIALIZER_CACHE.put("json",json);
        SERIALIZER_CACHE.put("hessian",hessian);

        SERIALIZER_CACHE_CODE.put((byte) 1, jdk);
        SERIALIZER_CACHE_CODE.put((byte) 2, json);
        SERIALIZER_CACHE_CODE.put((byte) 3, hessian);
    }

    /**
     * 使用工厂方法获取一个SerializerWrapper
     * @param serializeType 序列化的类型
     * @return SerializerWrapper
     */
    public static  ObjectWrapper<Serializer> getSerializer(String serializeType) {
        ObjectWrapper<Serializer> serializerWrapper = SERIALIZER_CACHE.get(serializeType);
        if(serializerWrapper == null){
            log.error("未找到您配置的【{}】序列化工具，默认选用jdk的序列化方式。",serializeType);
            return SERIALIZER_CACHE.get("jdk");
        }
        return SERIALIZER_CACHE.get(serializeType);
    }

    public static  ObjectWrapper<Serializer> getSerializer(Byte serializeCode) {
        ObjectWrapper<Serializer> serializerWrapper = SERIALIZER_CACHE_CODE.get(serializeCode);
        if(serializerWrapper == null){
            log.error("未找到您配置的【{}】序列化工具，默认选用jdk的序列化方式。",serializeCode);
            return SERIALIZER_CACHE.get("jdk");
        }
        return SERIALIZER_CACHE_CODE.get(serializeCode);
    }

    /**
     * 新增一个新的序列化器
     * @param serializerObjectWrapper 序列化器的包装
     */
    public static void addSerializer(ObjectWrapper<Serializer> serializerObjectWrapper){
        SERIALIZER_CACHE.put(serializerObjectWrapper.getName(),serializerObjectWrapper);
        SERIALIZER_CACHE_CODE.put(serializerObjectWrapper.getCode(),serializerObjectWrapper);
    }
}
