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
	VDiskAPI<VDiskAuthSession> mApi;// 重要

	private TextView cloud_path_edit;
	private ImageButton cloud_btn_qry;
	private ImageButton cloud_btn_add;
	private static final String rootPath = "/"; // rootPath：起始文件夹

	private static final String TAG = "VDiskTestActivity";
	private static final int SUCCEED = 0;
	private static final int FAILED = -1;

	private static final int NEW_PICTURE = 1;
	private static final int SHOW_THUMBNAIL = 2;
	private static final int UPDATE_UI = 3;

	private static final int CREATE_DIR = 0;

	private Drawable mDrawable;

	private List<String> items = null; // items：存放显示的名称
	private List<String> paths = null; // paths：存放文件路径
	private List<String> sizes = null; // sizes：文件大小

	private ArrayList<String> list = null;

	ProgressDialog dialog;
	Handler handler = new Handler() {

		public void handleMessage(android.os.Message msg) {// 接收异步函数的消息
			switch (msg.what) {
			case SUCCEED:
				dialog.dismiss();
				showToast(msg.getData().getString("msg"));
				getFileDir(cloud_path_edit.getText().toString());// 删除成功的时候刷新当前显示列表
				break;
			case UPDATE_UI:
				// 在这里更新UI并显示当前目录路径
				ArrayList templist = msg.getData().getParcelableArrayList(
						"list");
				list = (ArrayList<String>) templist.get(0);
				setListAdapter(new ArrayAdapter<String>(VDiskFileActivity.this,
						android.R.layout.simple_list_item_1, list));
				break;
			case SHOW_THUMBNAIL:
				dialog.dismiss();
				if (mDrawable != null)
					showThumbnailDialog();// 暂时不使用
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
					// errMsg = "网络连接异常";
					errMsg = getString(R.string.exception_vdisk_io).toString();
				} else if (e instanceof VDiskParseException) {
					// errMsg = "数据解析异常";
					errMsg = getString(R.string.exception_vdisk_parse)
							.toString();
				} else if (e instanceof VDiskLocalStorageFullException) {
					// errMsg = "本地存储空间已满";
					errMsg = getString(
							R.string.exception_vdisk_local_storage_full)
							.toString();
				} else if (e instanceof VDiskUnlinkedException) {
					// errMsg = "未登录或token过期";
					errMsg = getString(R.string.exception_vdisk_unlinked)
							.toString();
				} else if (e instanceof VDiskFileSizeException) {
					// errMsg = "文件大小超出限制";
					errMsg = getString(R.string.exception_vdisk_file_size)
							.toString();
				} else if (e instanceof VDiskPartialFileException) {
					// errMsg = "传输未完成";
					errMsg = getString(R.string.exception_vdisk_partial_file)
							.toString();
				} else {
					// errMsg = "未知异常";
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
		cloud_btn_qry = (ImageButton) findViewById(R.id.qry_button);// 地址栏右路径定位按钮
		cloud_btn_qry.setOnClickListener((new OnClickListener() {
			public void onClick(View arg0) {
				getFileDir(cloud_path_edit.getText().toString());
			}
		}));

		cloud_btn_add = (ImageButton) findViewById(R.id.add_button);// 新建文件夹
		cloud_btn_add.setOnClickListener((new OnClickListener() {
			public void onClick(View arg0) {
				String curPath = cloud_path_edit.getText().toString();

				showInputDialog("新建文件夹", new String[] { "请输入新文件夹的名字" },
						CREATE_DIR);
			}
		}));

		dialog = new ProgressDialog(this);
		dialog.setMessage("载入中...请等待");

		getListView().setOnItemLongClickListener(this);// 长按功能
		getFileDir(rootPath);// 进入根目录显示

		AppKeyPair appKeyPair = new AppKeyPair(LoginVDiskActivity.CONSUMER_KEY,
				LoginVDiskActivity.CONSUMER_SECRET);
		session = VDiskAuthSession.getInstance(this, appKeyPair,
				AccessType.APP_FOLDER);// 普通开发者只能使用此类型
		mApi = new VDiskAPI<VDiskAuthSession>(session);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_vdisk_file, menu);
		return true;
	}

	/**
	 * 设置ListItem项单击时要做的动作
	 * 
	 * 这里我要修改成：点击若为文件夹就进入显示，若为文件就弹出菜单选择下载或显示缩略图
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

		getFileDir(cloud_path_edit.getText().toString());// 若是文件夹路径则进入显示

		// File file = new File(paths.get(position));
		// fileOrDirHandle(file, "short");// 短按
	}

	/**
	 * 设置ListItem项长按时要做的动作
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
					downloadThumbnail(curPath);// 查看缩略图
				} else if (which == 1) {
					// 下载
				} else if (which == 2) {
					delete(curPath);// 删除
				} else if (which == 3) {
					// 返回
				}
			}
		};

		String[] list_ctrl = { "查看缩略图", "下载", "删除", "返回" }; // 长按菜单
		/* 选择一个程序时，跳出要如何处理启动机制的ListDialog */
		new AlertDialog.Builder(VDiskFileActivity.this).setTitle("请选择操作")
				.setIcon(R.drawable.ic_launcher)
				.setItems(list_ctrl, listener_list).show();

	}

	/**
	 * 取得文件结构的方法，并显示在列表中。若是文件夹路径则显示列表，若是文件则显示该文件信息
	 * 
	 * @param filePath
	 */
	private void getFileDir(String filePath) {

		cloud_path_edit.setText(filePath);// 设置目前所在路径
		getMetaData(filePath, 0);// 获得当前路径下文件及文件夹列表

	}

	// TODO Auto-generated method stub
	/**
	 * 获取文件/文件夹原始信息 Get information of file or directory
	 * 
	 * @param path
	 *            文件/文件夹路径,格式为 "/test/1.jpg",第一个"/"表示微盘根目录. Path of file or
	 *            directory, format as "/test/1.jpg", and the first "/"
	 *            represents the root directory of VDisk.
	 * @param type
	 *            0 表示获取该文件夹下所有文件列表信息;1 表示获取该文件/文件夹的原始信息. 0 represents to get
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
						list.add("文件名: " + metadata.fileName() + "\n"
								+ "文件大小: " + metadata.size + "\n" + "路径: "
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
	 * 删除文件/文件夹
	 * 
	 * Delete a file or a directory
	 * 
	 * @param path
	 *            文件/文件夹路径,格式为 "/test/1.jpg",第一个"/"表示微盘根目录.
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
					data.putString("msg", "已成功删除" + metaData.fileName());
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
	 * 展示缩略图的对话框
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
	 * 下载缩略图
	 * 
	 * Download the thumbnail
	 * 
	 * @param path
	 *            文件/文件夹路径,格式为 "/test/1.jpg",第一个"/"表示微盘根目录. Path of file or
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
	 * 创建文件夹
	 * 
	 * Create a directory
	 * 
	 * @param path
	 *            新文件夹的路径,格式为 "/test",第一个"/"表示微盘根目录. Path of new directory,
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
					data.putString("msg", "成功新建文件夹：" + metaData.fileName());
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
	 * 输入框大总管
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
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

				String content = edt.getText().toString();
				String content2 = edt2.getText().toString();
				switch (type) {

				case CREATE_DIR:
					// 新建文件夹
					// Create a directory
					if (TextUtils.isEmpty(content)) {
						showToast("输入不能为空！");
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
		builder.setNegativeButton("取消", null);
		final AlertDialog alert = builder.create();
		alert.show();
	}

	private void showToast(String msg) {
		Toast error = Toast.makeText(VDiskFileActivity.this, msg,
				Toast.LENGTH_LONG);
		error.show();
	}

	/**
	 * 重写返回键功能:返回上一级文件夹
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// 触发back键
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			cloud_path_edit = (EditText) findViewById(R.id.path_edit);
			if (cloud_path_edit.getText().toString().equals("")) {
				return super.onKeyDown(keyCode, event);
			}
			if (rootPath.equals(cloud_path_edit.getText().toString())) {
				return super.onKeyDown(keyCode, event);// 即如果为根目录则使用原返回键功能
			} else {

				String curPath = cloud_path_edit.getText().toString();
				int strlen = curPath.length();
				curPath = curPath.substring(0, strlen - 2);
				int index = curPath.lastIndexOf("/");
				String ParPath = curPath.substring(0, index + 1);

				getFileDir(ParPath);// 若当前不是根目录则返回上级父目录
				return true;
			}
			// 如果不是back键则正常响应
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	/**
	 * 用户最会一次返回键将断开session连接
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		session.unlink();
		dialog.dismiss();
		super.onDestroy();
	}

}
