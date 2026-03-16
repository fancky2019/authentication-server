package com.fancky.authorization.model.request;

import com.fancky.authorization.model.entity.BaseEntity;
import lombok.Data;

import java.io.Serializable;

@Data
public class Page extends BaseEntity implements Serializable {
    private Integer pageSize=10;
    private Integer pageIndex=1;
}
