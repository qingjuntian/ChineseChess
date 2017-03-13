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

public class PgameNetAdapter extends BaseAdapter {
	private LayoutInflater inflater;
	private ArrayList<HashMap<String, String>> data;

	public PgameNetAdapter(Context context, ArrayList<HashMap<String, String>> data) {
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
//		if (parent.getTag() == null || (Integer)parent.getTag() != position) {
//			// No select any item
//			if (convertView == null || convertView.getTag() != null) {
//				convertView = inflater.inflate(R.layout.list_item_simple, null);
//			}
//			((TextView)convertView).setText(data.get(position).get("id"));
//		} else
		{
			if (convertView == null || convertView.getTag() == null) {
				convertView = inflater.inflate(R.layout.room_list_item, null);
			}
			Integer ival = Integer.valueOf(1);
			convertView.setTag(ival);
			TextView ip = (TextView)convertView.findViewById(R.id.txt_room_id);
			ip.setText(data.get(position).get("id"));
			TextView gameInfo = (TextView)convertView.findViewById(R.id.txt_room_player_num);
			gameInfo.setText(data.get(position).get("playerNum"));
		}
		return convertView;
	}
}
