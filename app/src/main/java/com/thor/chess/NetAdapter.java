package com.thor.chess;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import andren.game.china.chess.R;

public class NetAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private ArrayList<HashMap<String, String>> data;

	public NetAdapter(Context context, ArrayList<HashMap<String, String>> data) {
        inflater = LayoutInflater.from(context);
        this.data = data;
    }

	public int getCount() {
		return data.size();
	}

	public Object getItem(int arg0) {
		return null;
	}

	public long getItemId(int arg0) {
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		if (parent.getTag() == null || (Integer)parent.getTag() != position) {
			// No select any item
			if (convertView == null || convertView.getTag() != null) {
				convertView = inflater.inflate(R.layout.list_item_simple, null);
			}
			((TextView)convertView).setText(data.get(position).get("title"));
		} else {
			if (convertView == null || convertView.getTag() == null) {
				convertView = inflater.inflate(R.layout.list_item, null);
			}
			Integer ival = Integer.valueOf(1);
			convertView.setTag(ival);
			TextView title = (TextView)convertView.findViewById(R.id.txt_host_title);
			title.setText(data.get(position).get("title"));
			TextView ip = (TextView)convertView.findViewById(R.id.txt_host_ip);
			ip.setText(data.get(position).get("ip"));
			TextView gameInfo = (TextView)convertView.findViewById(R.id.txt_game_info);
			gameInfo.setText(data.get(position).get("info"));
		}
		return convertView;
	}
}
