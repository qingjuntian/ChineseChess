package andren.game.china.chess;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
//import com.umeng.update.UmengUpdateAgent;
//import cn.domob.android.ads.DomobAdView;

public class BaseActivity extends Activity {
	private boolean isExit = false;
	private final static int CANCEL_EXIT = 0;
	protected Toast toast = null;

	protected void ToQuitTheApp() {
		if (isExit) {
			// ACTION_MAIN with category CATEGORY_HOME 启动主屏幕
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			startActivity(intent);
			System.exit(0);// 使虚拟机停止运行并退出程序
		} else {
			isExit = true;
			Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
			Message msg = mHandler.obtainMessage();
			msg.arg1 = CANCEL_EXIT;
			mHandler.sendMessageDelayed(msg, 3000);// 3秒后发送消息
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		toast = Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT);
	}

	// umeng platform
	protected void umeng_autoUpdate() {
		// UmengUpdateAgent.setUpdateOnlyWifi(true);
		// UmengUpdateAgent.update(this);
	}

	// waps platform
	protected void waps_initAppsWall() {
//		AppConnect
//				.getInstance("0d665bc65b8903f651006c126df29a88", "WAPS", this);
//
//		// 禁用错误报告
//		AppConnect.getInstance(this).setCrashReport(false);
//
//		AppConnect.getInstance(this)
//				.setAdViewClassName("cn.waps.OffersWebView");
//		// 初始化自定义广告数据
//		AppConnect.getInstance(this).initAdInfo();
//		// 初始化插屏广告数据
//		AppConnect.getInstance(this).initPopAd(this);
//		// 带有默认参数值的在线配置，使用此方法，程序第一次启动使用的是"defaultValue"，之后再启动则是使用的服务器端返回的参数值
//		String showAd = AppConnect.getInstance(this).getConfig("showAd",
//				"defaultValue");
	}

	protected void waps_showSelfOwnAppsWall() {
		// 显示自家应用
//		AppConnect.getInstance(BaseActivity.this).showMore(BaseActivity.this);
	}

	protected void waps_showGameAppsWall() {
//		AppConnect.getInstance(BaseActivity.this).showGameOffers(
//				BaseActivity.this);
	}

	private void waps_showSoftwareAppsWall() {
//		AppConnect.getInstance(BaseActivity.this).showAppOffers(
//				BaseActivity.this);
	}

	private void waps_showTodayHotAppsWall() {
//		AppConnect.getInstance(BaseActivity.this).showOffers(BaseActivity.this);
	}

	protected void waps_AppsWallFinalize() {
//		AppConnect.getInstance(this).finalize();
	}

	protected void waps_showFeedback() {
		// 用户反馈
//		AppConnect.getInstance(this).showFeedback();
	}

	// domob platform
	// public static final String DOMOB_PUBLISHER_ID = "56OJzQVouNQWRcRCZ8";
	// public static final String DOMOB_InlinePPID = "16TLm8EvApcqcNUH3EooDNli";
	protected void domob_addAd(ViewGroup rootLayout) {
		// DomobAdView mAdview320x50 = new DomobAdView(this, DOMOB_PUBLISHER_ID,
		// DOMOB_InlinePPID, DomobAdView.INLINE_SIZE_320X50);
		// mAdview320x50.setKeyword("game");
		// LinearLayout adLayout = (LinearLayout) rootLayout
		// .findViewById(R.id.ad_layout);
		// adLayout.addView(mAdview320x50);
	}

	private final static int DIALOG_EXIT_APP = 0;
	private boolean mBackKeyPressed = false;
	private boolean mExitDialogShown = false;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_EXIT_APP:
			mExitDialogShown = true;
			return createExitAppDialog();
		default:
			return null;
		}
	}

	private Dialog createExitAppDialog() {
		return new AlertDialog.Builder(this)
				.setIcon(R.drawable.question)
				.setTitle("退出游戏")
				.setMessage("试用一下其他应用吧, 说不定有你喜欢的!")
				.setPositiveButton("是", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
						if (mBackKeyPressed) {
							waps_showTodayHotAppsWall();
							mBackKeyPressed = false;
						} else {
							waps_showSoftwareAppsWall();
						}
					}
				})
				.setNegativeButton("否", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
						finish();
					}
				}).create();
	}

	private String displayPointsText;
	private String currencyName = "金币";
	protected TextView pointsTextView;
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
			case CANCEL_EXIT:
				isExit = false;
			default:
				super.handleMessage(msg);
			}
		}
	};

	@Override
	protected void onResume() {
//		Runnable run = new Runnable() {
//
//			@Override
//			public void run() {
//				// 从服务器端获取当前用户的虚拟货币.
//				// 返回结果在回调函数getUpdatePoints(...)中处理
//				AppConnect.getInstance(BaseActivity.this).getPoints(
//						BaseActivity.this);
//			}
//		};
//		mHandler.postDelayed(run, 3000);
		super.onResume();
	}

	/**
	 * AppConnect.getPoints()方法的实现，必须实现
	 * 
	 * @param currencyName
	 *            虚拟货币名称.
	 * @param pointTotal
	 *            虚拟货币余额.
	 */
	public void getUpdatePoints(String currencyName, int pointTotal) {
		this.currencyName = currencyName;
		displayPointsText = currencyName + ": " + pointTotal;
		mHandler.post(mUpdateResults);
	}

	/**
	 * AppConnect.getPoints() 方法的实现，必须实现
	 * 
	 * @param error
	 *            请求失败的错误信息
	 */
	public void getUpdatePointsFailed(String error) {
		displayPointsText = error;
		mHandler.post(mUpdateResults);
	}

	// 创建一个线程
	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			if (pointsTextView != null) {
				// Toast.makeText(ChessMain.this, displayPointsText +
				// " showing", Toast.LENGTH_LONG).show();
				pointsTextView.setText(displayPointsText);
			}
		}
	};

	protected void onExitingGame(boolean backKeyPressed) {
		if (mExitDialogShown) {
			finish();
		} else {
			showDialog(DIALOG_EXIT_APP);
			mBackKeyPressed = backKeyPressed;
		}
	}

	protected void onDestroy() {
		super.onDestroy();
		waps_AppsWallFinalize();
	}

	@Override
	public void onStart() {
		super.onStart();
		// The rest of your onStart() code.
		EasyTracker.getInstance().activityStart(this); // Add this method.
	}

	@Override
	public void onStop() {
		super.onStop();
		// The rest of your onStop() code.
		EasyTracker.getInstance().activityStop(this); // Add this method.
	}
}