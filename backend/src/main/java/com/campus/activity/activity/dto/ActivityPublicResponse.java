package com.campus.activity.activity.dto;

import com.campus.activity.activity.entity.Activity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityPublicResponse extends Activity {
    private String registrationStatus;
    private Long registrationId;

    public static ActivityPublicResponse from(Activity activity, String status, Long regId) {
        ActivityPublicResponse resp = new ActivityPublicResponse();
        BeanUtils.copyProperties(activity, resp);
        resp.setRegistrationStatus(status);
        resp.setRegistrationId(regId);
        return resp;
    }
}
