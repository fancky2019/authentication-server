package com.fancky.authorization.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;

import java.time.LocalDateTime;

public class LogicDeleteEntity extends BaseEntity {

    /**
     * MyBatis-Plus 的逻辑删除注解 @TableLogic
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    private LocalDateTime deleteTime;
}
