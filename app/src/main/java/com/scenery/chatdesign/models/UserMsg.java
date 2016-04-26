package com.scenery.chatdesign.models;

/**
 * Created by Scenery on 2016/4/24.
 */
public class UserMsg {
    private String Content;
    private Boolean isSend;

    public UserMsg(String content, Boolean _isSend) {
        Content = content;
        isSend = _isSend;
    }

    public String getContent() {
        return Content;
    }

    public void setContent(String content) {
        Content = content;
    }

    public Boolean getIsSend() {
        return isSend;
    }

    public void setIsSend(Boolean _isSend) {
        isSend = _isSend;
    }
}
