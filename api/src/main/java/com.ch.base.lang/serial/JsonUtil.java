package com.ch.base.lang.serial;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.type.JavaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by he.chen on 5/3/16.
 */
public class JsonUtil {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        objectMapper.configure(JsonParser.Feature.INTERN_FIELD_NAMES, true);
        objectMapper.configure(JsonParser.Feature.CANONICALIZE_FIELD_NAMES, true);
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    }

    public static ObjectMapper getObjectMapperInstance() {
        return objectMapper;
    }
    public static String toJsonString(Object obj){
        try{
            return objectMapper.writeValueAsString(obj);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }
    public static <T> T parseJson(String json, Class<T> tClass) throws IOException {
        return objectMapper.readValue(json, tClass);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T parseJsonGeneric(String json, Class<?> collectionClass, Class<?>... elementClasses) throws IOException {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType( collectionClass, elementClasses);
        try {
            return (T)objectMapper.readValue(json, javaType);
        } catch (IOException e) {
            logger.error("parseJsonGeneric error: {}", json);
            throw e;
        }
    }

    public static <T> T cloneGeneric(Object obj, Class<?> collectionClass, Class<?>... elementClasses ) throws IOException {
        try {
            String jsonStr = toJsonString(obj);
            return (T)parseJsonGeneric(jsonStr, collectionClass, elementClasses);
        } catch (IOException e) {
            logger.error("cloneGeneric error: {}", obj);
            throw e;
        }
    }

}
