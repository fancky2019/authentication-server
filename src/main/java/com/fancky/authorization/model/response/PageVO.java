package com.fancky.authorization.model.response;


import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

@Data
public class PageVO<T> {

    private Long total;
    private Long pageSize;
    private Long currentPage;
    private Long totalPages;
    private List<T> records;

    public static <T> PageVO<T> build(IPage<T> page) {
        PageVO<T> pageVO = new PageVO<>();
        pageVO.setTotal(page.getTotal());
        pageVO.setPageSize(page.getSize());
        pageVO.setCurrentPage(page.getCurrent());
        pageVO.setTotalPages(page.getPages());
        pageVO.setRecords(page.getRecords());
        return pageVO;
    }
}
