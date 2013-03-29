package com.konka.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class ScanApks {
	private static final String TAG = "onekey";
	private List<File> apksList = new ArrayList<File>();
	private List<String> apksNameList = new ArrayList<String>();
	private List<ApkDetails> scanApksList = new ArrayList<ApkDetails>();
	private Context mContext;
	
	public ScanApks(Context mContext)
	{
		this.mContext = mContext;
	}
	/**
	 * 判断外置T卡是否已经装载了，如果没有的话是不能进行T卡扫描的。
	 * @return
	 */
	public boolean isExternalStorageAvailable()
	{		
		return Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}
	
	/**
	 * 可以搜索出storage下面所有的apk文件。返回这个值给主activity。
	 * 为了便于测试，所以传入一个非空的String值。
	 * @return
	 */
	public void scanExternalStorage(String path)
	{
//		String filePath = Environment.getExternalStorageDirectory().toString();
//		Log.i(TAG, "externalDirectory " + filePath); 在4.1系统中路径为：/storage/sdcard0和/storage/sdcard1
//需要扫描/storage下面的所有文件即可。对应4.0以下的即为扫描/mnt/文件夹下面所有文件了。
		if(path != null)
		{
			Log.i(TAG, "transmit unnull path value for test");
			File scanFile = new File(path);
			iteratorDirectory(scanFile, true);
		}else{
			File externalFile = Environment.getExternalStorageDirectory().getParentFile();
			Log.i(TAG, "external Directory " + externalFile.toString());
			iteratorDirectory(externalFile, true);
		}
	}
	
	/**
	 * 迭代当前目录下面的特定文件。联合filefilter使用，可以列出当前目录下面所以特定后缀文件
	 * @param filename 需要迭代的目录名称。深度搜索，但是文件名不一定是安装顺序排列
	 */
	private void iteratorDirectory(File filename, boolean deepth)
	{
		File[] fileLists = filename.listFiles(new MyFileFilter("apk", deepth));
		if(fileLists == null)
			return ;
		for(File fileList : fileLists)
		{
			if(fileList.isDirectory() && !fileList.getName().equals("asec"))
			{
				iteratorDirectory(fileList, deepth);
			}else if(fileList.isDirectory())
			{
				continue;
			}
			//突然发现/storage/sdcard0/powerword/voice/727b79a9a430ccddcf9260a381d5ab10.p这个玩意儿也能够加进去，就太蛋疼了
			//所以加了一个后缀名判断，验证可以排除这个。那上面的那个apk有什么用呢。
			else if(fileList.getName().endsWith("apk"))
			{
				Log.v(TAG, "file add: " + fileList.toString());	
				scanApksList.add(new ApkDetails(fileList, mContext));
				apksList.add(fileList);
				apksNameList.add(fileList.getName());
			}
		}
 
	}
	
	public List<String> getApksNameList()
	{
		return apksNameList;
	}
	
	public List<File> getApksFileList()
	{
		return apksList;
	}
	
	public List<ApkDetails> getScanApksList()
	{
		return scanApksList;
	}
	
	
	

}
