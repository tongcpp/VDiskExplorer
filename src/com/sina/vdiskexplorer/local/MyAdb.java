package com.sina.vdiskexplorer.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
public class MyAdb extends SQLiteOpenHelper{
  
  public MyAdb(Context context){
    //在Android中，你为某个应用程序创建的数据库，只有它可以访问，其它应用程序是不能访问的
    super(context,"MYADB_FILE", null, 1);
  }
  
@Override
  public void onCreate(SQLiteDatabase db){
    //执行时，若表不存在，则创建之，注意SQLite数据库中必须有一个_id的字段作为主键，否则查询时将报错
    String sql = "CREATE TABLE FILESET_TABLE (_ID INTEGER PRIMARY KEY AUTOINCREMENT,ISZOOM INTEGER,ISOPEN INTEGER)";
    db.execSQL(sql);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
    //数据库被改变时，将原先的表删除，然后建立新表
    String sql = "DROP TABLE IF EXISTS FILESET_TABLE";
    db.execSQL(sql);
    onCreate(db);
  }

  /**
   * 查询所有记录
   * @return
   */
  public Cursor getFileSet(){
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.query("FILESET_TABLE", null, null, null, null, null, null);
    return cursor;
  }

  /**
   * 新增记录
   * @param isFitSizePic 是否缩放显示
   * @param isOpen       是否直接打开
   * @return
   */
  public long insertFileSet(int isZoom,int isOpen){
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues cv = new ContentValues();
    cv.put("ISZOOM",isZoom);
    cv.put("ISOPEN",isOpen);
    long row = db.insert("FILESET_TABLE", null, cv);
    return row;
  }

  /**
   * 根据ID修改
   * @param id
   * @param isFitSizePic  0否  1是
   * @param isOpen 0否  1是
   */
  public long updateFileSet(int id,int isZoom,int isOpen){
    SQLiteDatabase db = this.getWritableDatabase();
    String where = "_ID = ?";
    String[] whereValue = { Integer.toString(id) };
    ContentValues cv = new ContentValues();
    cv.put("ISZOOM",isZoom);
    cv.put("ISOPEN",isOpen);
    long row = db.update("FILESET_TABLE", cv, where, whereValue);
    return row;
  }
 
}
