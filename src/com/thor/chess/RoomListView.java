package com.thor.chess;

import java.util.ArrayList;
import java.util.HashMap;

import andren.game.china.chess.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.pgame.GameStatusListener;
import com.pgame.NetworkProxy;

/**
 * 2013-6-30
 */
public class RoomListView extends ListView implements GameStatusListener {

	private PgameNetAdapter adapter = null;

	protected ArrayList<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();

	private GameListener gameListener;

	private String playerName;

	public RoomListView(Context context) {
		super(context);
		adapter = new PgameNetAdapter(context, data);
		this.setBackgroundResource(R.drawable.face);
		setAdapter(adapter);
		setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {

				NetworkProxy proxy = NetworkProxy.createDefaultProxy();
				final NetEngine engine = new PgameNetEngine(proxy, playerName);
				gameListener.onCreateEngine(engine, false);
				proxy.openSocket(data.get(position).get("ip"),
						Integer.parseInt(data.get(position).get("port")));
				proxy.sitDown(playerName);
			}
		});
	}

	@Override
	public void onRoomInfoResp(final String[] rooms, final String myself) {
		post(new Runnable() {
			@Override
			public void run() {
				for (String room : rooms) {
					String[] roomInfo = room.split(":");
					assert (roomInfo.length == 4);
					data.add(new RoomListItem(roomInfo[0], roomInfo[1],
							roomInfo[2], roomInfo[3]));
				}
				adapter.notifyDataSetChanged();
				String[] myInfo = myself.split(",");
				ChessApplication.setSetting("Player", myInfo[0]);
				ChessApplication.setSetting("PlayerLevel", myInfo[1]);
			}
		});
	}

	public void clear() {
		data.clear();
		adapter.notifyDataSetChanged();
	}

	public void init(GameListener gameListener) {
		this.gameListener = gameListener;
	}

	public void clear(String playerName) {
		this.playerName = playerName;
		data.clear();
	}

}