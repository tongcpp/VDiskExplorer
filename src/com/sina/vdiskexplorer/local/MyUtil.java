package com.sina.vdiskexplorer.local;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MyUtil{
  
  /**
   * �ж��ļ�MimeType�ķ���
   * @param f
   * @param isOpen Ŀ�Ĵ򿪷�ʽΪtrue
   * @return
   */
  public static String getMIMEType(File f,boolean isOpen){
    String type="";
    String fName=f.getName();
    /* ȡ����չ�� */
    String end=fName.substring(fName.lastIndexOf(".")+1,fName.length()).toLowerCase();// ���Ǻ�׺�� 
    if(isOpen){
            /* �������������;���MimeType */
            if(end.equals("m4a")||end.equals("mp3")||end.equals("wma")||end.equals("mid")||end.equals("xmf")||end.equals("ogg")||end.equals("wav")){
              type = "audio"; 
            }else if(end.equals("3gp")||end.equals("mp4")){
              type = "video";
            }
            else if(end.equals("jpg")||end.equals("gif")||end.equals("png")||end.equals("jpeg")||end.equals("bmp")){
              type = "image";
            }
            else{
              /* ����޷�ֱ�Ӵ򿪣�����������б���û�ѡ�� */
              type="*";
            }
            type += "/*"; 
    }else{
          if(end.equals("m4a")||end.equals("mp3")||end.equals("mid")||end.equals("xmf")||end.equals("ogg")||end.equals("wav")){
            type = "audio"; 
          }else if(end.equals("3gp")||end.equals("mp4")){
            type = "video";
          }else if(end.equals("jpg")||end.equals("gif")||end.equals("png")||end.equals("jpeg")||end.equals("bmp")){
            type = "image";
          }else if(end.equals("apk")){
            type = "apk";
          }
    }
    return type; 
  }
  
  /**
   * ����ͼƬ�ķ���
   * @param bitMap
   * @param x
   * @param y
   * @param newWidth
   * @param newHeight
   * @param matrix
   * @param isScale
   * @return
   */
  public static Bitmap fitSizePic(File f){ 
    Bitmap resizeBmp = null;
    BitmapFactory.Options opts = new BitmapFactory.Options(); 
    //����Խ�������ͼƬռ�õ�heapԽС ��Ȼ�������
    if(f.length()<20480){         //0-20k
      opts.inSampleSize = 1;
    }else if(f.length()<51200){   //20-50k
      opts.inSampleSize = 2;
    }else if(f.length()<307200){  //50-300k
      opts.inSampleSize = 4;
    }else if(f.length()<819200){  //300-800k
      opts.inSampleSize = 6;
    }else if(f.length()<1048576){ //800-1024k
      opts.inSampleSize = 8;
    }else{
      opts.inSampleSize = 10;
    }
    resizeBmp = BitmapFactory.decodeFile(f.getPath(),opts);
    return resizeBmp; 
  }

  /**
   * �ļ���С����
   * @param f
   * @return
   */
  public static String  fileSizeMsg(File f){ 
    int sub_index = 0;
    String  show = "";
    if(f.isFile()){
          long length = f.length();
          if(length>=1073741824){
            sub_index = (String.valueOf((float)length/1073741824)).indexOf(".");
            show = ((float)length/1073741824+"000").substring(0,sub_index+3)+"GB";
          }else if(length>=1048576){
            sub_index = (String.valueOf((float)length/1048576)).indexOf(".");
            show =((float)length/1048576+"000").substring(0,sub_index+3)+"MB";
          }else if(length>=1024){
            sub_index = (String.valueOf((float)length/1024)).indexOf(".");
            show = ((float)length/1024+"000").substring(0,sub_index+3)+"KB";
          }else if(length<1024){
            show = String.valueOf(length)+"B";
          }
    }
    return show; 
  }
  
  /**
   * У��������ļ��������Ƿ�Ϸ�
   * @param newName
   * @return 
   */
  public static boolean checkDirPath(String newName){
    boolean ret = false;
    if(newName.indexOf("\\")==-1){
      ret = true;
    }
    return ret;
  }
  
  /**
   * У��������ļ������Ƿ�Ϸ�
   * @param newName
   * @return
   */
  public static boolean checkFilePath(String newName){
    boolean ret = false;
    if(newName.indexOf("\\")==-1){
      ret = true;
    }
    return ret;
  }
}
