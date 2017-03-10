package com.thor.chess;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

import andren.game.china.chess.BaseActivity;
import andren.game.china.chess.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pgame.ChessConstants;
import com.pgame.NetworkProxy;
import com.tencent.open.HttpStatusException;
import com.tencent.open.NetworkUnavailableException;
import com.tencent.tauth.Constants;
import com.tencent.tauth.IRequestListener;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

interface GameListener {
	public void onCreateEngine(Engine engine, boolean netServer);
}

@SuppressLint("ShowToast")
public class ChessMain extends BaseActivity implements GameListener,
		IChessEventListener {

	// /**
	// * 获取屏幕的亮度
	// *
	// * @param activity
	// * @return
	// */
	// public static int getScreenBrightness(Activity activity) {
	// int nowBrightnessValue = 0;
	// ContentResolver resolver = activity.getContentResolver();
	// try {
	// nowBrightnessValue = android.provider.Settings.System.getInt(
	// resolver, Settings.System.SCREEN_BRIGHTNESS);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// return nowBrightnessValue;
	// }

	@Override
	public void onBackPressed() {
		if (currentView == gameLayout) {
			returnMain(true);
		} else if (currentView == netGameResult) {
			returnGame();
		} else if (currentView == roomListView) {
			tencentHelper.logout();
			proxy.leaveGame();
			returnMain(false);
		} else if (currentView == loginLayout) {
			setContentView(homeMenuLayout);
			currentView = homeMenuLayout;
		} else {
			onExitingGame(true);
		}
	}

	@Override
	public void onReturn(boolean netGame, boolean needAsk) {
		if (netGame) {
			finishGame();
			startPgNetGame();
		} else {
			returnMain(needAsk);
		}
	}

	public void userLogin() {
		setContentView(loginLayout);
		currentView = loginLayout;
		loginLayout.startLogin();
	}

	private void startPgNetGame() {
		currentView.post(new Runnable() {
			@Override
			public void run() {
				roomListView.clear();
				setContentView(roomListView);
				currentView = roomListView;
				proxy.openSocket(ChessConstants.ROOM_SERVER_IP,
						ChessConstants.ROOM_SERVER_PORT);
				proxy.syncGameRoom(playerName);

			}
		});
	}

	@Override
	public void onFinishGame(final String result, final String score) {
		currentView.post(new Runnable() {
			public void run() {
				setContentView(netGameResult);
				TextView txtScore = (TextView) netGameResult
						.findViewById(R.id.score1);
				txtScore.setText(score);
				TextView txtResult = (TextView) netGameResult
						.findViewById(R.id.result);
				txtResult.setText(result);
				currentView = netGameResult;
			}
		});
	}

	// Chess board
	private ChessLayout gameLayout = null;
	private View homeMenuLayout = null;
	private View currentView = null;
	private View netGameResult = null;
	private LoginLayout loginLayout = null;
	private RoomListView roomListView = null;
	private NetGameDialog netDialog = null;
	private MenuItem soundItem = null;
	private MenuItem brightnessItem = null;
	private boolean nightMode = false;
	private int oldAutoBrightness = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
	private int oldBrightness = 255;
	protected int mSelectedItem = 4;
	private NetworkProxy proxy;
	public TencentHelp tencentHelper;
	private String playerName = "Qingjun";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		proxy = NetworkProxy.createDefaultProxy();

		tencentHelper = new TencentHelp("100543241");

		// Set screen should always portrait.
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		setContentView(R.layout.play_form);
		gameLayout = (ChessLayout) findViewById(R.id.mainLayout);
		gameLayout.setChessListener(this);

		roomListView = new RoomListView(ChessMain.this);
		roomListView.init(this);
		proxy.registListener(roomListView);

		setContentView(R.layout.login_form);
		loginLayout = (LoginLayout) findViewById(R.id.loginLayout);

		setContentView(R.layout.net_result_form);
		netGameResult = findViewById(R.id.net_result);

		setContentView(R.layout.main_form);
		homeMenuLayout = findViewById(R.id.main_menu);

		currentView = homeMenuLayout;

		initMainMenu();

		recordBrightness();
		gameLayout.setEnableSound(Boolean.parseBoolean(ChessApplication
				.getSetting("Sound", "true")));
		setNightMode(Boolean.parseBoolean(ChessApplication.getSetting(
				"NightMode", "false")));

		mSelectedItem = Integer.parseInt(ChessApplication.getSetting(
				"DifficultyLevel", "4"));
		// For advertisement
		pointsTextView = (TextView) gameLayout
				.findViewById(R.id.PointsTextView);
		domob_addAd(gameLayout);
		waps_initAppsWall();
		umeng_autoUpdate();
	}

	private void initMainMenu() {
		Button aiButton = (Button) findViewById(R.id.btn_ai_game);
		aiButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				AIGameDialog dialog = new AIGameDialog(ChessMain.this);
				dialog.setListener(ChessMain.this);
				dialog.show();
			}
		});

		Button btButton = (Button) findViewById(R.id.btn_bt_game);
		btButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				netDialog = new BluetoothNetGameDialog(ChessMain.this);
				netDialog.setListener(ChessMain.this);
				netDialog.show();
			}

		});

		Button netButton = (Button) findViewById(R.id.btn_net_game);
		netButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
//				startPgNetGame();
				userLogin();
			}
		});

		Button moreButton = (Button) findViewById(R.id.btn_more_game);
		moreButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				waps_showGameAppsWall();
			}
		});

		Button exitButton = (Button) findViewById(R.id.btn_exit);
		exitButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				toast.cancel();
				onExitingGame(false);
				// finish();
			}
		});
	}

	public void onCreateEngine(Engine engine, boolean netServer) {
		setContentView(gameLayout);
		currentView = gameLayout;
		gameLayout.startGame(engine);
		if (netServer) {
			gameLayout.startBluetoothServer();
		}
	}

	public void stopAutoBrightness() {
		Settings.System.putInt(this.getContentResolver(),
				Settings.System.SCREEN_BRIGHTNESS_MODE,
				Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
	}

	/**
	 *
	 * @param brightness
	 *            0 to 255 (means dark to full bright), less 0 means use default
	 */
	public void setBrightness(int brightness) {
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = Float.valueOf(brightness / 255f);
		getWindow().setAttributes(lp);
	}

	public void recordBrightness() {
		try {
			ContentResolver cr = getContentResolver();
			oldAutoBrightness = Settings.System.getInt(cr,
					Settings.System.SCREEN_BRIGHTNESS_MODE);
			oldBrightness = Settings.System.getInt(cr,
					Settings.System.SCREEN_BRIGHTNESS);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void restoreBrightness() {
		try {
			ContentResolver cr = getContentResolver();
			Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE,
					oldAutoBrightness);
			Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS,
					oldBrightness);
			setBrightness(oldBrightness);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// / 其实经我测试，如果只打算在自己的应用中修改亮度的话，只需要在修改亮度之前
	// / 先把自动亮度修改为关闭，等修改完亮度后在恢复成原来的值就行了。
	public void setNightMode(boolean nightMode) {
		this.nightMode = nightMode;
		gameLayout.setNightMode(nightMode);
		if (nightMode) {
			stopAutoBrightness();
			setBrightness(40);
		} else {
			restoreBrightness();
		}
		ChessApplication.setSetting("NightMode",
				((Boolean) nightMode).toString());
	}

	public boolean getNightMode() {
		return nightMode;
	}

	private void adjustSoundMenu() {
		if (gameLayout.getEnableSound()) {
			soundItem.setIcon(R.drawable.mute);
			soundItem.setTitle(R.string.menu_no_sound);
		} else {
			soundItem.setIcon(R.drawable.music);
			soundItem.setTitle(R.string.menu_sound);
		}
	}

	private void adjustBrightnessMenu() {
		if (getNightMode()) {
			brightnessItem.setIcon(R.drawable.sun);
			brightnessItem.setTitle(R.string.menu_day_mode);
		} else {
			brightnessItem.setIcon(R.drawable.moon);
			brightnessItem.setTitle(R.string.menu_night_mode);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if ((AIEngine) gameLayout.getEngine() != null) {
			menu.findItem(R.id.menu_difficulty).setVisible(true);
		} else {
			menu.findItem(R.id.menu_difficulty).setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	private int tempSelected;

	private void adjustDifficulty(final Context context) {
		final String[] levels = context.getResources().getStringArray(
				R.array.levels);
		tempSelected = mSelectedItem;
		AlertDialog mDialog = new AlertDialog.Builder(context)
				.setTitle(R.string.adjust_difficulty)
				.setSingleChoiceItems(levels, mSelectedItem,// 数据列表、第几个为选中
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								tempSelected = which;
							}
						})
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mSelectedItem = tempSelected;
						ChessApplication.setSetting("DifficultyLevel",
								String.valueOf(mSelectedItem));
						AIEngine engine = (AIEngine) gameLayout.getEngine();
						if (engine != null) {
							engine.setSearchSeconds(0.3f + mSelectedItem * 0.3f);
							String str = "您选择了" + levels[mSelectedItem]
									+ "级别的难度";
							Toast.makeText(context, str, 100).show();
						}
					}
				})
				.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).create();

		mDialog.show();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_feedback:
			waps_showFeedback();
			return true;
		case R.id.menu_sound:
			gameLayout.setEnableSound(!gameLayout.getEnableSound());
			adjustSoundMenu();
			return true;
		case R.id.menu_brightness:
			setNightMode(!getNightMode());
			adjustBrightnessMenu();
			return true;
		case R.id.menu_quit:
			toast.cancel();
			// finish();
			onExitingGame(false);
			return true;
		case R.id.menu_difficulty:
			adjustDifficulty(ChessMain.this);
			return true;
		default:
			return onOptionsItemSelected(item);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_chess_main, menu);
		soundItem = menu.findItem(R.id.menu_sound);
		adjustSoundMenu();

		brightnessItem = menu.findItem(R.id.menu_brightness);
		adjustBrightnessMenu();

		return true;
	}

	public void finishGame() {
		gameLayout.finishGame();
		setContentView(homeMenuLayout);
		currentView = homeMenuLayout;
	}

	public void returnMain(boolean needAsk) {
		if (!needAsk) {
			finishGame();
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("要结束当前游戏吗？");
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				finishGame();
			}
		});
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		});
		builder.create().show();
	}

	public void returnGame() {
		setContentView(gameLayout);
		currentView = gameLayout;
		gameLayout.resetNetGame();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		tencentHelper.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case 1:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				netDialog.startDiscovery();
			}
			break;
		case 2:
			if (resultCode == 300) {
				// Bluetooth is now discoverable, so set up a chat session
				netDialog.startNetServer(true);
			}
			break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		restoreBrightness();
		System.exit(0);
	}

	public void loginTencent() {
		tencentHelper.login();
	}
	

	public void login(String accountName, String pwd) {
		playerName = accountName;
		roomListView.clear(playerName);
		startPgNetGame();
	}

	private class BaseUiListener implements IUiListener {

		@Override
		public void onComplete(JSONObject response) {
			doComplete(response);
		}

		protected void doComplete(JSONObject values) {

		}

		@Override
		public void onError(UiError e) {
		}

		@Override
		public void onCancel() {
		}
	}

	private class BaseApiListener implements IRequestListener {
		private String mScope = "all";
		private Boolean mNeedReAuth = false;

		public BaseApiListener(String scope, boolean needReAuth) {
			mScope = scope;
			mNeedReAuth = needReAuth;
		}

		@Override
		public void onComplete(final JSONObject response, Object state) {
			// showResult("IRequestListener.onComplete:", response.toString());
			doComplete(response, state);
		}

		protected void doComplete(JSONObject response, Object state) {
			try {
				int ret = response.getInt("ret");
				if (ret == 100030) {
					if (mNeedReAuth) {
						Runnable r = new Runnable() {
							public void run() {
								tencentHelper.reAuth(ChessMain.this, mScope,
										new BaseUiListener());
							}
						};
						ChessMain.this.runOnUiThread(r);
					}
				} else if (ret == 0) {
					playerName = response.getString("nickname");
					roomListView.clear(playerName);
					startPgNetGame();
				}
			} catch (JSONException e) {
				e.printStackTrace();
				Log.e("toddtest", response.toString());
			}

		}

		@Override
		public void onIOException(final IOException e, Object state) {
			// showResult("IRequestListener.onIOException:", e.getMessage());
		}

		@Override
		public void onMalformedURLException(final MalformedURLException e,
				Object state) {
			// showResult("IRequestListener.onMalformedURLException",
			// e.toString());
		}

		@Override
		public void onJSONException(final JSONException e, Object state) {
			// showResult("IRequestListener.onJSONException:", e.getMessage());
		}

		@Override
		public void onConnectTimeoutException(ConnectTimeoutException arg0,
				Object arg1) {
			// showResult("IRequestListener.onConnectTimeoutException:",
			// arg0.getMessage());

		}

		@Override
		public void onSocketTimeoutException(SocketTimeoutException arg0,
				Object arg1) {
			// showResult("IRequestListener.SocketTimeoutException:",
			// arg0.getMessage());
		}

		@Override
		public void onUnknowException(Exception arg0, Object arg1) {
			// showResult("IRequestListener.onUnknowException:",
			// arg0.getMessage());
		}

		@Override
		public void onHttpStatusException(HttpStatusException arg0, Object arg1) {
			// showResult("IRequestListener.HttpStatusException:",
			// arg0.getMessage());
		}

		@Override
		public void onNetworkUnavailableException(
				NetworkUnavailableException arg0, Object arg1) {
			// showResult("IRequestListener.onNetworkUnavailableException:",
			// arg0.getMessage());
		}
	}

	private class TencentHelp {
		public Tencent tencent;

		public TencentHelp(String appId) {
			tencent = Tencent.createInstance(appId, ChessMain.this);
		}

		public void reAuth(ChessMain chessMain, String mScope,
				BaseUiListener baseUiListener) {
			tencent.reAuth(ChessMain.this, mScope, new BaseUiListener());
		}

		public void onActivityResult(int requestCode, int resultCode,
				Intent data) {
			tencent.onActivityResult(requestCode, resultCode, data);
		}

		private boolean ready() {
			boolean ready = tencent.isSessionValid()
					&& tencent.getOpenId() != null;
			if (!ready)
				Toast.makeText(ChessMain.this,
						"login and get openId first, please!",
						Toast.LENGTH_SHORT).show();
			return ready;
		}

		public void login() {
			if (!tencent.isSessionValid()) {
				tencent.login(ChessMain.this, "all", new BaseUiListener() {
					@Override
					protected void doComplete(JSONObject values) {
						System.out.println("doComplete!");
						if (ready()) {
							tencent.requestAsync(
									Constants.GRAPH_SIMPLE_USER_INFO, null,
									Constants.HTTP_GET, new BaseApiListener(
											"get_simple_userinfo", false), null);
						}
					}
				});
			}
		}

		public void logout() {
			if (tencent.isSessionValid()) {
				tencent.logout(ChessMain.this);
			}
		}
	}


}
