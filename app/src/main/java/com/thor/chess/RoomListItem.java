package com.thor.chess;

import java.util.HashMap;

public class RoomListItem extends HashMap<String, String> {

	private static final long serialVersionUID = -891461887415985079L;

	public RoomListItem(String id, String ip, String port, String playerNum) {
		put("id", id);
		put("ip", ip);
		put("port", port);
		put("playerNum", playerNum);
	}
}