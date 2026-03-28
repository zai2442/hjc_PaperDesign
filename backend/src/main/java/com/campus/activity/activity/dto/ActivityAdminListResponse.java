package com.campus.activity.activity.dto;

import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.entity.Tag;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityAdminListResponse extends Activity {
    private List<Tag> tags;

    public static ActivityAdminListResponse from(Activity activity, List<Tag> tags) {
        ActivityAdminListResponse resp = new ActivityAdminListResponse();
        BeanUtils.copyProperties(activity, resp);
        resp.setTags(tags);
        return resp;
    }
}
