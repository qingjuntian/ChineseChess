package com.thor.chess;

public class AIEngine extends Engine implements IEngine{
	private int playerColor;
	private float searchSeconds;

	public AIEngine(int playerColor, float searchSeconds) {
		super();
		this.playerColor = playerColor;
		this.searchSeconds = searchSeconds;
		startGame(playerColor);
	}

	@Override
	public synchronized boolean move(int fromX, int fromY, int toX, int toY) {
		boolean result = super.move(fromX, fromY, toX, toY);
		if (!result)
			postGameMessage("你的走法不太对哦！", 1);
		postPlayerInfo("您", "计算机", getPlayer());
		beginResponse();
		return result;
	}

	public void setSearchSeconds(float searchSeconds) {
		this.searchSeconds = searchSeconds;
	}

	public void syncPlayerInfo() {
		postPlayerInfo("您", "计算机", getPlayer());
	}

	public int getVaildAction() {
		return ACTION_UNDO | ACTION_RENEW |	ACTION_RESPONSE;
	}

	public void giveUp() {
		postGameMessage("游戏结束，您已经认输了", 2);
	}

	public void responseAskUndo(boolean accept) {
		// Won't be implemented
	}

	public void responseAskRenew(boolean accept) {
		// Won't be implemented
	}

	synchronized public void beginResponse() {
		if (!isGameOver() && getDirection() != getPlayer()) {
			Thread thread = new Thread(new Runnable() {
				public void run() {
					if (getMoveCount() == 0) {
						postGameMessage("马上开始游戏...", 0);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
						}
					}
					postGameMessage("计算机正在思考...", 0);
					long beginTime = System.currentTimeMillis();
					MoveInfo mv = findSolution(searchSeconds);
					long endTime = System.currentTimeMillis();
					if (endTime - beginTime < 1500) {
						try {
							Thread.sleep(1500 - (endTime - beginTime));
						} catch (InterruptedException e) {
						}
					}
					postGameMessage("", 0);
					if (mv != null) {
						postEndResponse(move(mv.fromX, mv.fromY, mv.toX, mv.toY));
					} else {
						postEndResponse(false);
					}
				}
			});
			thread.start();
		} else {
			postEndResponse(false);
		}
	}

	public void beginUndo() {
		if (playerColor == getPlayer()) {
			undo();
			undo();
			postGameMessage("您悔了一步棋！", 1);
		}
		postEndUndo(true);
	}

	public void beginRenew() {
		startGame(playerColor);
		postEndRenew(true);
		postPlayerInfo("您", "计算机", getPlayer());
		postGameMessage("", 0);
	}

	public void beginDraw() {
		postGameMessage("接着走吧，不同意和棋。", 0);
		postEndDraw(false);
	}

	public void responseAskDraw(boolean accept) {
		// Nothing to do
	}

}
