package com.sina.vdiskexplorer.local;


import java.io.File;
import java.util.List;

import com.sina.vdiskexplorer.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/* 自定义的Adapter，继承android.widget.BaseAdapter */
//基础数据适配器，它的主要用途是将一组数据传到像ListView、Spinner、Gallery及GridView等UI显示组件
public class FileListAdapter extends BaseAdapter{
  
  private LayoutInflater mInflater;
  private Bitmap mIcon_folder;
  private Bitmap mIcon_file;
  private Bitmap mIcon_image;
  private Bitmap mIcon_audio;
  private Bitmap mIcon_video;
  private Bitmap mIcon_apk;
  private List<String> items;
  private List<String> paths;
  private List<String> sizes;
  private int isZoom = 0;
  
  /* MyAdapter的构造器  */  
  public FileListAdapter(Context context,List<String> it,List<String> pa,List<String> si,int zm){
   
    mInflater = LayoutInflater.from(context);
    items = it;
    paths = pa;
    sizes = si;
    isZoom = zm;
    mIcon_folder = BitmapFactory.decodeResource(context.getResources(),R.drawable.folder);      //文件夹的图文件
    mIcon_file = BitmapFactory.decodeResource(context.getResources(),R.drawable.file);          //文件的图文件
    mIcon_image = BitmapFactory.decodeResource(context.getResources(),R.drawable.image);        //图片的图文件
    mIcon_audio = BitmapFactory.decodeResource(context.getResources(),R.drawable.audio);        //音频的图文件
    mIcon_video = BitmapFactory.decodeResource(context.getResources(),R.drawable.video);        //视频的图文件
    mIcon_apk = BitmapFactory.decodeResource(context.getResources(),R.drawable.apk);            //apk文件
  }   
  
  /* 因继承BaseAdapter，需重写以下方法 */
  @Override
  public int getCount(){
    return items.size();
  }

  @Override
  public Object getItem(int position){
    return items.get(position);
  }
  
  @Override
  public long getItemId(int position){
    return position;
  }
  
  @Override
  public View getView(int position,View convertView,ViewGroup par){
    Bitmap bitMap = null;
    ViewHolder holder = null;
      if(convertView == null){
        /* 使用自定义的list_items作为Layout */
        convertView = mInflater.inflate(R.layout.list_items, null);
        /* 初始化holder的text与icon */
        holder = new ViewHolder();
        holder.f_title = ((TextView) convertView.findViewById(R.id.f_title));
        holder.f_text = ((TextView) convertView.findViewById(R.id.f_text));
        holder.f_icon = ((ImageView) convertView.findViewById(R.id.f_icon)) ;
        convertView.setTag(holder);
      }else{
        holder = (ViewHolder) convertView.getTag();
      }
      File f = new File(paths.get(position).toString());
      /* 设置文件或文件夹的文字与icon */
      holder.f_title.setText(f.getName());
      String f_type = MyUtil.getMIMEType(f,false);
      if(f.isDirectory()){
        holder.f_icon.setImageBitmap(mIcon_folder);
        holder.f_text.setText("");
      }else{
        holder.f_text.setText(sizes.get(position));
        if("image".equals(f_type)){
              if(isZoom == 1){
                bitMap = MyUtil.fitSizePic(f);
                if(bitMap!=null){
                 holder.f_icon.setImageBitmap(bitMap);
                }else{
                  holder.f_icon.setImageBitmap(mIcon_image);
                }
              }else{
                 holder.f_icon.setImageBitmap(mIcon_image);
              }
              bitMap = null;
        }else if("audio".equals(f_type)){
              holder.f_icon.setImageBitmap(mIcon_audio);
        }else if("video".equals(f_type)){
              holder.f_icon.setImageBitmap(mIcon_video);
        }else if("apk".equals(f_type)){
              holder.f_icon.setImageBitmap(mIcon_apk);
        }else{
              holder.f_icon.setImageBitmap(mIcon_file);
        }
      }
    return convertView;
  }
  
  /**
   * 不单独写get set可以提高效率
   * class ViewHolder 
   * */
  private class ViewHolder{
    TextView f_title;
    TextView f_text;
    ImageView f_icon;
  }
}