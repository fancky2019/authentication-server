package com.fancky.authorization.init;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
//import java.util.Properties;


//容器初始化完成执行：ApplicationRunner-->CommandLineRunner-->ApplicationReadyEvent

/**
 * Flink DataStream API 方式
 *
 *
 * @author lirui
 */
//@Order控制配置类的加载顺序，通过@Order指定执行顺序，值越小，越先执行
@Component
@Order(1)
@Slf4j
public class CommandLineImp implements CommandLineRunner {



    @Override
    public void run(String... args) throws Exception {

    }

}
