package com.thor.chess;

import andren.game.china.chess.R;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.pgame.ChessConstants;
import com.pgame.GameStatusListener;
import com.pgame.NetworkProxy;

public class PgameNetGameDialog extends NetGameDialog implements
		GameStatusListener {

	private PgameNetAdapter adapter = null;

	private NetworkProxy netProxy;

	public PgameNetGameDialog(Context context) {
		super(context);
		this.context = context;
	}

	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.arg1 == 1) {
				Bundle bData = msg.getData();
				String[] rooms = (String[]) bData.getCharSequenceArray("rooms");
				for (String room : rooms) {
					String[] roomInfo = room.split(":");
					assert (roomInfo.length == 4);
					data.add(new RoomListItem(roomInfo[0], roomInfo[1], roomInfo[2], roomInfo[3]));
				}
				adapter.notifyDataSetChanged();
				String myself = bData.getString("myself");
				String[] myInfo = myself.split(",");
				ChessApplication.setSetting("Player", myInfo[0]);
				ChessApplication.setSetting("PlayerLevel", myInfo[1]);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pgnet_menu);
		setTitle(R.string.net_game);
		EditText username = (EditText) findViewById(R.id.txt_player_name);
		listView = (ListView) findViewById(R.id.list_games);
		username.setText(ChessApplication.getSetting("Player", ""));

		adapter = new PgameNetAdapter(context, data);
		netProxy = NetworkProxy.createDefaultProxy();
		netProxy.openSocket(ChessConstants.ROOM_SERVER_IP, ChessConstants.ROOM_SERVER_PORT);
		netProxy.registListener(PgameNetGameDialog.this);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				// listView.setTag(position);
				if (!verifyName())
					return;
				startNetServer(false);
				netProxy.openSocket(data.get(position).get("ip"), Integer.parseInt(data.get(position).get("port")));
//				netProxy.openSocket("10.0.2.2", Integer.parseInt(data.get(position).get("port")));
				netProxy.sitDown(playerName);
			}
		});

		// //////////////////////////////////////////////////////////
		// Setup button events
		Button btnDiscovery = (Button) findViewById(R.id.btn_sync_room);
		btnDiscovery.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (!verifyName()) {
					return;
				}
				data.clear();
				adapter.notifyDataSetChanged();
				netProxy.syncGameRoom(playerName);
			}

		});
	}

	private boolean verifyName() {
		EditText username = (EditText) findViewById(R.id.txt_player_name);
		playerName = username.getText().toString().trim();
		byte[] nameBytes = playerName.getBytes();
		if (playerName.length() == 0 || nameBytes.length > 24) {
			Toast toast = Toast.makeText(context, "请输入昵称,别超过8个汗字",
					Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, -100);
			toast.show();
			return false;
		} else
			return true;
	}

	@Override
	public void onRoomInfoResp(String[] rooms, String myself) {
		Message msg = handler.obtainMessage();
		Bundle bData = new Bundle();
		bData.putCharSequenceArray("rooms", rooms);
		bData.putString("myself", myself);
		msg.arg1 = 1;
		msg.setData(bData);
		handler.sendMessage(msg);
	}

	@Override
	protected NetEngine createNetEngine(String playerName, int playerColor) {
		return new PgameNetEngine(netProxy, playerName);
	}
}
