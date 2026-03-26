package com.campus.activity.activity.dto;

import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.entity.Tag;
import lombok.Data;

import java.util.List;

@Data
public class ActivityAdminResponse {
    private Activity activity;
    private List<Tag> tags;

    public static ActivityAdminResponse of(Activity activity, List<Tag> tags) {
        ActivityAdminResponse res = new ActivityAdminResponse();
        res.setActivity(activity);
        res.setTags(tags);
        return res;
    }
}
