package com.scenery.chatdesign.adapters;

import android.content.Context;
import android.text.Layout;
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
    // private Context mContext;
    private LayoutInflater mInflater;

    public MsgItemAdapter(Context context, ArrayList<UserMsg> list) {
        super();
        mInflater = LayoutInflater.from(context);
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
        ViewHolder holder = null;
        UserMsg userMsg = msgArrayList.get(position);
        if (convertView == null) {
            holder = new ViewHolder();
            switch (userMsg.getMsgType()) {
                case UserMsg.SEND_MSG:
                    convertView = mInflater.inflate(R.layout.send_msg_item, null);
                    break;
                case UserMsg.RECEIVE_MSG:
                    convertView = mInflater.inflate(R.layout.receive_msg_item, null);
                    break;
                case UserMsg.SYSTEM_MSG:
                    convertView = mInflater.inflate(R.layout.system_msg_item, null);
                    break;
            }
            holder.textView = (TextView) convertView.findViewById(R.id.MsgContent);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textView.setText(userMsg.getContent());
        return convertView;
    }

    public static class ViewHolder {
        public TextView textView;
    }
}
