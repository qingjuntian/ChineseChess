package com.thor.chess;

import android.app.Dialog;
import android.content.Context;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class NetGameDialog extends Dialog {

	protected Context context;

	protected String playerName;

	protected GameListener listener = null;

	protected ArrayList<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();

	protected ListView listView = null;

	public NetGameDialog(Context context) {
		super(context);
	}

	public void setListener(GameListener listener) {
		this.listener = listener;
	}

	public void startDiscovery() {

	}

	protected abstract NetEngine createNetEngine(String playerName, int playerColor);

	public void startNetServer(boolean netServer) {
		final NetEngine engine = createNetEngine(playerName, 0);
		dismiss();
		if (listener != null)
			listener.onCreateEngine(engine, netServer);
	}
}
