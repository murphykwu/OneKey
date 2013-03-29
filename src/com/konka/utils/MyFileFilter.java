package com.konka.utils;

import java.io.File;
import java.io.FileFilter;

public class MyFileFilter implements FileFilter{
	private final static String TAG = "onekey";
	private String searchType = null;
	private boolean deepthSearch = true;
	private String fileSuffixName;	
	
	/**
	 * 
	 * @param type 传入的type可以为"txt"或者"txt/pdf/umd/jar"
	 * @param deepth 决定是否采用深度搜索
	 */
	public MyFileFilter(String type, boolean deepth)
	{
		this.searchType = type;
		this.deepthSearch = deepth;
	}
	
	
	@Override
	public boolean accept(File pathname) {
		// TODO Auto-generated method stub
		//考虑到要进行深度搜索，所以需要
		if(deepthSearch && pathname.isDirectory())
			return true;
		//获取文件的后缀名
		fileSuffixName = pathname.getName();
		fileSuffixName = fileSuffixName.
				substring(fileSuffixName.lastIndexOf(".") + 1, fileSuffixName.length()).
				toLowerCase();
		//Log.i(TAG, "fileSuffixName = " + fileSuffixName + " searchType: " + searchType + " + fileSuffixName:" + fileSuffixName);	

		if(searchType.contains(fileSuffixName))
		{
			return true;
		}
		
		return false;
	}

}
