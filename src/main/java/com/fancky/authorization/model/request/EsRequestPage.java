package com.fancky.authorization.model.request;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Data
public class EsRequestPage extends Page {
    /**
     * 查询的列，废弃不用。请用fieldMap代替
     */
//    private List<String> sourceFieldList;
    /**
     * 导出的列
     */
    private LinkedHashMap<String, String> fieldMap;
    /**
     *bucket  相当于group by
     */
    private List<String> bucketFieldList;
    /**
     * 统计字段
     */
    private List<String> aggregationFieldList;

    private List<Sort> sortFieldList;

    public List<String> getSourceFieldList() {
        if (MapUtils.isNotEmpty(fieldMap)) {
            return new ArrayList<>(fieldMap.keySet());
//            return fieldMap.keySet().stream().collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    public static LinkedHashMap<String, String> setFieldMapByField(List<String> fieldList) {
        LinkedHashMap<String, String> fieldMap = new LinkedHashMap<>();
        if (CollectionUtils.isNotEmpty(fieldList)) {
            for (String item : fieldList) {
                fieldMap.put(item, item);
            }
        }
        return fieldMap;
    }


}
