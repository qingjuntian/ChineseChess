package com.pgame;

import com.thor.chess.NetEngine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkProxy implements Runnable {
	private static final String COMMAND_SEPARATOR = "\\|";

	private String REQUEST_SITDOWN = "sitdown:";

	private String REQUEST_STANDUP = "standup:";

	private String REQUEST_LEAVEROOM = "leaveroom:";

	private String REQUEST_RESET_SYNC = "resetsync:";

	private String REQUEST_HAND = "hand:";

	private String REQUEST_ROOM_LIST = "roomlist:";

	private final static Matcher respStartGame = Pattern.compile(
			"startGame:(.+):(.+)").matcher("");

	private final static Matcher syncRoom = Pattern.compile(
			"syncRoom:(.+);Player:(.+)").matcher("");

	private final static Matcher playerJoin = Pattern
			.compile("playerJoin:(.*)").matcher("");

	private final static Matcher playerLeave = Pattern.compile(
			"playerLeave:(.+)").matcher("");

	private final static Matcher playerReady = Pattern.compile(
			"playerReady:(.+)").matcher("");

	private final static Matcher gameResult = Pattern.compile(
			"gameresult:(.+):(.+)").matcher("");

	private static NetworkProxy proxy;

	private SocketChannel channel;

	private boolean running;

	private GameStatusListener listener;

	private NetEngine netEngine;

	public void sitDown(String player) {
		sendRequest(REQUEST_SITDOWN + player);
	}

	public void standup(String player) {
		sendRequest(REQUEST_STANDUP + player);
	}

	public void leaveRoom(String player) {
		sendRequest(REQUEST_STANDUP + player);
	}

	public void syncGameRoom(String player) {
		sendRequest(REQUEST_ROOM_LIST + player);
	}

	public void hand(String player) {
		sendRequest(REQUEST_HAND + player);
	}

	public void resetNetGame(String player) {
		sendRequest(REQUEST_RESET_SYNC + player);
	}

	public static NetworkProxy createDefaultProxy() {
		if (proxy == null) {
			proxy = new NetworkProxy();
			new Thread(proxy).start();
		}
		return proxy;
	}

	public void leaveGame() {
		try {
			if (channel != null) {
				channel.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			channel = null;
		}
	}

	private NetworkProxy() {
		running = true;
	}

	public void openSocket(String ip, int port) {
		try {
			synchronized (proxy) {
				if (channel != null) {
					channel.close();
				}
				channel = SocketChannel.open();
				channel.connect(new InetSocketAddress(ip, port));
				proxy.notify();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendRequest(final String req) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					ByteBuffer buf = ByteBuffer.allocate(48);
					buf.put(req.getBytes());
					buf.flip();
					while (buf.hasRemaining()) {
						channel.write(buf);
					}
					buf.clear();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void processResponse(String response) {
		System.out.println(response);

		String[] commands = response.split(COMMAND_SEPARATOR);
		for (String resp : commands) {
			respStartGame.reset(resp);
			if (respStartGame.matches()) {
				int playerColor = Integer.parseInt(respStartGame.group(1));
				String[] parterner = respStartGame.group(2).split(",");
				assert (parterner.length == 1);
				netEngine.setPartnerName(parterner[0]);
				netEngine.startGame(playerColor);
				return;
			}

			syncRoom.reset(resp);
			if (syncRoom.matches()) {
				String rooms = syncRoom.group(1);
				String myself = syncRoom.group(2);
				String[] roomInfo = rooms.split(",");
				listener.onRoomInfoResp(roomInfo, myself);
				return;
			}

			playerJoin.reset(resp);
			if (playerJoin.matches()) {
				resp = playerJoin.group(1);
				netEngine.joinPlayer(resp);
				return;
			}

			playerLeave.reset(resp);
			if (playerLeave.matches()) {
				String player = playerLeave.group(1);
				netEngine.leavePlayer(player);
				return;
			}

			playerReady.reset(resp);
			if (playerReady.matches()) {
				String player = playerReady.group(1);
				netEngine.playerReady(player);
				return;
			}

			gameResult.reset(resp);
			if (gameResult.matches()) {
				String result = gameResult.group(1);
				String score = gameResult.group(2);
				netEngine.finishGame(result, score);
			}

			if (netEngine != null) {
				netEngine.processResp(resp);
			}
		}
	}

	@Override
	public void run() {
		while (running) {
			try {
				synchronized (proxy) {
					while (channel == null) {
						proxy.wait();
					}
				}
				ByteBuffer buffer = ByteBuffer.allocate(256);
				byte[] barr = new byte[256];
				int read = channel.read(buffer);
				if (read > 0) {
					buffer.flip();
					buffer.get(barr, 0, read);
					buffer.clear();
					String resp = new String(barr, 0, read, "UTF-8");
					processResponse(resp);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void registListener(GameStatusListener listener) {
		this.listener = listener;
	}

	public void setEngine(NetEngine engine) {
		this.netEngine = engine;
	}

}
