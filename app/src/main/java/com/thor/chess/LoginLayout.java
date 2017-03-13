package com.thor.chess;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import andren.game.china.chess.R;

public class LoginLayout extends LinearLayout implements View.OnClickListener {

	private Context context = null;


	public LoginLayout(Context context) {
		super(context);
		this.context = context;
	}

	public LoginLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	public LoginLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}

	public void startLogin() {
		Button btnLogin = (Button) findViewById(R.id.btn_login);
		btnLogin.setOnClickListener(this);

		Button btnTencentLogin = (Button) findViewById(R.id.btn_tencent_login);
		btnTencentLogin.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btn_login) {
			EditText account = (EditText) this.findViewById(R.id.account);
			EditText password = (EditText) this.findViewById(R.id.account);
			String accountName = account.getText().toString().trim();
			String pwd = password.getText().toString().trim();
			((ChessMain) context).login(accountName, pwd);
		} else if (v.getId() == R.id.btn_tencent_login) {
			((ChessMain) context).loginTencent();
		}
	}
}
