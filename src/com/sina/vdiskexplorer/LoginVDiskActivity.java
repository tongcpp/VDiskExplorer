package com.sina.vdiskexplorer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.vdisk.android.VDiskAuthSession;
import com.vdisk.android.VDiskDialogListener;
import com.vdisk.net.exception.VDiskDialogError;
import com.vdisk.net.exception.VDiskException;
import com.vdisk.net.session.AccessToken;
import com.vdisk.net.session.AppKeyPair;
import com.vdisk.net.session.Session.AccessType;
import com.vdisk.net.session.WeiboAccessToken;

public class LoginVDiskActivity extends Activity implements VDiskDialogListener {

	VDiskAuthSession session;

	public static final String CONSUMER_KEY = "CONSUMER_KEY";
	public static final String CONSUMER_SECRET = "CONSUMER_SECRET";
	public static final String WEIBO_ACCESS_TOKEN = "WEIBO_ACCESS_TOKEN";
	private static final String REDIRECT_URL = "http://vdisk.weibo.com/";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_vdisk);

		AppKeyPair appKeyPair = new AppKeyPair(CONSUMER_KEY, CONSUMER_SECRET);
		session = VDiskAuthSession.getInstance(this, appKeyPair,
				AccessType.APP_FOLDER);

		final Spinner sp = (Spinner) findViewById(R.id.spinner2choose_login_type);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item);
		adapter.add("ʹ��΢����֤��¼");
		adapter.add("ʹ��΢��Token��¼");
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp.setAdapter(adapter);

		Button btn = (Button) findViewById(R.id.button2auth);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				if (sp.getSelectedItemPosition() == 0) {
					// ʹ��΢����֤
					session.setRedirectUrl(REDIRECT_URL);

					Toast.makeText(getApplicationContext(), "����ʹ��΢�̵�¼��֤",
							Toast.LENGTH_SHORT).show();
				}
				if (sp.getSelectedItemPosition() == 1) {
					// ʹ��΢��Token��֤
					WeiboAccessToken weiboToken = new WeiboAccessToken();
					weiboToken.mAccessToken = LoginVDiskActivity.WEIBO_ACCESS_TOKEN;
					session.enabledAndSetWeiboAccessToken(weiboToken);

					Toast.makeText(getApplicationContext(), "����ʹ��΢��Token��֤",
							Toast.LENGTH_SHORT).show();
				}
				session.authorize(LoginVDiskActivity.this,
						LoginVDiskActivity.this);

			}
		});

		if (session.isLinked()) {
			// startActivity(new Intent(this, FileActivity.class));//���뱾��
			// startActivity(new Intent(this, VDiskFileActivity.class));// �����ƶ�
			startActivity(new Intent(this, FileTabActivity.class));// ����˫ѡ����

			finish();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_login_vdisk, menu);
		return true;
	}

	// ��֤�쳣�Ļص�����
	// Callback method for abnormal authentication
	@Override
	public void onVDiskException(VDiskException exception) {
		// TODO Auto-generated method stub
		Toast.makeText(getApplicationContext(),
				"Auth exception : " + exception.getMessage(), Toast.LENGTH_LONG)
				.show();
	}

	// ��֤����Ļص�����
	// Callback method for authentication mistakes
	@Override
	public void onError(VDiskDialogError error) {
		Toast.makeText(getApplicationContext(),
				"Auth error : " + error.getMessage(), Toast.LENGTH_LONG).show();
	}

	// ��֤��ȡ���Ļص�����
	// Callback method as authentication is canceled
	@Override
	public void onCancel() {
		Toast.makeText(getApplicationContext(), "Auth cancel",
				Toast.LENGTH_LONG).show();
	}

	// ��֤������Ļص�����
	// Callback method after authentication
	@Override
	public void onComplete(Bundle values) {

		if (values != null) {
			AccessToken mToken = (AccessToken) values
					.getSerializable(VDiskAuthSession.OAUTH2_TOKEN);
			session.finishAuthorize(mToken);
		}
		// ����activity��ת
		startActivity(new Intent(this, LoginVDiskActivity.class));// ���¿���һ�������Activity����Ϊ�˽���һ��session�Ƿ����ӳɹ����ж�
		finish();
	}

}
