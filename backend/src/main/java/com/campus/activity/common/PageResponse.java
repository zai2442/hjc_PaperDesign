package com.campus.activity.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {
    private long total;
    private long page;
    private long size;
    private List<T> records;

    public static <T> PageResponse<T> of(long page, long size, long total, List<T> records) {
        PageResponse<T> resp = new PageResponse<>();
        resp.setPage(page);
        resp.setSize(size);
        resp.setTotal(total);
        resp.setRecords(records);
        return resp;
    }

    public static <T> PageResponse<T> from(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> mpPage) {
        return of(mpPage.getCurrent(), mpPage.getSize(), mpPage.getTotal(), mpPage.getRecords());
    }
}
