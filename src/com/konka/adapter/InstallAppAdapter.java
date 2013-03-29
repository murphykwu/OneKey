package com.konka.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.konka.onekey.R;
import com.konka.utils.ApkDetails;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class InstallAppAdapter extends BaseAdapter {
	
	private final static String TAG = "onekey";
	private List<ApkDetails> mData = new ArrayList<ApkDetails>();
	private List<ApkDetails> mInstallSuccess = new ArrayList<ApkDetails>();
	private List<ApkDetails> mInstallFailure = new ArrayList<ApkDetails>();
	private List<String> mStrInstallSuccess = new ArrayList<String>();
	private List<String> mStrInstallFailure = new ArrayList<String>();
	/**
	 * 选中待安装的apk位置
	 */
	private List<Integer> installApks = new ArrayList<Integer>();
	//安装成功和失败的apk列表
	private List<Integer> installSuccess = new ArrayList<Integer>();
	private List<Integer> installFail = new ArrayList<Integer>();
	private HashMap<Integer, Boolean> isSelected;
	LayoutInflater mInflater;
	//以选中的app个数
	private int selectedCounts = 0;
	private int allApkCount = 0;
	private Context mContext;
	public static final String INSTALL_COUNTS = "install_counts";
	public static final String INSTALL_SUCESS_COUNTS = "install_sucess_counts";
	public static final String INSTALL_FAILURE_COUNTS = "install_failure_counts";
	

	public InstallAppAdapter(LayoutInflater inflater, Context mContext, List<ApkDetails> mData)
	{
		mInflater = inflater;
		isSelected = new HashMap<Integer, Boolean>();
		this.mData = mData;
		this.allApkCount = mData.size();
		this.mContext = mContext;
		initSelectedMap();
	}

	public void setListData(List<ApkDetails> data)
	{
		this.mData = data;
		this.notifyDataSetChanged();
	}
	
	public void initSelectedMap()
	{
		int len = mData.size();
		for(int i = 0; i< len; i ++)
		{
			isSelected.put(i, false);
		}
	}
	
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mData.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		//Log.i(TAG, "getView position = " + position);

		ViewHolder viewHolder = new ViewHolder();
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.apps_list_item, null);
			viewHolder.cb_installed = (CheckBox) convertView
					.findViewById(R.id.cb_checkedApp);
			viewHolder.iv_appIcon = (ImageView) convertView
					.findViewById(R.id.iv_app_icon);
			viewHolder.tv_appName = (TextView) convertView
					.findViewById(R.id.tv_app_name);
			viewHolder.tv_appVersion = (TextView)convertView
					.findViewById(R.id.tv_app_version);
			convertView.setTag(viewHolder);
		}else
		{
			viewHolder = (ViewHolder)convertView.getTag();
		}
		
		// 设置应用名称
//		去掉应用名称前面的“名称”这两个。所以去掉
		//String mAppName = mContext.getString(R.string.default_app_name_tag) + mData.get(position).getFile().getName();
		String mAppName = mData.get(position).getFile().getName();
		String mAppVersion = mContext.getString(R.string.default_app_version_tag) + mData.get(position).getAppVersion();
		Log.i("new", "position " + position + " mAppName = " + mAppName);
		viewHolder.iv_appIcon.setImageDrawable(mData.get(position).getApkIcon());//.setImageResource(R.drawable.ic_launcher)
		viewHolder.tv_appName.setText(mAppName);
		viewHolder.tv_appVersion.setText(mAppVersion);
		viewHolder.cb_installed.setChecked(mData.get(position).IsChecked());//isSelected.get(position)
		
		return convertView;		
	}
	
	/**
	 * 设置某一条目checkbox状态
	 * @param mView
	 * @param index
	 */
	public void setItemCheckedBoxState(View mView, int index)
	{
		ViewHolder holder = (ViewHolder)mView.getTag();
		holder.cb_installed.toggle();
		mData.get(index).setIsChecked(holder.cb_installed.isChecked());
		if(holder.cb_installed.isChecked())
		{
			Log.i(TAG, "InstallAppAdapter ADD checked " + mData.get(index).toString());
			//installApks.add(Integer.valueOf(index));
			selectedCounts ++;
		}else
		{
			Log.i(TAG, "InstallAppAdapter REMOVE checked " + mData.get(index).toString());
			//installApks.remove(Integer.valueOf(index));
			selectedCounts --;
		}
	}
	
	/**
	 * 设置全选、取消全选。需要修改安装列表和选中的个数
	 * @param mFlag
	 */
	public void setAllCheckboxStatus(Boolean mFlag)
	{
		int allApkCounts = mData.size();
		//取消全选
		if(!mFlag)
		{
			selectedCounts = 0;
			//installApks.clear();
			Log.i(TAG, "setAllCheckboxStatus unSelectAll!");
			for(int i = 0; i < allApkCounts; i ++)
			{
				mData.get(i).setIsChecked(false);
			}
		}else//全选所有
		{
			selectedCounts = mData.size();
			//installApks.clear();
			for(int i = 0; i < allApkCounts; i ++)
			{	
			//	installApks.add(Integer.valueOf(i));
				mData.get(i).setIsChecked(true);
			}
			Log.i(TAG, "setAllCheckboxStatus selectall");
		}		
	}
	
	/**
	 * 在每次批量安装之前必须清空安装成功和失败列表，因为
	 * 每次安装必须重新计数
	 */
	public void clearSuceesAndFailList()
	{
		mStrInstallFailure.clear();
		mStrInstallSuccess.clear();
	}
	
	/**
	 * 获取以选中的应用个数，直接返回待安装的list长度即可。防止多个变量的修改
	 * @return
	 */
	public int getCheckedCounts()
	{//installApks.size()
		return selectedCounts;
	}
	
	/**
	 * 获取已经选中的要安装的应用列表
	 * @return
	 */
	public List<Integer> getInstallApks()
	{
		return installApks;
	}
	
//	public void addSuccessList(Integer add)
//	{
//		installSuccess.add(add);
//	}

	/**
	 * 重载一下上面的函数。如果验证通过就不需要上面的了。
	 * 由于异步原因，mInstallSuccess这个列表并无正确，不过数量应该是ok的。
	 * @param sucessApk
	 */
	public void addSuccessList(ApkDetails sucessApk)
	{
		mInstallSuccess.add(sucessApk);
	}
	
	/**
	 * 重载一下上面的函数。如果验证通过就不需要上面的了。
	 * 由于异步原因，mInstallSuccess这个列表并无正确，不过数量应该是ok的。
	 * @param sucessApk
	 */
	public void addSuccessList(String sucessApk)
	{
		mStrInstallSuccess.add(sucessApk);
	}
	
	public void removeSuccessList(ApkDetails add)
	{
		installSuccess.remove(add);
	}
	
	public void addFailList(Integer add)
	{
		installFail.add(add);
	}
	/**
	 * 重载一下安装失败列表的操作。
	 * @param failure
	 */
	public void addFailList(ApkDetails failure)
	{
		mInstallFailure.add(failure);
	}
	
	/**
	 * 重载一下安装失败列表的操作。
	 * @param failure
	 */
	public void addFailList(String failure)
	{
		mStrInstallFailure.add(failure);
	}
	
	public void removeFailList(ApkDetails add)
	{
		installFail.remove(add);
	}
	/**
	 * 获取成功安装的应用列表
	 * @return
	 */
	public List<Integer> getSuccessList()
	{
		return installSuccess;
	}
	
	/**
	 * 获取安装成功的应用个数
	 * @return
	 */
	public int getSucessCounts()
	{
		//return installSuccess.size();
		return mStrInstallSuccess.size();
		//return mSetInstallSuccess.size();
		
	}
	
	public int getFailureCounts()
	{
		//return installSuccess.size();
		return mStrInstallFailure.size();
		
	}
	
	/**
	 * 获取安装失败的应用列表
	 * @return
	 */
	public List<Integer> getFialList()
	{
		return installFail;
	}
	
	public int getAllApkCounts()
	{
		return mData.size();
	}
	
	/**
	 * 当执行删除函数的时候，同时删除选中应用程序列表中的应用
	 * @param apkIndex
	 */
	public void removeApk(int apkIndex)
	{
//		Log.i(TAG, "apkIndex = " + apkIndex);
//		for(int i = 0; i < installApks.size(); i ++)
//		{
//			Log.i(TAG, "removeApk---installApks index = " + i + ", contents = " + installApks.get(i));
//		}
		
		//去掉指定位置的内容
		Log.i(TAG, "removeApk mData.size() = " + mData.size());
		//如果删除的是以选中的apk，那么还必须改变选中的数值了。
		if(mData.get(apkIndex).IsChecked())
		{
			selectedCounts --;
		}
		mData.remove(apkIndex);
		Log.i(TAG, "removeApk mData.size()2 = " + mData.size());
		//判断指定位置是否在installApks和installSuccess这两个列表里面。伪代码为：
		//判断删除序号和当前内容，如果一样，说明选中的里面包含当前删除的，直接remove掉他。如果取出的比当前删除的小
		//说明需要将存储的序号减1
//		int installApksSize = installApks.size();
//		for(int i = 0; i < installApksSize; i ++)
//		{
//			//清楚installApks相关的数据，移除之后会导致该list长度减少，所以需要调整list长度和当前处理到的元素
//			if(apkIndex == installApks.get(i))
//			{
//				Log.i(TAG, "installApks size = " + installApks.size());
//				//由于之前没有将apkIndex转化成Integer对象，导致remove掉的不是list内置的对象，而是指定位置的对象
//				//导致异常情况
//				installApks.remove(new Integer(apkIndex));
//				i --;
//				installApksSize --;
//			}
//			//重新调整installapks列表里面存储的mData中的对象下标
//			if((i >= 0) && (apkIndex < installApks.get(i)))
//			{
//				Log.i(TAG, "i else if = " + i + ", the contents = " + (installApks.get(i) -1));
//				installApks.set(i, new Integer(installApks.get(i) -1));
//			}
//		}

	}
	
	
	private final static class ViewHolder
	{
		CheckBox cb_installed;
		ImageView iv_appIcon;
		TextView tv_appName;
		TextView tv_appVersion;
	}

}













