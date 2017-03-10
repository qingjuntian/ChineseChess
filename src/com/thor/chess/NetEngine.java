package com.thor.chess;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

interface IConnectListener {
	public void onCompleted(boolean connectionEstablished);
}

interface IAcceptListener {
	public void onCompleted(boolean connectionEstablished);
}

public abstract class NetEngine extends Engine implements IEngine {
	protected static final UUID CHESS_UUID = UUID
			.fromString("08e2b297-3470-44d2-9457-0787b57f5a21");

	private String partnerName;

	private String playerName;

	protected int playerColor;

	protected String remoteAddress;

	private boolean isServer = false;

	protected boolean offeredToClose = false;

	public NetEngine(String yourName) {
		super();
		playerName = yourName;
	}

	public NetEngine(String yourName, int playerColor) {
		super();
		playerName = yourName;
		startGame(playerColor);
		isServer = true;
	}


	public NetEngine(String yourName, String remoteAddress) {
		super();
		playerName = yourName;
		this.remoteAddress = remoteAddress;
		isServer = false;
	}

	public void resetNetGame() {
	}

	public void startGame(int playerColor) {
		this.playerColor = playerColor;
		super.startGame(playerColor);
		syncPlayerInfo();
		listener.onNetGameBegin();
	}

	public boolean isServer() {
		return isServer;
	}

	static class NetCommand {
		public static final int CMD_UNKNOW = 0;
		public static final int CMD_START = 1;
		public static final int CMD_MOVE = 2;
		public static final int CMD_UNDO = 3;
		public static final int CMD_UNDO_RESP = (0x100 | 0x3);
		public static final int CMD_RENEW = 4;
		public static final int CMD_RENEW_RESP = (0x100 | 0x4);
		public static final int CMD_GIVE_UP = 5;
		public static final int CMD_SYNC_NAME = 6;
		public static final int CMD_DRAW = 7;
		public static final int CMD_DRAW_RESP = (0x100 | 0x7);

		public int pos = 0;
		public int command = CMD_UNKNOW;
		public Object argument = null;
	}

	final static Pattern askMove = Pattern
			.compile("ask:move:([0-9])([0-9])([0-9])([0-9])\\n");
	final static Pattern askUndo = Pattern.compile("ask:undo\\n");
	final static Pattern askDraw = Pattern.compile("ask:draw\\n");
	final static Pattern askGiveup = Pattern.compile("ask:giveup\\n");
	final static Pattern askRenew = Pattern.compile("ask:renew:(0|1)\\n");
	final static Pattern askSyncName = Pattern.compile("ask:sync:(.+)\\n");
	final static Pattern rspUndo = Pattern.compile("rsp:undo:(true|false)\\n");
	final static Pattern rspRenew = Pattern
			.compile("rsp:renew:(true|false)\\n");
	final static Pattern rspDraw = Pattern.compile("rsp:draw:(true|false)\\n");
	final static Pattern askStart = Pattern.compile("ask:start:(0|1)\\n");

	private static NetCommand parseCommand(String cmdStr) {
		Log.i("ChineseChess", cmdStr);
		NetCommand cmd = new NetCommand();
		cmd.command = NetCommand.CMD_UNKNOW;
		Matcher matcher = askMove.matcher(cmdStr);
		if (matcher.find()) {
			cmd.command = NetCommand.CMD_MOVE;
			MoveInfo mv = new MoveInfo();
			mv.fromX = Integer.parseInt(matcher.group(1));
			mv.fromY = Integer.parseInt(matcher.group(2));
			mv.toX = Integer.parseInt(matcher.group(3));
			mv.toY = Integer.parseInt(matcher.group(4));
			cmd.argument = mv;
			return cmd;
		}
		matcher = askUndo.matcher(cmdStr);
		if (matcher.find()) {
			cmd.command = NetCommand.CMD_UNDO;
			return cmd;
		}
		matcher = askGiveup.matcher(cmdStr);
		if (matcher.find()) {
			cmd.command = NetCommand.CMD_GIVE_UP;
			return cmd;
		}
		matcher = askDraw.matcher(cmdStr);
		if (matcher.find()) {
			cmd.command = NetCommand.CMD_DRAW;
			return cmd;
		}
		matcher = askStart.matcher(cmdStr);
		if (matcher.find()) {
			cmd.command = NetCommand.CMD_START;
			cmd.argument = Integer.parseInt(matcher.group(1));
			return cmd;
		}
		matcher = askRenew.matcher(cmdStr);
		if (matcher.find()) {
			cmd.command = NetCommand.CMD_RENEW;
			cmd.argument = Integer.parseInt(matcher.group(1));
			return cmd;
		}
		matcher = askSyncName.matcher(cmdStr);
		if (matcher.find()) {
			cmd.command = NetCommand.CMD_SYNC_NAME;
			cmd.argument = matcher.group(1);
			return cmd;
		}
		matcher = rspUndo.matcher(cmdStr);
		if (matcher.find()) {
			cmd.command = NetCommand.CMD_UNDO_RESP;
			cmd.argument = Boolean.parseBoolean(matcher.group(1));
			return cmd;
		}
		matcher = rspRenew.matcher(cmdStr);
		if (matcher.find()) {
			cmd.command = NetCommand.CMD_RENEW_RESP;
			cmd.argument = Boolean.parseBoolean(matcher.group(1));
			return cmd;
		}
		matcher = rspDraw.matcher(cmdStr);
		if (matcher.find()) {
			cmd.command = NetCommand.CMD_DRAW_RESP;
			cmd.argument = Boolean.parseBoolean(matcher.group(1));
			return cmd;
		}
		return cmd;
	}

	protected static NetCommand parseCommand(byte[] buffer, int length) {
		ByteBuffer arr = ByteBuffer.wrap(buffer, 0, length);
		for (int i = 0; i < length; i++) {
			if (arr.get(i) == '\n') {
				byte[] cmdBytes = new byte[i + 1];
				arr.position(0);
				arr.get(cmdBytes);
				String cmdStr;
				try {
					cmdStr = new String(cmdBytes, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					NetCommand cmd = new NetCommand();
					cmd.command = NetCommand.CMD_UNKNOW;
					cmd.pos = i + 1;
					return cmd;
				}
				NetCommand cmd = parseCommand(cmdStr);
				cmd.pos = i + 1;
				return cmd;
			} else if (i > 1024) {
				NetCommand cmd = new NetCommand();
				cmd.command = NetCommand.CMD_UNKNOW;
				cmd.pos = i + 1;
				return cmd;
			}
		}
		return null;
	}

	private static void rotateMove(MoveInfo mv) {
		mv.fromX = 8 - mv.fromX;
		mv.fromY = 9 - mv.fromY;
		mv.toX = 8 - mv.toX;
		mv.toY = 9 - mv.toY;
	}

	public void processResp(String resp) {
		processCommand(parseCommand(resp));
	}

	public void processCommand(NetCommand cmd) {
		switch (cmd.command) {
		case NetCommand.CMD_START:
			startGame(1 - (Integer) cmd.argument);
			postSync();
			break;
		case NetCommand.CMD_MOVE:
			MoveInfo mv = (MoveInfo) cmd.argument;
			rotateMove(mv); // Rotate move info
			postEndResponse(super.move(mv.fromX, mv.fromY, mv.toX, mv.toY));
			beginResponse();
			postPlayerInfo(playerName, partnerName, getPlayer());
//			postGameMessage("", 0);
			break;
		case NetCommand.CMD_UNDO:
			postAskUndo();
			break;
		case NetCommand.CMD_RENEW:
			postAskRenew();
			break;
		case NetCommand.CMD_DRAW:
			postAskDraw();
			break;
//		case NetCommand.CMD_GIVE_UP:
//			listener.onPartnerGiveup();
//			postGameMessage("游戏结束，对方认输了", 2);
//			break;
		case NetCommand.CMD_SYNC_NAME:
			partnerName = (String) cmd.argument;
			ChessApplication.setSetting(remoteAddress, partnerName);
			postPlayerInfo(playerName, partnerName, getPlayer());
			break;
		case NetCommand.CMD_UNDO_RESP:
			if ((Boolean) cmd.argument) {
				undo();
				undo();
				postGameMessage("您悔了一步棋！", 1);
				postEndUndo(true);
			} else {
				postEndUndo(false);
				postGameMessage("", 0);
			}
			break;
		case NetCommand.CMD_RENEW_RESP:
			if ((Boolean) cmd.argument) {
				startGame(playerColor);
				postEndRenew(true);
				postPlayerInfo(playerName, partnerName, getPlayer());
				postGameMessage("重新开始游戏", 0);
			} else {
				postEndRenew(false);
			}
			break;
		case NetCommand.CMD_DRAW_RESP:
			if ((Boolean) cmd.argument) {
				postEndDraw(true);
				postGameMessage("和棋！", 2);
			} else {
				postEndDraw(false);
			}
			break;
		}
	}

	protected abstract void initInputOutput(boolean isServer, int playerColor,
			String playerName) throws IOException;

	protected void run(boolean isServer) {
		try {
			initInputOutput(isServer, playerColor, playerName);

			Iterator<NetCommand> it = null;
			while ((it = readCommand()) != null) {
				while (it.hasNext()) {
					NetCommand cmd = it.next();
					processCommand(cmd);
				}
			}
		} catch (IOException e) {
			if (!offeredToClose) {
				postGameOver();
				postGameMessage("游戏结束，对方已断开。", 3);
			}
		}
	}

	protected abstract Iterator<NetCommand> readCommand();

	public abstract void connect(final IConnectListener listener);

	public abstract void listen(final IAcceptListener listener);

	public abstract void stopListen();

	protected abstract void sendToPartner(String msg);

	public int getVaildAction() {
		if (isGameStarted()) {
			return ACTION_UNDO | ACTION_RENEW | ACTION_DRAW | ACTION_GIVEUP
					| ACTION_RESPONSE;
		} else {
			return ACTION_START | ACTION_CHANGE_SEAT;
		}
	}

	public void syncPlayerInfo() {
		if (partnerName != null) {
			beginResponse();
			postPlayerInfo(playerName, partnerName, getPlayer());
		}
	}

	@Override
	public synchronized boolean move(int fromX, int fromY, int toX, int toY) {
		boolean result = super.move(fromX, fromY, toX, toY);
		if (!result)
			postGameMessage("你的走法不太对哦！", 1);
		postPlayerInfo(playerName, partnerName, getPlayer());
		String msg = String.format("ask:move:%1$d%2$d%3$d%4$d\n", fromX, fromY,
				toX, toY);
		sendToPartner(msg);
		beginResponse();
		return result;
	}

	public void giveUp() {
		sendToPartner("finishgame:-1\n");
	}

	public void responseAskUndo(boolean accept) {
		if (accept) {
			undo();
			undo();
		}
		String rsp = String.format("rsp:undo:%1$s\n", Boolean.toString(accept));
		sendToPartner(rsp);
	}

	public void responseAskRenew(boolean accept) {
		if (accept) {
			startGame(playerColor);
			postPlayerInfo(playerName, partnerName, getPlayer());
			postGameMessage("", 0);
		}
		String rsp = String
				.format("rsp:renew:%1$s\n", Boolean.toString(accept));
		sendToPartner(rsp);
	}

	public void responseAskDraw(boolean accept) {
		if (accept) {
			postGameOver();
			postGameMessage("和棋！", 2);
		}
		String rsp = String.format("rsp:draw:%1$s\n", Boolean.toString(accept));
		sendToPartner(rsp);
	}

	public void beginResponse() {
		if (partnerName != null) {
			if (getPlayer() == getDirection()) {
				postGameMessage("等待您走棋...", 0);
			} else {
				postGameMessage("等待对方走棋...", 0);
			}
		}
	}

	public void beginUndo() {
		postGameMessage("您请求悔棋，等待对方确认...", 0);
		String rsp = String.format("ask:undo\n");
		sendToPartner(rsp);
	}

	public void beginRenew() {
		postGameMessage("您请求重新开始，等待对方确认...", 0);
		String rsp = String.format("ask:renew:%1$d\n", playerColor);
		sendToPartner(rsp);
	}

	public void beginDraw() {
		postGameMessage("您请求和棋，等待对方确认...", 0);
		String rsp = String.format("ask:draw\n");
		sendToPartner(rsp);
	}

	public String getPartnerName() {
		return partnerName;
	}

	public void setPartnerName(String partnerName) {
		this.partnerName = partnerName;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public void joinPlayer(String newJoinInfo) {
		if (listener != null) {
			listener.onJoinPlayer(newJoinInfo);
		}
	}

	public String getPlayerLevel() {
		return "Level 1";
	}

	public void leavePlayer(String player) {
		if (listener != null) {
			listener.onLeavePlayer(player);
		}
	}

	public void playerReady(String player) {
		if (listener != null) {
			listener.onGetReadyPlayer(player);
		}
	}

	public void finishGame(String result, String score) {
		this.started = false;;
		if (listener != null) {
			listener.onGameFinish(result, score);
		}
	}


}
