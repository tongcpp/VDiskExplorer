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
    
      //��һ��TAB
        intent = new Intent(this,FileActivity.class);//�½�һ��Intent����Tab1��ʾ������
        spec = tabHost.newTabSpec("�����ļ�")//�½�һ�� Tab
        .setIndicator("�����ļ�", res.getDrawable(android.R.drawable.ic_menu_manage))//���������Լ�ͼ��
        .setContent(intent);//������ʾ��intent������Ĳ���Ҳ������R.id.xxx
        tabHost.addTab(spec);//��ӽ�tabHost
    
        //�ڶ���TAB
        intent = new Intent(this,VDiskFileActivity.class);//�ڶ���Intent����Tab2��ʾ������
        spec = tabHost.newTabSpec("΢��VDisk")//�½�һ�� Tab
        .setIndicator("΢��VDisk", res.getDrawable(android.R.drawable.ic_dialog_map))//���������Լ�ͼ��
        .setContent(intent);//������ʾ��intent������Ĳ���Ҳ������R.id.xxx
        tabHost.addTab(spec);//��ӽ�tabHost
        
        tabHost.setCurrentTab(0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_file_tab, menu);
		return true;
	}

}
