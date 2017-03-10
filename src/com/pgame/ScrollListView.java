package com.pgame;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 2013-6-30
 */
public class ScrollListView extends ListView implements OnScrollListener,
		OnItemClickListener, OnClickListener {

	public interface LoadNotifyer {
		public void load();
	}

	public interface OnScrollStateChangedListener {
		public void onScrollStateChanged(int oldState, int newState);
	}

	private LinearLayout footViewLoading, footViewRetry, footViewNomore;
	private LoadNotifyer loadNotifyer;
	private int scrollState;
	private OnScrollStateChangedListener onScrollStateChangedListener;

	public ScrollListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public ScrollListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ScrollListView(Context context) {
		super(context);
		init(context);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == 0x1001) { // reload
			setFootviewType(FOOTVIEW_TYPE.LOADING);
			if (loadNotifyer != null)
				loadNotifyer.load();
		} else if (v.getId() == 0x1002) {
			setSelection(0);
		}
	}

	private void init(Context context) {
		footViewLoading = new LinearLayout(context);
		footViewLoading.setOrientation(LinearLayout.HORIZONTAL);
		footViewLoading.setGravity(Gravity.CENTER);
		ProgressBar bar = new ProgressBar(context);
		TextView textView = new TextView(context);
		textView.setText("loading...");
		footViewLoading.addView(bar, new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		footViewLoading.addView(textView, new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		footViewRetry = new LinearLayout(context);
		footViewRetry.setOrientation(LinearLayout.HORIZONTAL);
		footViewRetry.setGravity(Gravity.CENTER);
		textView = new TextView(context);
		textView.setId(0x1001);
		textView.setGravity(Gravity.CENTER);
		textView.setText("network problem, please retry!");
		textView.setOnClickListener(this);
		footViewRetry.addView(textView, new LayoutParams(
				LayoutParams.WRAP_CONTENT, getFixPx(50)));

		footViewNomore = new LinearLayout(context);
		footViewNomore.setOrientation(LinearLayout.HORIZONTAL);
		footViewNomore.setGravity(Gravity.CENTER);
		footViewNomore.setId(0x1002);

		textView = new TextView(context);
		textView.setText("Go to top");
		textView.setGravity(Gravity.CENTER);

		footViewNomore.setClickable(true);
		footViewNomore.setOnClickListener(this);
		footViewNomore.addView(textView, new LayoutParams(
				LayoutParams.WRAP_CONTENT, getFixPx(50)));

		setFootviewType(FOOTVIEW_TYPE.LOADING);

		setOnScrollListener(this);
		scrollState = SCROLL_STATE_IDLE;

//		super.setOnItemClickListener(this);
	}

	public enum FOOTVIEW_TYPE {
		LOADING, NOMOR, RETRY, NONE
	}

	private View curFootView;

	public void setFootviewType(FOOTVIEW_TYPE type) {
		if (curFootView != null && curFootView.getTag() == type)
			return;

		if (curFootView != null)
			removeFooterView(curFootView);

		switch (type) {
		case LOADING:
			curFootView = footViewLoading;
			break;
		case NOMOR:
			curFootView = footViewNomore;
			break;
		case RETRY:
			curFootView = footViewRetry;
			break;
		case NONE:
			return;
		}

		addFooterView(curFootView);
		curFootView.setTag(type);
	}

	private View curHeadView;

	public void setHeadView(View v) {
		if (curHeadView != null)
			return;
		curHeadView = v;
		addHeaderView(v);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState != this.scrollState) {
			if (onScrollStateChangedListener != null) {
				onScrollStateChangedListener.onScrollStateChanged(
						this.scrollState, scrollState);
			}
			this.scrollState = scrollState;
		}
	}

	protected int firstVisibleItem, visibleItemCount, totalItemCount;

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (totalItemCount < 2) //
			return;

		if (firstVisibleItem + visibleItemCount >= totalItemCount) { //
																		// f
																		//
			if (loadNotifyer != null && (curFootView != footViewNomore)) {
				loadNotifyer.load();
			}
		}
		this.firstVisibleItem = firstVisibleItem;
		this.visibleItemCount = visibleItemCount;
		this.totalItemCount = totalItemCount;
	}

	public int getFirstVisibleItem() {
		return firstVisibleItem;
	}

	public int getVisibleItemCount() {
		return visibleItemCount;
	}

	public int getScrollState() {
		return scrollState;
	}

	public void setLoadNotifyer(LoadNotifyer loadNotifyer) {
		this.loadNotifyer = loadNotifyer;
	}

	public void setOnScrollStateChangedListener(
			OnScrollStateChangedListener onScrollStateChangedListener) {
		this.onScrollStateChangedListener = onScrollStateChangedListener;
	}

	public int getFixPx(int dp) {
		float scale = getContext().getResources().getDisplayMetrics().density;
		return (int) (scale * dp + 0.5);
	}

	private OnItemClickListener listener;

	@Override
	public void setOnItemClickListener(OnItemClickListener listener) {
		this.listener = listener;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (listener == null)
			return;
		if (curHeadView != null) {
			if (position == 0)
				return;
			listener.onItemClick(parent, view, position - 1, id);
		} else {
			listener.onItemClick(parent, view, position, id);
		}
	}
}