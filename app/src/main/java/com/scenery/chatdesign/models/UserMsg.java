package com.scenery.chatdesign.models;

/**
 * Created by Scenery on 2016/4/24.
 */
public class UserMsg {
    public static final int SEND_MSG = 0;
    public static final int RECEIVE_MSG = 1;
    public static final int SYSTEM_MSG = 2;

    private String Content;
    private int MsgType;

    public UserMsg(String content, int type) {
        Content = content;
        MsgType = type;
    }

    public String getContent() {
        return Content;
    }

    public void setContent(String content) {
        Content = content;
    }

    public int getMsgType() {
        return MsgType;
    }

    public void setMsgType(int type) {
        MsgType = type;
    }
}
