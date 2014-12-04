package com.sina.vdiskexplorer.local;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MyUtil{
  
  /**
   * 判断文件MimeType的方法
   * @param f
   * @param isOpen 目的打开方式为true
   * @return
   */
  public static String getMIMEType(File f,boolean isOpen){
    String type="";
    String fName=f.getName();
    /* 取得扩展名 */
    String end=fName.substring(fName.lastIndexOf(".")+1,fName.length()).toLowerCase();// 考虑后缀名 
    if(isOpen){
            /* 依附档名的类型决定MimeType */
            if(end.equals("m4a")||end.equals("mp3")||end.equals("wma")||end.equals("mid")||end.equals("xmf")||end.equals("ogg")||end.equals("wav")){
              type = "audio"; 
            }else if(end.equals("3gp")||end.equals("mp4")){
              type = "video";
            }
            else if(end.equals("jpg")||end.equals("gif")||end.equals("png")||end.equals("jpeg")||end.equals("bmp")){
              type = "image";
            }
            else{
              /* 如果无法直接打开，就跳出软件列表给用户选择 */
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
   * 缩放图片的方法
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
    //数字越大读出的图片占用的heap越小 不然总是溢出
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
   * 文件大小描述
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
   * 校验输入的文件夹名称是否合法
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
   * 校验输入的文件名称是否合法
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
