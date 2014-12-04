package com.sina.vdiskexplorer;


import com.sina.vdiskexplorer.cloud.VDiskFileActivity;
import com.sina.vdiskexplorer.local.FileActivity;

import android.os.Bundle;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.view.Menu;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class FileTabActivity extends TabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file_tab);
		
		Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost();  // The activity TabHost
        TabSpec spec;
        Intent intent;  // Reusable Intent for each tab
    
      //第一个TAB
        intent = new Intent(this,FileActivity.class);//新建一个Intent用作Tab1显示的内容
        spec = tabHost.newTabSpec("本地文件")//新建一个 Tab
        .setIndicator("本地文件", res.getDrawable(android.R.drawable.ic_menu_manage))//设置名称以及图标
        .setContent(intent);//设置显示的intent，这里的参数也可以是R.id.xxx
        tabHost.addTab(spec);//添加进tabHost
    
        //第二个TAB
        intent = new Intent(this,VDiskFileActivity.class);//第二个Intent用作Tab2显示的内容
        spec = tabHost.newTabSpec("微盘VDisk")//新建一个 Tab
        .setIndicator("微盘VDisk", res.getDrawable(android.R.drawable.ic_dialog_map))//设置名称以及图标
        .setContent(intent);//设置显示的intent，这里的参数也可以是R.id.xxx
        tabHost.addTab(spec);//添加进tabHost
        
        tabHost.setCurrentTab(0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_file_tab, menu);
		return true;
	}

}
