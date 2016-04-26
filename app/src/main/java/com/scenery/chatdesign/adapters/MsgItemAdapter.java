package com.scenery.chatdesign.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.scenery.chatdesign.R;
import com.scenery.chatdesign.models.UserMsg;

import java.util.ArrayList;

/**
 * Created by Scenery on 2016/4/24.
 */
public class MsgItemAdapter extends BaseAdapter {
    private ArrayList<UserMsg> msgArrayList = new ArrayList<>();
    private Context mContext;

    public MsgItemAdapter(Context context, ArrayList<UserMsg> list) {
        super();
        mContext = context;
        msgArrayList = list;
    }

    @Override
    public int getCount() {
        return msgArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return msgArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View resultView = null;
        UserMsg userMsg = (UserMsg) msgArrayList.get(position);
        
        if (userMsg.getIsSend()) {
            resultView = LayoutInflater.from(mContext).inflate(R.layout.send_msg_item, null);
        } else {
            resultView = LayoutInflater.from(mContext).inflate(R.layout.receive_msg_item, null);
        }
        TextView text = (TextView) resultView.findViewById(R.id.MsgContent);
        text.setText(userMsg.getContent());
        return resultView;
    }
}
