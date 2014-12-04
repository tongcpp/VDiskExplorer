package com.sina.vdiskexplorer.local;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.sina.vdiskexplorer.LoginVDiskActivity;
import com.sina.vdiskexplorer.R;
import com.vdisk.android.VDiskAuthSession;
import com.vdisk.net.VDiskAPI;
import com.vdisk.net.session.AppKeyPair;
import com.vdisk.net.session.Session.AccessType;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;

public class FileActivity extends ListActivity implements
		OnItemLongClickListener {

	VDiskAuthSession session;
	VDiskAPI<VDiskAuthSession> mApi;// 重要

	private List<String> items = null; // items：存放显示的名称
	private List<String> paths = null; // paths：存放文件路径
	private List<String> sizes = null; // sizes：文件大小
	private String rootPath = "/"; // rootPath：起始文件夹
	private TextView path_edit;
	private View myView;
	private TextView new_textView;
	private EditText myEditText;
	private RadioGroup radioGroup;
	private RadioButton rb_file;
	private RadioButton rb_dir;
	private ImageButton rb_qry;
	protected final static int MENU_ADD = Menu.FIRST; // 新建文件/文件夹
	protected final static int MENU_SET = Menu.FIRST + 1;// 设置(我希望只保留是否显示缩略图选项就可以了)
	protected final static int MENU_ABOUT = Menu.FIRST + 2; // 关于
	private MyAdb db;
	private CheckBox cb_open;
	private CheckBox cb_zoom;
	private Cursor myCursor;
	private int id = 0;
	private int isZoom = 0;
	private int isOpen = 0;


	/**
	 * MENU键功能
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		/* 添加MENU */
		menu.add(Menu.NONE, MENU_ADD, 0, R.string.dirAddButton);
		menu.add(Menu.NONE, MENU_SET, 0, R.string.dirSetButton);
		menu.add(Menu.NONE, MENU_ABOUT, 0, R.string.dirAboutButton);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case MENU_ADD:
			newDirOrFile();// 新建文件/文件夹
			break;
		case MENU_SET:
			set();// 设置
			break;
		case MENU_ABOUT:
			about();// 关于
			break;
		}
		return true;
	}

	/**
	 * 重写返回键功能:返回上一级文件夹
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// 是否触发按键为back键
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			path_edit = (EditText) findViewById(R.id.path_edit);// 绑定地址路径控件//而且path_edit是一个TextView变量
			File file = new File(path_edit.getText().toString());// 两步方法才能取得string形式的当前路径信息
			if (rootPath.equals(path_edit.getText().toString())) {
				return super.onKeyDown(keyCode, event);// 即如果为根目录则使用原返回键功能，这里暂时指示默认为退出程序
			} else {
				getFileDir(file.getParent());// 若当前不是根目录则返回上级父目录
				return true;
			}
			// 如果不是back键则正常响应
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.file_selected);
		db = new MyAdb(this);// 详情查看MyAdb.java自写数据库类
		myCursor = db.getFileSet();// Cursor类型变量，可理解为数据源集合
		if (myCursor.moveToFirst()) {// 定位第一行
			id = myCursor.getInt(myCursor.getColumnIndex("_ID"));// 通过列名取数据
			isZoom = myCursor.getInt(myCursor.getColumnIndex("ISZOOM"));
			isOpen = myCursor.getInt(myCursor.getColumnIndex("ISOPEN"));
		} else {
			db.insertFileSet(isZoom, isOpen);// 新增记录
			myCursor = db.getFileSet();// 查询所有记录
			myCursor.moveToFirst();// 定位第一行
			id = myCursor.getInt(myCursor.getColumnIndex("_ID"));
		}

		path_edit = (EditText) findViewById(R.id.path_edit);// 又绑定一次
		rb_qry = (ImageButton) findViewById(R.id.qry_button);// 地址栏右路径定位按钮
		rb_qry.setOnClickListener(listener_qry);// 按钮的监听定义就在下面
		getListView().setOnItemLongClickListener(this);// Get the activity's
		// list view
		// widget.增加了长按功能
		getFileDir(rootPath);// 进入根目录显示

		AppKeyPair appKeyPair = new AppKeyPair(LoginVDiskActivity.CONSUMER_KEY,
				LoginVDiskActivity.CONSUMER_SECRET);
		session = VDiskAuthSession.getInstance(this, appKeyPair,
				AccessType.APP_FOLDER);//普通开发者只能使用此类型
		mApi = new VDiskAPI<VDiskAuthSession>(session);

	}

	Button.OnClickListener listener_qry = new Button.OnClickListener() {
		public void onClick(View arg0) {
			File file = new File(path_edit.getText().toString());
			if (file.exists()) {
				if (file.isFile()) {
					openFile(file);// 若输入的是具体文件的话就直接调用打开了
				} else {
					getFileDir(path_edit.getText().toString());// 若是文件夹路径则进入显示
				}
			} else {
				Toast.makeText(FileActivity.this, "找不到该位置,请确定位置是否正确!",
						Toast.LENGTH_SHORT).show();
			}
		}
	};

	/**
	 * 设置ListItem被单击时要做的动作
	 * 
	 * 这里我要修改成：点击若为文件夹就进入，若为文件就弹出菜单：
	 * 1.选定，给出文件路径及文件名2.预览（打开图片或者音乐或者给打开方式供选择）3.取消返回
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {// 这个形参我熟
		File file = new File(paths.get(position));
		fileOrDirHandle(file, "short");// 短按
	}

	/**
	 * 设置ListItem被长按时要做的动作 是不是长按也可以给予选定/预览/取消的功能，有待商榷
	 */
	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		File file = new File(paths.get(arg2));
		fileOrDirHandle(file, "long");// 长按
		return true;
	}

	/**
	 * 处理文件或者目录的方法
	 * 可能在这里进行文件组单击和长按的具体修改
	 * 
	 * @param file
	 * @param flag
	 *            
	 */
	private void fileOrDirHandle(final File file, String flag) {
		/* 点击文件时的OnClickListener */
		OnClickListener listener_list = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					copyFileOrDir(file);// 复制
				} else if (which == 1) {
					moveFileOrDir(file);// 移动
				} else if (which == 2) {
					modifyFileOrDir(file);// 重命名
				} else if (which == 3) {
					delFileOrDir(file);// 删除
					/* 新加入 */
				} else if (which == 4) {
					uptoVDisk(file);// 上传
				}
			}
		};

		if (flag.equals("long")) {
			String[] list_file = { "复制", "移动", "重命名", "删除", "上传到微盘" }; // file操作
			String[] list_dir = { "复制", "移动", "重命名", "删除", }; // directory操作
			/* 选择一个文件或者目录时，跳出要如何处理的ListDialog */
			if (file.isFile()) {
				new AlertDialog.Builder(FileActivity.this)
						.setTitle(file.getName())
						.setIcon(R.drawable.list)
						.setItems(list_file, listener_list)
						.setPositiveButton("返回",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
									}
								}).show();
			}
			if (file.isDirectory()) {
				new AlertDialog.Builder(FileActivity.this)
						.setTitle(file.getName())
						.setIcon(R.drawable.list)
						.setItems(list_dir, listener_list)
						.setPositiveButton("返回",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
									}
								}).show();
			}
		} else {// 单击
			if (file.isDirectory()) {// 若为文件夹
				getFileDir(file.getPath());// 进入
			} else {
				openFile(file);// 打开文件
			}
		}
	}

	/**
	 * 取得文件结构的方法，并显示在列表中
	 * 
	 * @param filePath
	 */
	private void getFileDir(String filePath) {
		/* 设置目前所在路径 */
		path_edit.setText(filePath);
		// 三个List类型
		items = new ArrayList<String>();
		paths = new ArrayList<String>();
		sizes = new ArrayList<String>();// 文件大小，文件夹大小为空
		File f = new File(filePath);
		File[] files = f.listFiles();
		if (files != null) {
			/* 将所有文件添加ArrayList中 */
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {// 先遍历文件夹
					items.add(files[i].getName());
					paths.add(files[i].getPath());
					sizes.add("");
				}
			}
			for (int i = 0; i < files.length; i++) {// 再遍历文件
				if (files[i].isFile()) {
					items.add(files[i].getName());
					paths.add(files[i].getPath());
					sizes.add(MyUtil.fileSizeMsg(files[i]));
				}
			}
		}
		/* 使用自定义的MyAdapter来将数据传入ListActivity */
		setListAdapter(new FileListAdapter(this, items, paths, sizes, isZoom));
	}

	/**
	 * 新建文件夹或者文件
	 */
	private void newDirOrFile() {
		AlertDialog nameDialog = new AlertDialog.Builder(FileActivity.this)
				.create();
		LayoutInflater factory = LayoutInflater.from(FileActivity.this);
		/* 初始化myChoiceView，使用new_alert为layout */
		myView = factory.inflate(R.layout.new_alert, null);
		new_textView = (TextView) myView.findViewById(R.id.new_view);
		rb_dir = (RadioButton) myView.findViewById(R.id.newdir_radio);
		rb_file = (RadioButton) myView.findViewById(R.id.newfile_radio);
		radioGroup = (RadioGroup) myView.findViewById(R.id.new_radio);
		myEditText = (EditText) myView.findViewById(R.id.new_edit);
		path_edit = (EditText) findViewById(R.id.path_edit); // 当前所在路径
		/* 将原始文件名先放入EditText中 */
		nameDialog.setView(myView);
		// 单选按扭选择
		radioGroup
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						if (checkedId == rb_file.getId()) {
							new_textView.setText("新建文件:");
						} else if (checkedId == rb_dir.getId()) {
							new_textView.setText("新建文件夹:");
						}
					}
				});

		/* 新建文件夹的确认提示 */
		nameDialog.setButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				final int checkedId = radioGroup.getCheckedRadioButtonId(); // 选择的文件夹或者文件标记
				final String newName = myEditText.getText().toString(); // 取得创建的文件夹或者文件名
				final String newPath = path_edit.getText().toString() + "/"
						+ newName; // 新的文件夹或者文件路径
				final File f_new = new File(newPath);
				if (f_new.exists()) {
					Toast.makeText(FileActivity.this,
							"指定文件'" + newName + "'与现有文件重名,请指定另一名称!",
							Toast.LENGTH_LONG).show();
					return;
				} else {
					new AlertDialog.Builder(FileActivity.this)
							.setTitle("注意")
							.setIcon(R.drawable.alert)
							.setMessage(
									"确定创建"
											+ ((checkedId == rb_dir.getId()) ? "文件夹"
													: "文件") + "'" + newName
											+ "' 吗?")
							.setPositiveButton("确定",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
											if (checkedId == rb_dir.getId()) {
												if (MyUtil
														.checkDirPath(newPath)) {
													if (f_new.mkdirs()) {
														Toast.makeText(
																FileActivity.this,
																"已创建!",
																Toast.LENGTH_SHORT)
																.show();
														getFileDir(f_new
																.getParent());
													} else {
														Toast.makeText(
																FileActivity.this,
																"出错!",
																Toast.LENGTH_SHORT)
																.show();
													}
												} else {
													Toast.makeText(
															FileActivity.this,
															"请输入正确的格式(不包含//)!",
															Toast.LENGTH_SHORT)
															.show();
												}
											} else {
												if (MyUtil
														.checkFilePath(newPath)) {
													if (newFile(f_new)) {
														Toast.makeText(
																FileActivity.this,
																"已创建!",
																Toast.LENGTH_SHORT)
																.show();
														getFileDir(f_new
																.getParent());
													} else {
														Toast.makeText(
																FileActivity.this,
																"出错!",
																Toast.LENGTH_SHORT)
																.show();
													}
												} else {
													Toast.makeText(
															FileActivity.this,
															"请输入正确的格式(不包含//)!",
															Toast.LENGTH_SHORT)
															.show();
												}
											}
										}
									})
							.setNegativeButton("取消",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
										}
									}).show();
				}
			}
		});
		nameDialog.setButton2("取消", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		nameDialog.show();
	}

	/**
	 * 修改文件名或者文件夹名
	 * 
	 * @param f
	 */
	private void modifyFileOrDir(File f) {
		final File f_old = f;
		LayoutInflater factory = LayoutInflater.from(FileActivity.this);
		myView = factory.inflate(R.layout.rename_alert, null);
		myEditText = (EditText) myView.findViewById(R.id.rename_edit);
		myEditText.setText(f_old.getName());
		OnClickListener listenerFileEdit = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				/* 取得修改后的文件路径 */
				final String modName = myEditText.getText().toString(); // 取得修改的文件名
				final String pFile = f_old.getParentFile().getPath() + "/"; // 取得该文件路径
				final String newPath = pFile + modName; // 新的文件路径+文件名
				final File f_new = new File(newPath);
				if (f_new.exists()) {
					if (!modName.equals(f_old.getName())) {
						Toast.makeText(FileActivity.this,
								"指定文件'" + modName + "'与现有文件重名,请指定另一名称!",
								Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(FileActivity.this, "名称未修改!",
								Toast.LENGTH_SHORT).show();
					}
				} else {
					new AlertDialog.Builder(FileActivity.this)
							.setTitle("注意")
							.setIcon(R.drawable.alert)
							.setMessage(
									"确定要修改"
											+ (f_old.isDirectory() ? "文件夹'"
													: "文件'") + f_old.getName()
											+ "'名称为'" + modName + "'吗?")
							.setPositiveButton("确定",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
											if (f_old.isDirectory()) {
												if (MyUtil
														.checkDirPath(newPath)) {
													if (f_old.renameTo(f_new)) {
														Toast.makeText(
																FileActivity.this,
																"已修改!",
																Toast.LENGTH_SHORT)
																.show();
														/* 重新产生文件列表的ListView即重新显示上一级文件夹下文件 */
														getFileDir(pFile);
													} else {
														Toast.makeText(
																FileActivity.this,
																"出错!",
																Toast.LENGTH_SHORT)
																.show();
													}
												} else {
													Toast.makeText(
															FileActivity.this,
															"请输入正确的格式(不包含//)!",
															Toast.LENGTH_SHORT)
															.show();
												}
											} else {
												if (MyUtil
														.checkFilePath(newPath)) {
													if (f_old.renameTo(f_new)) {
														Toast.makeText(
																FileActivity.this,
																"已修改!",
																Toast.LENGTH_SHORT)
																.show();
														getFileDir(pFile);
													} else {
														Toast.makeText(
																FileActivity.this,
																"出错!",
																Toast.LENGTH_SHORT)
																.show();
													}
												} else {
													Toast.makeText(
															FileActivity.this,
															"请输入正确的格式(不包含//)!",
															Toast.LENGTH_SHORT)
															.show();
												}
											}
										}
									})
							.setNegativeButton("取消",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
										}
									}).show();
				}
			};
		};

		/* 设置更改文件名点击确定后的Listener */
		AlertDialog renameDialog = new AlertDialog.Builder(FileActivity.this)
				.create();
		renameDialog.setView(myView);
		renameDialog.setButton("确定", listenerFileEdit);
		renameDialog.setButton2("取消", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		renameDialog.show();
	}

	/**
	 * 复制文件或者文件夹
	 * 
	 * @param file
	 */
	public void copyFileOrDir(File f) {
		final File f_old = f;
		LayoutInflater factory = LayoutInflater.from(FileActivity.this);
		myView = factory.inflate(R.layout.copy_alert, null);
		myEditText = (EditText) myView.findViewById(R.id.copy_edit);
		myEditText.setText(f.getParent());
		OnClickListener listenerCopy = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String new_path = myEditText.getText().toString();
				if (new_path.endsWith(File.separator)) {
					new_path = new_path + f_old.getName();
				} else {
					new_path = new_path + File.separator + f_old.getName();
				}
				final File f_new = new File(new_path);
				if (f_new.exists()) {
					Toast.makeText(FileActivity.this,
							"指定文件'" + f_new.getName() + "'与现有文件重名,请指定另一名称!",
							Toast.LENGTH_SHORT).show();
				} else {
					new AlertDialog.Builder(FileActivity.this)
							.setTitle("注意")
							.setIcon(R.drawable.alert)
							.setMessage(
									"确定要把"
											+ (f_old.isDirectory() ? "文件夹"
													: "文件") + "'"
											+ f_old.getName() + "'复制到'"
											+ f_new.getParent() + "'吗?")
							.setPositiveButton("确定",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
											if (f_old.isDirectory()) {
												if (MyUtil.checkDirPath(f_new
														.getPath())) {
													if (copyDir(
															f_old.getPath(),
															f_new.getParent())) {
														Toast.makeText(
																FileActivity.this,
																"已复制!",
																Toast.LENGTH_SHORT)
																.show();
														getFileDir(f_new
																.getParent());
													} else {
														Toast.makeText(
																FileActivity.this,
																"出错!",
																Toast.LENGTH_SHORT)
																.show();
													}
												} else {
													Toast.makeText(
															FileActivity.this,
															"请输入正确的格式(不包含//)!",
															Toast.LENGTH_SHORT)
															.show();
												}
											} else {
												if (MyUtil.checkFilePath(f_new
														.getPath())) {
													if (copyFile(
															f_old.getPath(),
															f_new.getParent())) {
														Toast.makeText(
																FileActivity.this,
																"已复制!",
																Toast.LENGTH_SHORT)
																.show();
														getFileDir(f_new
																.getParent());
													} else {
														Toast.makeText(
																FileActivity.this,
																"出错!",
																Toast.LENGTH_SHORT)
																.show();
													}
												} else {
													Toast.makeText(
															FileActivity.this,
															"请输入正确的格式(不包含//)!",
															Toast.LENGTH_SHORT)
															.show();
												}
											}
										}
									})
							.setNegativeButton("取消",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
										}
									}).show();

				}
			}
		};

		// 设置复制点击确定后的Dialog
		AlertDialog copyDialog = new AlertDialog.Builder(FileActivity.this)
				.create();
		copyDialog.setView(myView);
		copyDialog.setButton("确定", listenerCopy);
		copyDialog.setButton2("取消", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		copyDialog.show();
	}

	/**
	 * 移动文件或者文件夹
	 * 
	 * @param file
	 */
	public void moveFileOrDir(File f) {
		final File f_old = f;
		LayoutInflater factory = LayoutInflater.from(FileActivity.this);
		myView = factory.inflate(R.layout.move_alert, null);
		myEditText = (EditText) myView.findViewById(R.id.move_edit);
		myEditText.setText(f_old.getParent());
		OnClickListener listenerMove = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String new_path = myEditText.getText().toString();
				if (new_path.endsWith(File.separator)) {
					new_path = new_path + f_old.getName();
				} else {
					new_path = new_path + File.separator + f_old.getName();
				}
				final File f_new = new File(new_path);
				if (f_new.exists()) {
					Toast.makeText(FileActivity.this,
							"指定文件'" + f_new.getName() + "'与现有文件重名,请指定另一名称!",
							Toast.LENGTH_SHORT).show();
				} else {
					new AlertDialog.Builder(FileActivity.this)
							.setTitle("注意")
							.setIcon(R.drawable.alert)
							.setMessage(
									"确定要把"
											+ (f_old.isDirectory() ? "文件夹"
													: "文件") + "'"
											+ f_old.getName() + "'移动到'"
											+ f_new.getParent() + "'吗?")
							.setPositiveButton("确定",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
											if (f_old.isDirectory()) {
												if (MyUtil.checkDirPath(f_new
														.getPath())) {
													if (moveDir(
															f_old.getPath(),
															f_new.getParent())) {
														Toast.makeText(
																FileActivity.this,
																"已移动!",
																Toast.LENGTH_SHORT)
																.show();
														getFileDir(f_new
																.getParent());
													} else {
														Toast.makeText(
																FileActivity.this,
																"出错!",
																Toast.LENGTH_SHORT)
																.show();
													}
												} else {
													Toast.makeText(
															FileActivity.this,
															"请输入正确的格式(不包含//)!",
															Toast.LENGTH_SHORT)
															.show();
												}
											} else {
												if (MyUtil.checkDirPath(f_new
														.getPath())) {
													if (moveFile(
															f_old.getPath(),
															f_new.getParent())) {
														Toast.makeText(
																FileActivity.this,
																"已移动!",
																Toast.LENGTH_SHORT)
																.show();
														getFileDir(f_new
																.getParent());
													} else {
														Toast.makeText(
																FileActivity.this,
																"出错!",
																Toast.LENGTH_SHORT)
																.show();
													}
												} else {
													Toast.makeText(
															FileActivity.this,
															"请输入正确的格式(不包含//)!",
															Toast.LENGTH_SHORT)
															.show();
												}
											}
										}
									})
							.setNegativeButton("取消",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
										}
									}).show();
				}
			}
		};

		// 设置移动点击确定后的Dialog
		AlertDialog moveDialog = new AlertDialog.Builder(FileActivity.this)
				.create();
		moveDialog.setView(myView);
		moveDialog.setButton("确定", listenerMove);
		moveDialog.setButton2("取消", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		moveDialog.show();
	}

	/**
	 * 删除文件或者文件夹
	 * 
	 * @param f
	 */
	private void delFileOrDir(File f) {
		final File f_del = f;
		new AlertDialog.Builder(FileActivity.this)
				.setTitle("注意")
				.setIcon(R.drawable.alert)
				.setMessage(
						"确定要删除" + (f_del.isDirectory() ? "文件夹'" : "文件'")
								+ f_del.getName() + "'吗?")
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						/* 删除文件或者文件夹 */
						if (f_del.isDirectory()) {
							if (delDir(f_del)) {
								Toast.makeText(FileActivity.this, "已删除!",
										Toast.LENGTH_SHORT).show();
								getFileDir(f_del.getParent());
							} else {
								Toast.makeText(FileActivity.this, "出错!",
										Toast.LENGTH_SHORT).show();
							}
						} else {
							if (delFile(f_del)) {
								Toast.makeText(FileActivity.this, "已删除!",
										Toast.LENGTH_SHORT).show();
								getFileDir(f_del.getParent());
							} else {
								Toast.makeText(FileActivity.this, "出错!",
										Toast.LENGTH_SHORT).show();
							}
						}
					}
				})
				.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}

	/**
	 * 上传选定文件到微盘
	 * 
	 * @param f
	 */
	private void uptoVDisk(File f) {
		final File f_del = f;
		new AlertDialog.Builder(FileActivity.this).setTitle("确认")
				.setIcon(R.drawable.alert)
				.setMessage("确定要上传文件‘" + f_del.getName() + "'吗?")
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						/* 选定文件 */
						//String srcPath;
						//if(rootPath.equals(path_edit.getText().toString())){
						//	srcPath = path_edit.getText().toString() + f_del.getName();
						//}
						//else{
						//srcPath = path_edit.getText().toString() + "/"+ f_del.getName();}
						
						//File file = new File(srcPath);
						//Toast.makeText(getApplicationContext(),
						//		"源文件地址"+ srcPath, Toast.LENGTH_SHORT)
						//		.show();
						String desPath = "/";//暂时先上传到根目录
						LargeFileUpload upload = new LargeFileUpload(FileActivity.this, mApi, desPath, f_del);
						upload.execute();

					}

				})
				.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}

	/**
	 * 设置
	 */
	public void set() {
		LayoutInflater factory = LayoutInflater.from(FileActivity.this);// 设置菜单采用了多选按钮
		myView = factory.inflate(R.layout.setview, null);
		cb_open = (CheckBox) myView.findViewById(R.id.checkOpen);
		cb_zoom = (CheckBox) myView.findViewById(R.id.checkZoom);
		if (isZoom == 1) {
			cb_zoom.setChecked(true);
		}
		if (isOpen == 1) {
			cb_open.setChecked(true);
		}
		OnClickListener listenerSet = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				new AlertDialog.Builder(FileActivity.this)
						.setTitle("注意")
						.setIcon(R.drawable.alert)
						.setMessage("确定要保存设置吗?")
						.setPositiveButton("确定",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										if (cb_zoom.isChecked()) {
											isZoom = 1;
										} else {
											isZoom = 0;
										}
										if (cb_open.isChecked()) {
											isOpen = 1;
										} else {
											isOpen = 0;
										}
										if (db.updateFileSet(id, isZoom, isOpen) != 0) {
											Toast.makeText(FileActivity.this,
													"已设置!", Toast.LENGTH_SHORT)
													.show();
											getFileDir(rootPath);
										} else {
											Toast.makeText(FileActivity.this,
													"出错!", Toast.LENGTH_SHORT)
													.show();
										}
									}
								})
						.setNegativeButton("取消",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
									}
								}).show();
			}
		};

		AlertDialog setDialog = new AlertDialog.Builder(FileActivity.this)
				.create();
		setDialog.setView(myView);
		setDialog.setButton("确定", listenerSet);
		setDialog.setButton2("取消", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		setDialog.show();
	}

	/**
	 * 新建文件
	 * 
	 * @param file
	 * @return
	 */
	public boolean newFile(File f) {
		try {
			f.createNewFile();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * 复制单个文件
	 * 
	 * @param oldPath
	 *            String 原文件路径 如：/xx
	 * @param newPath
	 *            String 复制后路径 如：/xx/ss
	 * @return boolean
	 */
	public boolean copyFile(String oldPath, String newPath) {
		try {
			int bytesum = 0;
			int byteread = 0;
			String f_new = "";
			File f_old = new File(oldPath);
			if (newPath.endsWith(File.separator)) {
				f_new = newPath + f_old.getName();
			} else {
				f_new = newPath + File.separator + f_old.getName();
			}
			new File(newPath).mkdirs(); // 如果文件夹不存在 则建立新文件夹
			new File(f_new).createNewFile(); // 如果文件不存在 则建立新文件
			// 文件存在时
			if (f_old.exists()) {
				InputStream inStream = new FileInputStream(oldPath); // 读入原文件
				FileOutputStream fs = new FileOutputStream(f_new);
				byte[] buffer = new byte[1444];
				while ((byteread = inStream.read(buffer)) != -1) {
					bytesum += byteread; // 字节数 文件大小
					fs.write(buffer, 0, byteread);
				}
				inStream.close();
			}
		} catch (Exception e) {
			return false;

		}
		return true;
	}

	/**
	 * 复制文件夹
	 * 
	 * @param oldPath
	 *            String 原文件路径 如：/aa/bb 11,22
	 * @param newPath
	 *            String 复制后路径 如：/ss/cc
	 * @return boolean
	 */
	public boolean copyDir(String oldPath, String newPath) {
		try { // 要复制的文件夹 /aa/bb---[1.txt,rr]
			File f_old = new File(oldPath);
			String d_old = "";
			String d_new = newPath + File.separator + f_old.getName(); // 新文件夹路径
			// 传入/cc/dd
			// 转为/cc/dd/bb
			new File(d_new).mkdirs(); // 如果文件夹不存在 则建立新文件夹 //建立/cc/dd/bb文件夹
			File[] files = f_old.listFiles();
			for (int i = 0; i < files.length; i++) {
				d_old = oldPath + File.separator + files[i].getName(); // 要复制的文件夹下的文件：/aa/bb/1.txt,文件夹：/aa/bb/rr
				if (files[i].isFile()) {
					copyFile(d_old, d_new);
				} else {
					copyDir(d_old, d_new);
				}
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * 移动文件到指定目录
	 * 
	 * @param oldPath
	 *            String 如：/fqf.txt
	 * @param newPath
	 *            String 如：/xx/fqf.txt
	 */
	public boolean moveFile(String oldPath, String newPath) {
		boolean ret = false;
		try {
			if (copyFile(oldPath, newPath)) {
				new File(oldPath).delete();
				ret = true;
			}
		} catch (Exception e) {
			return false;
		}
		return ret;
	}

	/**
	 * 移动文件夹到指定目录
	 * 
	 * @param oldPath
	 *            String 如：/xx
	 * @param newPath
	 *            String 如：/cc/xx
	 */
	public boolean moveDir(String oldPath, String newPath) {
		boolean ret = false;
		try {
			if (copyDir(oldPath, newPath)) {
				if (delDir(new File(oldPath))) {
					ret = true;
				}
			}
		} catch (Exception e) {
			return false;
		}
		return ret;
	}

	/**
	 * 删除单个文件
	 * 
	 * @param file
	 * @return
	 */
	public boolean delFile(File f) {
		boolean ret = false;
		try {
			if (f.exists()) {
				f.delete();
				ret = true;
			}
		} catch (Exception e) {
			return false;
		}
		return ret;
	}

	/**
	 * 删除文件夹
	 * 
	 * @param file
	 * @return
	 */
	public boolean delDir(File f) {
		boolean ret = false;
		try {
			if (f.exists()) {
				File[] files = f.listFiles();
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						if (!delDir(files[i])) {
							return false;
						}
					} else {
						files[i].delete();
					}
				}
				f.delete(); // 删除空文件夹
				ret = true;
			}
		} catch (Exception e) {
			return false;
		}
		return ret;
	}

	/**
	 * 打开文件
	 * 
	 * @param f
	 */
	private void openFile(File f) {
		Intent intent = new Intent();// 使用了intent，我要好好看看关于转换界面的关系
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);
		// 跳出列表供选择
		String type = "*/*";
		if (isOpen == 0) {
			type = MyUtil.getMIMEType(f, true);
		}
		// 设置intent的file与MimeType
		intent.setDataAndType(Uri.fromFile(f), type);
		startActivity(intent);
	}

	/**
	 * 关于
	 */
	private void about() {
		new AlertDialog.Builder(FileActivity.this).setTitle("关于")
				.setMessage("本地目录及文件操作模块\nvia SinaWT ")
				.setPositiveButton("返回", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).show();
	}

	@Override
	protected void onDestroy() {
		// 物理返回键将注销账户
		session.unlink();
		super.onDestroy();
	}

}