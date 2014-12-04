package com.sina.vdiskexplorer.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
public class MyAdb extends SQLiteOpenHelper{
  
  public MyAdb(Context context){
    //��Android�У���Ϊĳ��Ӧ�ó��򴴽������ݿ⣬ֻ�������Է��ʣ�����Ӧ�ó����ǲ��ܷ��ʵ�
    super(context,"MYADB_FILE", null, 1);
  }
  
@Override
  public void onCreate(SQLiteDatabase db){
    //ִ��ʱ���������ڣ��򴴽�֮��ע��SQLite���ݿ��б�����һ��_id���ֶ���Ϊ�����������ѯʱ������
    String sql = "CREATE TABLE FILESET_TABLE (_ID INTEGER PRIMARY KEY AUTOINCREMENT,ISZOOM INTEGER,ISOPEN INTEGER)";
    db.execSQL(sql);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
    //���ݿⱻ�ı�ʱ����ԭ�ȵı�ɾ����Ȼ�����±�
    String sql = "DROP TABLE IF EXISTS FILESET_TABLE";
    db.execSQL(sql);
    onCreate(db);
  }

  /**
   * ��ѯ���м�¼
   * @return
   */
  public Cursor getFileSet(){
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.query("FILESET_TABLE", null, null, null, null, null, null);
    return cursor;
  }

  /**
   * ������¼
   * @param isFitSizePic �Ƿ�������ʾ
   * @param isOpen       �Ƿ�ֱ�Ӵ�
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
   * ����ID�޸�
   * @param id
   * @param isFitSizePic  0��  1��
   * @param isOpen 0��  1��
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
