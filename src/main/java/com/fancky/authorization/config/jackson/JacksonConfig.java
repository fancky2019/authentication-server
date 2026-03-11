package com.fancky.authorization.config.jackson;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 *  @JsonIgnore //jackson 不序列化 反序列化
 *   @JsonProperty("XCode") 字段映射
 *
 *       @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")  // 用于 Spring 接收前端传入的字符串日期
 *     @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")  // 用于返回给前端时格式化 JSON 输出
 */
@Configuration
public class JacksonConfig {

    /*
     如果只用 @Autowired，Spring 会根据类型自动注入 Bean。
     如果存在多个同类型的 Bean（例如两个 ObjectMapper），
     Spring 会根据 @Primary 来决定使用哪个。除非 @Autowired + @Qualifier("nonEmptyObjectMapper")
     指定bean 名称

     当只使用 @Autowired 时，Spring 会按类型 (ObjectMapper) 查找
    找到多个同类型 Bean 时，Spring 优先选择 @Primary 的 Bean
    所以即使字段名是 nonEmptyObjectMapper，也会注入 @Primary 的 objectMapper()
     */


    /**
    多个类型相同的bean最好为每个bean 指定 @Qualifier
     * 时区在配置文件中配置
     * @return
     */
//    @Qualifier("objectMapper")//最好指定名称
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
//        ZonedDateTime
        // 设置时区为 GMT+8   UTC
        //LocalDateTime 是一个不带时区的日期时间类型，它只表示一个本地时间。
        //当你将 LocalDateTime 序列化为 JSON 时，Jackson 会直接将其值输出，而不会进行任何时区转换。
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        // 关闭时间戳模式:jackson ZonedDateTime  默认序列化是时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        全局配置忽略实体中不存在的字段  _class 字段
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
// 使用小写字母开头的命名策略 LOWER_CAMEL_CASE
//        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
        //默认JsonInclude.Include.ALWAYS
//        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        // 日期和时间格式化
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));


        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));

        // 注册自定义的序列化和反序列化器
        javaTimeModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        javaTimeModule.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
//        javaTimeModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
//        javaTimeModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(DateTimeFormatter.ISO_ZONED_DATE_TIME));

//        SimpleModule module = new SimpleModule();
        // "" 不序列化成null
        javaTimeModule.addSerializer(String.class, new JsonSerializer<String>() {
            @Override
            public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
//                if (value == null || value.isEmpty()) {
                if (value == null) {
                    gen.writeNull(); // 序列化为 null
                } else {
                    gen.writeString(value); // 正常序列化
                }
            }
        });
//        objectMapper.registerModule(module);


        objectMapper.registerModule(javaTimeModule);
//        //空处理成“”
//        objectMapper.getSerializerProvider().setNullValueSerializer(new JsonSerializer<Object>() {
//            @Override
//            public void serialize(Object paramT, JsonGenerator paramJsonGenerator,
//                                  SerializerProvider paramSerializerProvider) throws IOException {
//                //设置返回null转为 空字符串""
//                paramJsonGenerator.writeString("");
//            }
//        });

        /*
        //1.实体上

        @JsonInclude(Include.NON_NULL)

        //将该标记放在属性上，如果该属性为NULL则不参与序列化
        //如果放在类上边,那对这个类的全部属性起作用
        //Include.Include.ALWAYS 默认
        //Include.NON_DEFAULT 属性为默认值不序列化
        //Include.NON_EMPTY 属性为 空（“”） 或者为 NULL 都不序列化
        //Include.NON_NULL 属性为NULL 不序列化

        //2.代码上
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);

        //通过该方法对mapper对象进行设置，所有序列化的对象都将按改规则进行系列化
        //Include.Include.ALWAYS 默认
        //Include.NON_DEFAULT 属性为默认值不序列化
        //Include.NON_EMPTY 属性为 空（“”） 或者为 NULL 都不序列化
        //Include.NON_NULL 属性为NULL 不序列化

         */

        //json 序列化null值。字符串“” 序列化成null, 默认序列化：  "serialNo": null,
//        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//
//       //忽略未知字段 ,不然字段不匹配会报错
//        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//
//
//
//
//        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        objectMapper.configure(MapperFeature.USE_ANNOTATIONS, false);
//       // objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
//        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
//       // objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//
////        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
////        objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
////        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        return objectMapper;

    }


    @Bean("upperObjectMapper")  // 指定特定名称
    public ObjectMapper upperObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
//        ZonedDateTime
        // 设置时区为 GMT+8   UTC
        //LocalDateTime 是一个不带时区的日期时间类型，它只表示一个本地时间。
        //当你将 LocalDateTime 序列化为 JSON 时，Jackson 会直接将其值输出，而不会进行任何时区转换。
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        // 关闭时间戳模式:jackson ZonedDateTime  默认序列化是时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        全局配置忽略 _class 字段
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
// 使用小写字母开头的命名策略 LOWER_CAMEL_CASE
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);

        // 日期和时间格式化
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        //后注册的会覆盖前面的
        // 统一由我们自定义的反序列化器处理
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeUniversalDeserializer());
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));

        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
//        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
//        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
//        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));

        // 注册自定义的序列化和反序列化器
        javaTimeModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        javaTimeModule.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
//        javaTimeModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
//        javaTimeModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(DateTimeFormatter.ISO_ZONED_DATE_TIME));

//        SimpleModule module = new SimpleModule();
        // "" 不序列化成null
        javaTimeModule.addSerializer(String.class, new JsonSerializer<String>() {
            @Override
            public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
//                if (value == null || value.isEmpty()) {
                if (value == null) {
                    gen.writeNull(); // 序列化为 null
                } else {
                    gen.writeString(value); // 正常序列化
                }
            }
        });
//        objectMapper.registerModule(module);


        objectMapper.registerModule(javaTimeModule);
//        //空处理成“”
//        objectMapper.getSerializerProvider().setNullValueSerializer(new JsonSerializer<Object>() {
//            @Override
//            public void serialize(Object paramT, JsonGenerator paramJsonGenerator,
//                                  SerializerProvider paramSerializerProvider) throws IOException {
//                //设置返回null转为 空字符串""
//                paramJsonGenerator.writeString("");
//            }
//        });

        /*
        //1.实体上

        @JsonInclude(Include.NON_NULL)

        //将该标记放在属性上，如果该属性为NULL则不参与序列化
        //如果放在类上边,那对这个类的全部属性起作用
        //Include.Include.ALWAYS 默认
        //Include.NON_DEFAULT 属性为默认值不序列化
        //Include.NON_EMPTY 属性为 空（“”） 或者为 NULL 都不序列化
        //Include.NON_NULL 属性为NULL 不序列化

        //2.代码上
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);

        //通过该方法对mapper对象进行设置，所有序列化的对象都将按改规则进行系列化
        //Include.Include.ALWAYS 默认
        //Include.NON_DEFAULT 属性为默认值不序列化
        //Include.NON_EMPTY 属性为 空（“”） 或者为 NULL 都不序列化
        //Include.NON_NULL 属性为NULL 不序列化

         */

        //json 序列化null值。字符串“” 序列化成null, 默认序列化：  "serialNo": null,
//        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//
//       //忽略未知字段 ,不然字段不匹配会报错
//        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//
//
//
//
//        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        objectMapper.configure(MapperFeature.USE_ANNOTATIONS, false);
//       // objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
//        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
//       // objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//
////        objectMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
////        objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
////        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        return objectMapper;

    }

    /**
     *     @Autowired
     *     @Qualifier("nonEmptyObjectMapper") //注意 加上    @Qualifier("nonEmptyObjectMapper") ,否则应用的是objectMapper()
     *     public ObjectMapper nonEmptyObjectMapper;
     * @return
     */

    @Bean("nonEmptyObjectMapper")
    public ObjectMapper nonEmptyObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
//        ZonedDateTime
        // 设置时区为 GMT+8   UTC
        //LocalDateTime 是一个不带时区的日期时间类型，它只表示一个本地时间。
        //当你将 LocalDateTime 序列化为 JSON 时，Jackson 会直接将其值输出，而不会进行任何时区转换。
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        // 关闭时间戳模式:jackson ZonedDateTime  默认序列化是时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        全局配置忽略实体中不存在的字段  _class 字段
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
// 使用小写字母开头的命名策略 LOWER_CAMEL_CASE
//        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
        //不序列化空值 ：默认JsonInclude.Include.ALWAYS
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);


        //        不能启用类型信息，否则  objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);  配置不生效
        //@CLASS 信息
        //objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
//        不能启用类型信息，
//        objectMapper.deactivateDefaultTyping();

        //属性字段可见性
//        默认情况下，Jackson 的可见性规则是： 只检测 public 的字段和方法 需要 getter/setter 方法来访问 private 字段
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        //类型信息，不认反序列化会转成LinkedHashMap
//        启用默认类型信息，在序列化 JSON 时添加类型信息，用于反序列化时识别具体类型。
//        默认情况下，Jackson 不启用 DefaultTyping，即： 不添加类型信息 反序列化时可能丢失多态类型信息
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        // 日期和时间格式化
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));

        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));

        // 注册自定义的序列化和反序列化器
        javaTimeModule.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        javaTimeModule.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        objectMapper.registerModule(javaTimeModule);
        return objectMapper;

    }
}
