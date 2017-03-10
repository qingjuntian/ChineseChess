package com.thor.chess;

import andren.game.china.chess.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Vibrator;

interface IChessEventListener {
	public void onReturn(boolean netGame, boolean needAsk);

	public void onFinishGame(String result, String score);
}

public class ChessLayout extends RelativeLayout implements
		View.OnClickListener, IEngineEventListener {

	private Engine engine = null;

	private ChessView chessView = null;

	private IChessEventListener listener = null;

	private AlertDialog msgDialog = null;

	private TextView txtInfo = null;

	private TextView txtMessage = null;

	private TextView txtPlayerName = null;

	private TextView txtPlayerLevel = null;

	private TextView txtPlayerStatus = null;

	private TextView txtPartnerName = null;

	private TextView txtPartnerLevel = null;

	private TextView txtPartnerStatus = null;

	private Toast toast = null;

	private Context context = null;

	private boolean inAction = false;

	private static SoundPool soundPool = null;

	private static SparseIntArray soundMap = null;

	private boolean enableSound = true;

	private boolean nightMode = false;

	private Vibrator vibrator = null;

	private ProgressDialog dialog;

	public ChessLayout(Context context) {
		super(context);
		this.context = context;
		toast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
	}

	public ChessLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		toast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
	}

	public ChessLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
		toast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
	}

	public void setNightMode(boolean nightMode) {
		this.nightMode = nightMode;
		if (chessView != null)
			chessView.setNightMode(nightMode);
	}

	public boolean getNightMode() {
		return nightMode;
	}

	public boolean getEnableSound() {
		return enableSound;
	}

	public Engine getEngine() {
		return engine;
	}

	public void setEnableSound(boolean enableSound) {
		this.enableSound = enableSound;
		ChessApplication
				.setSetting("Sound", ((Boolean) enableSound).toString());
	}

	public void setChessListener(IChessEventListener listener) {
		this.listener = listener;
	}

	public void startBluetoothServer() {
		final ProgressDialog dialog = ProgressDialog.show(context, null,
				"等待设备接入...", true, true, new OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						((NetEngine) engine).stopListen();
						((ChessMain) context).finishGame();
					}
				});
		((NetEngine) engine).listen(new IAcceptListener() {
			public void onCompleted(final boolean connectionEstablished) {
				chessView.post(new Runnable() {
					public void run() {
						dialog.dismiss();
						if (!connectionEstablished) {
							((ChessMain) context).finishGame();
						}
					}
				});
			}
		});
	}

	public void playSound(int soundId) {
		if (!enableSound) {
			if (soundId == R.raw.captured) {
				try {
					vibrator.vibrate(60);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (soundId == R.raw.check) {
				try {
					vibrator.vibrate(120);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (soundId == R.raw.loss || soundId == R.raw.win) {
				try {
					vibrator.vibrate(200);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return;
		}
		AudioManager audioManager = (AudioManager) ((Activity) context)
				.getSystemService(Context.AUDIO_SERVICE);
		float volume = (float) audioManager
				.getStreamVolume(AudioManager.STREAM_RING)
				/ (float) audioManager
						.getStreamMaxVolume(AudioManager.STREAM_RING);
		soundPool.play(soundMap.get(soundId), volume, volume, 1, 0, 1f);
	}

	public void startGame(Engine engine) {
		this.engine = engine;
		this.engine.setEventListener(this);
		vibrator = (Vibrator) ((Activity) context)
				.getSystemService(Context.VIBRATOR_SERVICE);

		chessView = ((ChessView) findViewById(R.id.chess_board));
		chessView.setNightMode(nightMode);

		txtInfo = ((TextView) findViewById(R.id.text_info));
		txtMessage = ((TextView) findViewById(R.id.text_msg));
		if (engine instanceof NetEngine) {
			txtPlayerName = ((TextView) findViewById(R.id.player_name));
			txtPlayerLevel = ((TextView) findViewById(R.id.player_level));
			txtPlayerStatus = ((TextView) findViewById(R.id.player_status));
			txtPartnerName = ((TextView) findViewById(R.id.partner_name));
			txtPartnerLevel = ((TextView) findViewById(R.id.partner_level));
			txtPartnerStatus = ((TextView) findViewById(R.id.partner_status));

			txtPlayerName.setText(((NetEngine) engine).getPlayerName());
			txtPlayerLevel.setText(ChessApplication.getSetting("PlayerLevel",
					((NetEngine) engine).getPlayerLevel()));

			dialog = ProgressDialog.show(context, null, "等待...", true, true,
					new OnCancelListener() {
						public void onCancel(DialogInterface dialog) {
							((ChessMain) context).finishGame();
						}
					});
		} else {
			findViewById(R.id.players_info).setVisibility(View.GONE);
		}

		if (msgDialog == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
			builder.setPositiveButton("确定", null);
			msgDialog = builder.create();
		}

		if (soundPool == null) {
			soundMap = new SparseIntArray();
			soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 0);
			soundMap.put(R.raw.move, soundPool.load(context, R.raw.move, 1));
			soundMap.put(R.raw.captured,
					soundPool.load(context, R.raw.captured, 1));
			soundMap.put(R.raw.check, soundPool.load(context, R.raw.check, 1));
			soundMap.put(R.raw.win, soundPool.load(context, R.raw.win, 1));
			soundMap.put(R.raw.loss, soundPool.load(context, R.raw.loss, 1));
			soundMap.put(R.raw.draw, soundPool.load(context, R.raw.draw, 1));
		}

		initGameMenu();

		Button btnReturn = (Button) findViewById(R.id.btn_return);
		btnReturn.setOnClickListener(this);

		chessView.startGame(engine);
	}

	public void resetNetGame() {
		initGameMenu();
		txtPlayerStatus.setVisibility(View.VISIBLE);
		txtPlayerStatus.setText("Sitting");

		((NetEngine) engine).resetNetGame();

		txtInfo.setText("");
		txtMessage.setText("");
	}

	private void initGameMenu() {
		int capability = ((IEngine) engine).getVaildAction();
		Button btnAgain = (Button) findViewById(R.id.btn_again);
		if ((capability & IEngine.ACTION_RENEW) > 0) {
			btnAgain.setVisibility(View.VISIBLE);
		} else {
			btnAgain.setVisibility(View.GONE);
		}
		btnAgain.setOnClickListener(this);
		Button btnUndo = (Button) findViewById(R.id.btn_undo);
		if ((capability & IEngine.ACTION_UNDO) > 0) {
			btnUndo.setVisibility(View.VISIBLE);
		} else {
			btnUndo.setVisibility(View.GONE);
		}
		btnUndo.setOnClickListener(this);
		Button btnDraw = (Button) findViewById(R.id.btn_draw);
		if ((capability & IEngine.ACTION_DRAW) > 0) {
			btnDraw.setVisibility(View.VISIBLE);
		} else {
			btnDraw.setVisibility(View.GONE);
		}
		btnDraw.setOnClickListener(this);
		Button btnGiveup = (Button) findViewById(R.id.btn_giveup);
		if ((capability & IEngine.ACTION_GIVEUP) > 0) {
			btnGiveup.setVisibility(View.VISIBLE);
		} else {
			btnGiveup.setVisibility(View.GONE);
		}
		btnGiveup.setOnClickListener(this);

		Button btnStart = (Button) findViewById(R.id.btn_start);
		if ((capability & IEngine.ACTION_START) > 0) {
			btnStart.setVisibility(View.VISIBLE);
		} else {
			btnStart.setVisibility(View.GONE);
		}
		btnStart.setOnClickListener(this);

		Button btnChangeSeat = (Button) findViewById(R.id.btn_change_seat);
		if ((capability & IEngine.ACTION_START) > 0) {
			btnChangeSeat.setVisibility(View.VISIBLE);
		} else {
			btnChangeSeat.setVisibility(View.GONE);
		}
		btnChangeSeat.setOnClickListener(this);
	}

	public void finishGame() {
		chessView.finishGame();
	}

	// Process button click events
	public void onClick(View v) {
		if (inAction)
			return;
		if (v.getId() == R.id.btn_again) {
			inAction = true;
			((IEngine) engine).beginRenew();
		} else if (v.getId() == R.id.btn_undo) {
			inAction = true;
			((IEngine) engine).beginUndo();
		} else if (v.getId() == R.id.btn_draw) {
			if (chessView.isGameOver())
				return;
			inAction = true;
			((IEngine) engine).beginDraw();
		} else if (v.getId() == R.id.btn_giveup) {
			if (chessView.isGameOver())
				return;
			((IEngine) engine).giveUp();
		} else if (v.getId() == R.id.btn_return) {
			if (engine instanceof PgameNetEngine) {
				((IEngine) engine).leaveRoom();
			}
			listener.onReturn(engine instanceof PgameNetEngine, true);
		} else if (v.getId() == R.id.btn_start) {
			engine.hand();
			txtPlayerStatus.setVisibility(View.VISIBLE);
			txtPlayerStatus.setText(R.string.ready);
		} else if (v.getId() == R.id.btn_change_seat) {
			dialog = ProgressDialog.show(context, null, "等待...", true, true,
					new OnCancelListener() {
						public void onCancel(DialogInterface dialog) {
							((ChessMain) context).finishGame();
						}
					});
			txtPlayerStatus.setText("");
			txtPartnerName.setText("");
			txtPartnerLevel.setText("");
			txtPartnerStatus.setText("");
			engine.changeSeat();
		}
	}

	public void onGameMessage(final String message, final int withNotify) {
		post(new Runnable() {
			public void run() {
				txtMessage.setText(message);
				if (withNotify == 1) {
					toast.cancel();
					toast.setText(message);
					toast.show();
				} else if (withNotify == 2) {
					msgDialog.cancel();
					msgDialog.setMessage(message);
					msgDialog.show();
				} else if (withNotify == 3) {
					msgDialog.cancel();
					msgDialog.setMessage(message);
					msgDialog
							.setOnDismissListener(new DialogInterface.OnDismissListener() {
								public void onDismiss(DialogInterface dialog) {
									if (listener != null)
										listener.onReturn(false, false);
								}
							});
					msgDialog.show();
				}
			}
		});
	}

	@Override
	public void onGetReadyPlayer(String player) {
		final String[] partnerInfo = player.split(",");
		post(new Runnable() {
			public void run() {
				txtPartnerStatus.setText(partnerInfo[2]);
			}
		});
	}

	@Override
	public void onLeavePlayer(String player) {
		post(new Runnable() {
			public void run() {
				txtPartnerName.setText("");
				txtPartnerLevel.setText("");
				txtPartnerStatus.setText("");
			}
		});
	}

	@Override
	public void onGameFinish(String result, String score) {
		listener.onFinishGame(result, score);
	}

	@Override
	public void onJoinPlayer(String newJoinInfo) {
		if (newJoinInfo != null && newJoinInfo.length() > 0) {
			final String[] playerInfo = newJoinInfo.split(";");
			// partner is the new joiner
			post(new Runnable() {
				public void run() {
					String[] partnerInfo = playerInfo[0].split(",");
					txtPartnerName.setText(partnerInfo[0]);
					txtPartnerLevel.setText(partnerInfo[1]);
					txtPartnerStatus.setVisibility(View.VISIBLE);
					txtPartnerStatus.setText(partnerInfo[2]);
					dialog.dismiss();
				}
			});
		} else {
			post(new Runnable() {
				public void run() {
					txtPartnerName.setText("");
					txtPartnerLevel.setText("");
					txtPartnerStatus.setVisibility(View.VISIBLE);
					txtPartnerStatus.setText("");
					dialog.dismiss();
				}
			});
		}
	}

	@Override
	public void onSyncPlayerInfo(String player, String partner, int goSide) {
		final String txt = String.format("%1$s .VS. %2$s 现在轮到%3$s走棋", player,
				partner, goSide == 0 ? "红方" : "黑方");
		post(new Runnable() {
			public void run() {
				txtInfo.setText(txt);
			}
		});
	}

	public void onNetGameBegin() {
		post(new Runnable() {
			public void run() {
				txtPlayerStatus.setVisibility(View.GONE);
				txtPartnerStatus.setVisibility(View.GONE);
				initGameMenu();
				chessView.sync();
			}
		});
	}

	public void onAskUndo() {
		post(new Runnable() {
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getContext());
				builder.setMessage("对方请求悔棋，是否接受？");
				builder.setPositiveButton("同意",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								((IEngine) engine).responseAskUndo(true);
								chessView.sync();
							}
						});
				builder.setNegativeButton("拒绝",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								((IEngine) engine).responseAskUndo(false);
							}
						});
				builder.create().show();
			}
		});
	}

	public void onAskRenew() {
		post(new Runnable() {
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getContext());
				builder.setMessage("对方请求重新开始，是否接受？");
				builder.setPositiveButton("同意",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								((IEngine) engine).responseAskRenew(true);
								chessView.sync();
							}
						});
				builder.setNegativeButton("拒绝",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								((IEngine) engine).responseAskRenew(false);
							}
						});
				builder.create().show();
			}
		});
	}

	public void onAskDraw() {
		post(new Runnable() {
			public void run() {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getContext());
				builder.setMessage("对方请求和棋，是否接受？");
				builder.setPositiveButton("同意",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								((IEngine) engine).responseAskDraw(true);
								chessView.sync();
							}
						});
				builder.setNegativeButton("拒绝",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								((IEngine) engine).responseAskDraw(false);
							}
						});
				builder.create().show();
			}
		});
	}

	public void onEndResponse(boolean result) {
		if (result) {
			post(new Runnable() {
				public void run() {
					chessView.sync();
				}
			});
		}
	}

	public void onEndUndo(final boolean result) {
		inAction = false;
		post(new Runnable() {
			public void run() {
				if (result) {
					chessView.sync();
				} else {
					toast.cancel();
					toast.setText("对方拒绝了你的悔棋请求！");
					toast.show();
				}
			}
		});
	}

	public void onEndRenew(final boolean result) {
		inAction = false;
		post(new Runnable() {
			public void run() {
				if (result) {
					chessView.sync();
					chessView.restart();
				} else {
					toast.cancel();
					toast.setText("对方拒绝重新开始！");
					toast.show();
				}
			}
		});
	}

	public void onEndDraw(final boolean result) {
		inAction = false;
		post(new Runnable() {
			public void run() {
				if (result) {
					chessView.sync();
					chessView.stopGame();
				} else {
					toast.cancel();
					toast.setText("对方拒绝了和棋！");
					toast.show();
				}
				((NetEngine) engine).beginResponse();
			}
		});
	}

	public void onGameOver() {
		chessView.stopGame();
	}

	public void onSyncGame() {
		post(new Runnable() {
			public void run() {
				chessView.sync();
			}
		});
	}

	public void onMove(int state) {
		switch (state & 0xf) {
		case 0:
			playSound(R.raw.move);
			break;
		case 1:
			Log.i("ChineseChess", "Captured");
			playSound(R.raw.captured);
			break;
		case 2:
			Log.i("ChineseChess", "Checked");
			playSound(R.raw.check);
			break;
		case 3:
			if (engine.getDirection() == 0)
				playSound(R.raw.win);
			else
				playSound(R.raw.loss);
			break;
		case 4:
			if (engine.getDirection() == 1)
				playSound(R.raw.win);
			else
				playSound(R.raw.loss);
			break;
		case 5:
			playSound(R.raw.draw);
		}
	}

}
