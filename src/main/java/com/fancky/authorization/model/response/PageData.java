package com.fancky.authorization.model.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PageData<T> {
//    private static final long serialVersionUID = 1L;
    private long count;
    private List<T> data;

//    Lombok默认不处理静态字段：Lombok的@Getter和@Setter注解默认只对实例字段生成方法，不会为静态字段生成静态getter/setter。

//    即使在字段上手动指定@Getter @Setter也不会生成：

    private static volatile PageData instance;

    public static PageData getDefault() {
        if (instance == null) {
            //注意静态方法 锁当前累的class  不是 this
            synchronized (PageData.class) {
                if (instance == null) {
                    instance = new PageData();
                    instance.setCount(0L);
                    instance.setData(new ArrayList());
                }
            }
        }
        return instance;
    }

}
