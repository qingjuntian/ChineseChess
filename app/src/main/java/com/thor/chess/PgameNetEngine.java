package com.thor.chess;

import com.pgame.NetworkProxy;

import java.io.IOException;
import java.util.Iterator;

public class PgameNetEngine extends NetEngine {
	private NetworkProxy proxy;

	public PgameNetEngine(NetworkProxy netProxy, String playerName) {
		super(playerName);
		this.proxy = netProxy;
		proxy.setEngine(this);
	}

	@Override
	protected void initInputOutput(boolean isServer, int playerColor,
			String playerName) throws IOException {
	}

	@Override
	protected Iterator<NetCommand> readCommand() {
		return null;
	}

	@Override
	public void connect(IConnectListener listener) {

	}

	@Override
	public void listen(final IAcceptListener listener) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				listener.onCompleted(true);
			}
		});
		thread.start();
	}

	@Override
	public void stopListen() {

	}

	@Override
	public void hand() {
		proxy.hand(getPlayerName());
	}

	@Override
	public void leaveRoom() {
		proxy.leaveRoom(getPlayerName());
	}

	@Override
	public void changeSeat() {
		proxy.sitDown(getPlayerName());
	}

	@Override
	public void resetNetGame() {
		proxy.resetNetGame(getPlayerName());
	}

	@Override
	protected void sendToPartner(String msg) {
		StringBuffer sb = new StringBuffer();
		sb.append("game:").append(getPlayerName()).append(":").append(msg);
		proxy.sendRequest(sb.toString());
	}
}
