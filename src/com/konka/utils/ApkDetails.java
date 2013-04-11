package com.konka.utils;

import java.io.File;
import java.util.ArrayList;

import com.konka.onekey.R;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageParser;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

/**
 * 该类用来获取一个apk文件的图标、应用信息、包信息等。
 * 应用信息是一些在AndroidManifest.xml里面的application标签所包含的信息。
 * 包信息是从androidmanifest.xml里面收集的信息，当然包含应用信息。
 * @author {wphoenix9}
 *
 */
public class ApkDetails{
	private static final String TAG = "onekey";
	private static final boolean DBG = true;
	private PackageInfo packageInfo;
	private ApplicationInfo appInfo;
	private Context mContext;
	//apk文件的绝对路径
	private String absPath;
	private PackageManager pm;
	private String appName = null;
	private String packageName = null;
	private String version = "1.0";
	private Drawable icon1, icon2;
	private File apks = null;
//	//判断是否已经选中进行安装
//	private boolean isChecked = false;
	private Uri mPackageURI = null;
	private PackageParser.Package mPkgInfo = null;
	private int installFlag = 0;
	//用来确定该应用是否需要安装
	private boolean ifInstall = false;
	//安装是否成功
	private boolean success = false;
	
	/**
	 * 为包名+应用版本号。通过该key来作为应用在hashmap中的键。
	 * 每个应用的包名都是不一样的。但是应用名称可能一样，所以用这种组合的方式
	 * 保证key的独一性。如果该key值一样，那么只保留一个文件。不管文件名称是否相同。
	 */
	private String app_key;	
	
	public ApkDetails(File apks, Context mContent)
	{
		this.apks = apks;
		this.mContext = mContent;
		this.absPath = apks.getAbsolutePath();
		this.pm = mContent.getPackageManager();
		parserApkDetail();
	}
	
	/**
	 * 解析apk信息，并且提取其中的一些信息。
	 * 应用名称、应用图标、应用版本
	 */
	public void parserApkDetail()
	{
		packageInfo = pm.getPackageArchiveInfo(absPath, PackageManager.GET_ACTIVITIES);
		if(packageInfo != null)
		{
			appInfo = packageInfo.applicationInfo;
			appInfo.sourceDir = absPath;
			appInfo.publicSourceDir = absPath;
			appName = pm.getApplicationLabel(appInfo).toString();
			packageName = appInfo.packageName;
			version = packageInfo.versionName;
			icon1 = pm.getApplicationIcon(appInfo);
			icon2 = appInfo.loadIcon(pm);
			String pkgInfoStr = String.format("PackageName: %s, Version: %s, AppName:%s", packageName, version, appName);
			if(DBG) Log.i(TAG, String.format("PkgInfo: %s", pkgInfoStr));
			
			mPackageURI = Uri.fromFile(apks);
			mPkgInfo = PackageUtil.getPackageInfo(mPackageURI);
			app_key = packageName + version;
		}else
		{
			if(DBG) Log.e(TAG, "packageInfo is null");
		}
	}
	
	/**
	 * 返回应用key，作为键值。
	 * @return
	 */
	public String getAppKey()
	{
		return app_key;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		String pkgInfoStr = String.format("PackageName: %s, Version: %s, AppName:%s", packageName, version, appName);
		return String.format("PkgInfo: %s", pkgInfoStr);
	}

	/**
	 * 获取应用图标
	 * @return
	 */
	public Drawable getApkIcon()
	{
		if(icon1 != null)
			return icon1;
		if(icon2 != null)
			return icon2;
		return mContext.getResources().getDrawable(R.drawable.ic_action_search);
	}
	
	public String getAppName()
	{
		if(appName == null)
		{
//			appName = apks.getName();使用文件名代替。不是很好，放弃该方法
			appName = mContext.getResources().getString(R.string.unknownApp);
		}
		return appName;
	}
	
	public String getPackageName()
	{
		if(getApplicationInfo() != null)
			return getApplicationInfo().packageName;//packageName;
		else
			return null;
	}
	
	public String getAppVersion()
	{
		if(getPackageInfo() != null)
			return getPackageInfo().versionName;
		else
			return null;
	}
	
	public PackageInfo getPackageInfo()
	{
		return pm.getPackageArchiveInfo(getAppPath(), PackageManager.GET_ACTIVITIES);//packageInfo
	}
	
	public ApplicationInfo getApplicationInfo()
	{
		if(getPackageInfo() != null)
			return getPackageInfo().applicationInfo;
		else
			return null;
	}
	public void setApplicationInfo(ApplicationInfo ai)
	{
		appInfo = ai;
	}
	
	public boolean IsChecked()
	{//isChecked
		return ifInstall;
	}
	
	public void setIsChecked(boolean isChecked)
	{
		this.ifInstall = isChecked;
	}
	
	public Uri getPackageURI()
	{
		return Uri.fromFile(apks);
	}
	
	public PackageParser.Package getPackage()
	{
		return PackageUtil.getPackageInfo(Uri.fromFile(apks));
	}
	
	/**
	 * 获取安装表示位
	 * @return 返回安装标志位
	 */
	public int getInstallFlag()
	{
		mPkgInfo = getPackage();
		try {
			PackageInfo pi = pm.getPackageInfo(mPkgInfo.applicationInfo.packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
			if(pi != null)
			{
				installFlag |= PackageManager.INSTALL_REPLACE_EXISTING;
				if(DBG) Log.i(TAG, "Replacing package: " + mPkgInfo.applicationInfo.packageName);
			}
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if((mPkgInfo.applicationInfo.installLocation == PackageInfo.INSTALL_LOCATION_INTERNAL_ONLY)
				|| (mPkgInfo.applicationInfo.installLocation == PackageInfo.INSTALL_LOCATION_UNSPECIFIED)
				|| (mPkgInfo.applicationInfo.installLocation == PackageInfo.INSTALL_LOCATION_AUTO))
		{
			installFlag |= PackageManager.INSTALL_INTERNAL;
			installFlag &= ~PackageManager.INSTALL_EXTERNAL;
		}else{
			installFlag |= PackageManager.INSTALL_EXTERNAL;
			installFlag &= ~PackageManager.INSTALL_INTERNAL;
		}
		if(DBG) Log.i(TAG, mPkgInfo.applicationInfo.installLocation + "--" + mPkgInfo.applicationInfo.packageName);
		
		ArrayList<PackageParser.Activity> activityArray = mPkgInfo.receivers;
		int N = activityArray.size();
		for(int i = 0; i < N; i ++)
		{
			ArrayList<PackageParser.ActivityIntentInfo> intents = activityArray.get(i).intents;
			int intentN = intents.size();
			for(int j = 0; j < intentN; j ++)
			{
				if(DBG) Log.i(TAG, "Intent--" + intents.get(j).toString());
				if(intents.get(j).hasAction("Android.appwidget.action.APPWIDGET_UPDATE"))
				{
					//如果包含widget控件，那么置标志位为INSTALL_INTERNAL，并且不能够置为INSTALL_EXTERNAL;
					if(DBG) Log.i(TAG, "Widget Intent--" + intents.get(j).toString());
					installFlag |= PackageManager.INSTALL_INTERNAL;
					installFlag &= ~PackageManager.INSTALL_EXTERNAL;
					return installFlag;
				}
			}
		}
		
		return installFlag;
	}
	
	public File getFile()
	{
		return apks;
	}
	public void setFile(File value)
	{
		apks = value;
	}
	
	/**
	 * 获取该文件的绝对路径
	 * @return
	 */
	public String getAppPath()
	{
		return apks.getAbsolutePath();
	}
	
	/**
	 * 设定该应用是否安装成功
	 * @return
	 */
	public boolean isSuccess()
	{
		return success;
	}
	
	public void setSucess(boolean isSucess)
	{
		success = isSucess;
	}
}




















