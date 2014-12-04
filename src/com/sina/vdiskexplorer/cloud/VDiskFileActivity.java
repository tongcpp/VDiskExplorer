package com.sina.vdiskexplorer.cloud;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.sina.vdiskexplorer.LoginVDiskActivity;
import com.sina.vdiskexplorer.R;
import com.vdisk.android.VDiskAuthSession;
import com.vdisk.net.VDiskAPI;
import com.vdisk.net.VDiskAPI.Entry;
import com.vdisk.net.VDiskAPI.ThumbSize;
import com.vdisk.net.exception.VDiskException;
import com.vdisk.net.exception.VDiskFileSizeException;
import com.vdisk.net.exception.VDiskIOException;
import com.vdisk.net.exception.VDiskLocalStorageFullException;
import com.vdisk.net.exception.VDiskParseException;
import com.vdisk.net.exception.VDiskPartialFileException;
import com.vdisk.net.exception.VDiskServerException;
import com.vdisk.net.exception.VDiskUnlinkedException;
import com.vdisk.net.session.AppKeyPair;
import com.vdisk.net.session.Session.AccessType;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract.Data;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;
import android.support.v4.app.NavUtils;

public class VDiskFileActivity extends ListActivity implements
		OnItemLongClickListener {

	VDiskAuthSession session;
	VDiskAPI<VDiskAuthSession> mApi;// ��Ҫ

	private TextView cloud_path_edit;
	private ImageButton cloud_btn_qry;
	private ImageButton cloud_btn_add;
	private static final String rootPath = "/"; // rootPath����ʼ�ļ���

	private static final String TAG = "VDiskTestActivity";
	private static final int SUCCEED = 0;
	private static final int FAILED = -1;

	private static final int NEW_PICTURE = 1;
	private static final int SHOW_THUMBNAIL = 2;
	private static final int UPDATE_UI = 3;

	private static final int CREATE_DIR = 0;

	private Drawable mDrawable;

	private List<String> items = null; // items�������ʾ������
	private List<String> paths = null; // paths������ļ�·��
	private List<String> sizes = null; // sizes���ļ���С

	private ArrayList<String> list = null;

	ProgressDialog dialog;
	Handler handler = new Handler() {

		public void handleMessage(android.os.Message msg) {// �����첽��������Ϣ
			switch (msg.what) {
			case SUCCEED:
				dialog.dismiss();
				showToast(msg.getData().getString("msg"));
				getFileDir(cloud_path_edit.getText().toString());// ɾ���ɹ���ʱ��ˢ�µ�ǰ��ʾ�б�
				break;
			case UPDATE_UI:
				// ���������UI����ʾ��ǰĿ¼·��
				ArrayList templist = msg.getData().getParcelableArrayList(
						"list");
				list = (ArrayList<String>) templist.get(0);
				setListAdapter(new ArrayAdapter<String>(VDiskFileActivity.this,
						android.R.layout.simple_list_item_1, list));
				break;
			case SHOW_THUMBNAIL:
				dialog.dismiss();
				if (mDrawable != null)
					showThumbnailDialog();// ��ʱ��ʹ��
				break;
			case FAILED:
				dialog.dismiss();
				VDiskException e = (VDiskException) msg.getData()
						.getSerializable("error");
				String errMsg = "";
				if (e instanceof VDiskServerException) {
					errMsg = ((VDiskServerException) e).toString();
					Log.d("SDK", errMsg);
				} else if (e instanceof VDiskIOException) {
					// errMsg = "���������쳣";
					errMsg = getString(R.string.exception_vdisk_io).toString();
				} else if (e instanceof VDiskParseException) {
					// errMsg = "���ݽ����쳣";
					errMsg = getString(R.string.exception_vdisk_parse)
							.toString();
				} else if (e instanceof VDiskLocalStorageFullException) {
					// errMsg = "���ش洢�ռ�����";
					errMsg = getString(
							R.string.exception_vdisk_local_storage_full)
							.toString();
				} else if (e instanceof VDiskUnlinkedException) {
					// errMsg = "δ��¼��token����";
					errMsg = getString(R.string.exception_vdisk_unlinked)
							.toString();
				} else if (e instanceof VDiskFileSizeException) {
					// errMsg = "�ļ���С��������";
					errMsg = getString(R.string.exception_vdisk_file_size)
							.toString();
				} else if (e instanceof VDiskPartialFileException) {
					// errMsg = "����δ���";
					errMsg = getString(R.string.exception_vdisk_partial_file)
							.toString();
				} else {
					// errMsg = "δ֪�쳣";
					errMsg = getString(R.string.exception_vdisk_unknown)
							.toString();
					// showToast(e.getMessage());
				}
				showToast(errMsg);
				break;
			default:
				dialog.dismiss();
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_selected);

		cloud_path_edit = (EditText) findViewById(R.id.path_edit);
		cloud_btn_qry = (ImageButton) findViewById(R.id.qry_button);// ��ַ����·����λ��ť
		cloud_btn_qry.setOnClickListener((new OnClickListener() {
			public void onClick(View arg0) {
				getFileDir(cloud_path_edit.getText().toString());
			}
		}));

		cloud_btn_add = (ImageButton) findViewById(R.id.add_button);// �½��ļ���
		cloud_btn_add.setOnClickListener((new OnClickListener() {
			public void onClick(View arg0) {
				String curPath = cloud_path_edit.getText().toString();

				showInputDialog("�½��ļ���", new String[] { "���������ļ��е�����" },
						CREATE_DIR);
			}
		}));

		dialog = new ProgressDialog(this);
		dialog.setMessage("������...��ȴ�");

		getListView().setOnItemLongClickListener(this);// ��������
		getFileDir(rootPath);// �����Ŀ¼��ʾ

		AppKeyPair appKeyPair = new AppKeyPair(LoginVDiskActivity.CONSUMER_KEY,
				LoginVDiskActivity.CONSUMER_SECRET);
		session = VDiskAuthSession.getInstance(this, appKeyPair,
				AccessType.APP_FOLDER);// ��ͨ������ֻ��ʹ�ô�����
		mApi = new VDiskAPI<VDiskAuthSession>(session);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_vdisk_file, menu);
		return true;
	}

	/**
	 * ����ListItem���ʱҪ���Ķ���
	 * 
	 * ������Ҫ�޸ĳɣ������Ϊ�ļ��оͽ�����ʾ����Ϊ�ļ��͵����˵�ѡ�����ػ���ʾ����ͼ
	 * 
	 * 
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		String filenameString = l.getItemAtPosition(position).toString();
		
		String pathString = cloud_path_edit.getText().toString();
		if (pathString.charAt(pathString.length() - 1) != '/') {
			pathString = pathString + "/";
		}
		cloud_path_edit.setText(pathString + filenameString);

		getFileDir(cloud_path_edit.getText().toString());// �����ļ���·���������ʾ

		// File file = new File(paths.get(position));
		// fileOrDirHandle(file, "short");// �̰�
	}

	/**
	 * ����ListItem���ʱҪ���Ķ���
	 */
	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		String curPath = cloud_path_edit.getText().toString()
				+ arg0.getItemAtPosition(arg2).toString();
		showToast(curPath);

		chooseF(curPath);
		return true;
	}

	private void chooseF(final String curPath) {
		// TODO Auto-generated method stub
		android.content.DialogInterface.OnClickListener listener_list = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					downloadThumbnail(curPath);// �鿴����ͼ
				} else if (which == 1) {
					// ����
				} else if (which == 2) {
					delete(curPath);// ɾ��
				} else if (which == 3) {
					// ����
				}
			}
		};

		String[] list_ctrl = { "�鿴����ͼ", "����", "ɾ��", "����" }; // �����˵�
		/* ѡ��һ������ʱ������Ҫ��δ����������Ƶ�ListDialog */
		new AlertDialog.Builder(VDiskFileActivity.this).setTitle("��ѡ�����")
				.setIcon(R.drawable.ic_launcher)
				.setItems(list_ctrl, listener_list).show();

	}

	/**
	 * ȡ���ļ��ṹ�ķ���������ʾ���б��С������ļ���·������ʾ�б������ļ�����ʾ���ļ���Ϣ
	 * 
	 * @param filePath
	 */
	private void getFileDir(String filePath) {

		cloud_path_edit.setText(filePath);// ����Ŀǰ����·��
		getMetaData(filePath, 0);// ��õ�ǰ·�����ļ����ļ����б�

	}

	// TODO Auto-generated method stub
	/**
	 * ��ȡ�ļ�/�ļ���ԭʼ��Ϣ Get information of file or directory
	 * 
	 * @param path
	 *            �ļ�/�ļ���·��,��ʽΪ "/test/1.jpg",��һ��"/"��ʾ΢�̸�Ŀ¼. Path of file or
	 *            directory, format as "/test/1.jpg", and the first "/"
	 *            represents the root directory of VDisk.
	 * @param type
	 *            0 ��ʾ��ȡ���ļ����������ļ��б���Ϣ;1 ��ʾ��ȡ���ļ�/�ļ��е�ԭʼ��Ϣ. 0 represents to get
	 *            information of the all file list in the directory; 1
	 *            represents to get Original information of the file or
	 *            directory.
	 */
	private void getMetaData(final String path, final int type) {

		dialog.show();
		new Thread() {
			@Override
			public void run() {
				Message msg = new Message();
				Bundle data = new Bundle();
				try {
					Entry metadata = mApi.metadata(path, null, true, false);
					List<Entry> contents = metadata.contents;

					ArrayList<String> list = new ArrayList<String>();

					if (contents != null && type == 0) {
						for (Entry entry : contents) {
							if (entry.isDir) {
								list.add(entry.fileName() + "/");
							} else {
								list.add(entry.fileName());
							}
						}
					} else {// type is 1, to get the detail infomation of the
							// file
						list.add("�ļ���: " + metadata.fileName() + "\n"
								+ "�ļ���С: " + metadata.size + "\n" + "·��: "
								+ metadata.path);
					}
					// startResultActivity(list);

					msg.what = UPDATE_UI;
					ArrayList templist = new ArrayList();
					templist.add(list);
					data.putParcelableArrayList("list", templist);
					msg.setData(data);
					dialog.dismiss();
					handler.sendMessage(msg);

				} catch (VDiskException e) {
					e.printStackTrace();
					msg.what = FAILED;
					data.putSerializable("error", e);
					msg.setData(data);
					handler.sendMessage(msg);
				}
			}

		}.start();
	}

	/**
	 * ɾ���ļ�/�ļ���
	 * 
	 * Delete a file or a directory
	 * 
	 * @param path
	 *            �ļ�/�ļ���·��,��ʽΪ "/test/1.jpg",��һ��"/"��ʾ΢�̸�Ŀ¼.
	 */
	private void delete(final String path) {
		dialog.show();
		new Thread() {
			@Override
			public void run() {
				Message msg = new Message();
				Bundle data = new Bundle();
				try {
					Entry metaData = mApi.delete(path);
					msg.what = SUCCEED;
					data.putString("msg", "�ѳɹ�ɾ��" + metaData.fileName());
				} catch (VDiskException e) {
					e.printStackTrace();
					msg.what = FAILED;
					data.putSerializable("error", e);
				}
				msg.setData(data);
				handler.sendMessage(msg);
			}
		}.start();
	}

	/**
	 * չʾ����ͼ�ĶԻ���
	 * 
	 * AlertDialog to show the thumbnail
	 */
	private void showThumbnailDialog() {

		LayoutInflater inflater = LayoutInflater.from(this);
		View layout = inflater.inflate(R.layout.thumb_dialog, null);

		ImageView imageView = (ImageView) layout.findViewById(R.id.iv_image);
		imageView.setImageDrawable(mDrawable);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		if (layout != null)
			builder.setView(layout);

		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * ��������ͼ
	 * 
	 * Download the thumbnail
	 * 
	 * @param path
	 *            �ļ�/�ļ���·��,��ʽΪ "/test/1.jpg",��һ��"/"��ʾ΢�̸�Ŀ¼. Path of file or
	 *            directory, format as "/test/1.jpg", and the first "/"
	 *            represents the root directory of VDisk.
	 */
	private void downloadThumbnail(final String path) {

		dialog.show();
		new Thread() {
			@Override
			public void run() {
				Message msg = new Message();
				Bundle data = new Bundle();
				FileOutputStream mFos = null;
				try {
					String cachePath = Environment
							.getExternalStorageDirectory().getAbsolutePath()
							+ "/vdisk.thumbnail.jpg";
					try {
						mFos = new FileOutputStream(cachePath, false);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					mApi.getThumbnail(path, mFos, ThumbSize.ICON_640x480, null);
					mDrawable = Drawable.createFromPath(cachePath);
					msg.what = SHOW_THUMBNAIL;
				} catch (VDiskException e) {
					e.printStackTrace();
					msg.what = FAILED;
					data.putSerializable("error", e);
				}
				msg.setData(data);
				handler.sendMessage(msg);
			}
		}.start();
	}

	/**
	 * �����ļ���
	 * 
	 * Create a directory
	 * 
	 * @param path
	 *            ���ļ��е�·��,��ʽΪ "/test",��һ��"/"��ʾ΢�̸�Ŀ¼. Path of new directory,
	 *            format as "/test", and the first "/" represents the root
	 *            directory of VDisk.
	 */
	private void createFolder(final String path) {
		dialog.show();
		new Thread() {
			@Override
			public void run() {
				Message msg = new Message();
				Bundle data = new Bundle();
				try {
					Entry metaData = mApi.createFolder(path);
					msg.what = SUCCEED;
					data.putString("msg", "�ɹ��½��ļ��У�" + metaData.fileName());
				} catch (VDiskException e) {
					e.printStackTrace();
					msg.what = FAILED;
					data.putSerializable("error", e);
				}
				msg.setData(data);
				handler.sendMessage(msg);
			}
		}.start();
	}

	/**
	 * �������ܹ�
	 * 
	 * InputDialog
	 */
	private void showInputDialog(String title, String[] hint, final int type) {

		LayoutInflater inflater = LayoutInflater.from(this);
		final View layout = inflater.inflate(R.layout.input_dialog, null);
		final EditText edt = (EditText) layout.findViewById(R.id.et_input);
		final EditText edt2 = (EditText) layout.findViewById(R.id.et_input2);

		// if (type == RESTORE_VERSION || type == SAVE_TO_VDISK || type == COPY
		// || type == MOVE || type == LARGE_FILE_UPLOAD) {
		// edt2.setVisibility(View.VISIBLE); }

		if (hint != null) {
			edt.setHint(hint[0]);
			if (hint.length == 2) {
				edt2.setHint(hint[1]);
			}
		}

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		builder.setTitle(title);
		if (layout != null)
			builder.setView(layout);
		builder.setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

				String content = edt.getText().toString();
				String content2 = edt2.getText().toString();
				switch (type) {

				case CREATE_DIR:
					// �½��ļ���
					// Create a directory
					if (TextUtils.isEmpty(content)) {
						showToast("���벻��Ϊ�գ�");
					} else {
						String curPath = cloud_path_edit.getText().toString();
						if (curPath.charAt(curPath.length() - 1) != '/') {
							curPath = curPath + "/";
						}
						createFolder(curPath + content);
					}
					break;
				default:
					break;
				}
			}
		});
		builder.setNegativeButton("ȡ��", null);
		final AlertDialog alert = builder.create();
		alert.show();
	}

	private void showToast(String msg) {
		Toast error = Toast.makeText(VDiskFileActivity.this, msg,
				Toast.LENGTH_LONG);
		error.show();
	}

	/**
	 * ��д���ؼ�����:������һ���ļ���
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// ����back��
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			cloud_path_edit = (EditText) findViewById(R.id.path_edit);
			if (cloud_path_edit.getText().toString().equals("")) {
				return super.onKeyDown(keyCode, event);
			}
			if (rootPath.equals(cloud_path_edit.getText().toString())) {
				return super.onKeyDown(keyCode, event);// �����Ϊ��Ŀ¼��ʹ��ԭ���ؼ�����
			} else {

				String curPath = cloud_path_edit.getText().toString();
				int strlen = curPath.length();
				curPath = curPath.substring(0, strlen - 2);
				int index = curPath.lastIndexOf("/");
				String ParPath = curPath.substring(0, index + 1);

				getFileDir(ParPath);// ����ǰ���Ǹ�Ŀ¼�򷵻��ϼ���Ŀ¼
				return true;
			}
			// �������back����������Ӧ
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	/**
	 * �û����һ�η��ؼ����Ͽ�session����
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		session.unlink();
		dialog.dismiss();
		super.onDestroy();
	}

}
