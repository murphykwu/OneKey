package com.konka.adapter;

import java.util.ArrayList;

import com.konka.onekey.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class InstallResultAdapter extends BaseAdapter {

	LayoutInflater mInflater;
	private ArrayList<String> mShowData;
	private Context mContext;
	
	public InstallResultAdapter(LayoutInflater inflater, Context context, ArrayList<String> showData)
	{
		this.mInflater = inflater;
		this.mContext = context;
		this.mShowData = showData;
	}
	
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mShowData.size();
	}

	@Override
	public Object getItem(int arg0) {
		// TODO Auto-generated method stub
		return mShowData.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		// TODO Auto-generated method stub
		return arg0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		ViewHolder viewHolder = new ViewHolder();
		if(convertView == null)
		{
			convertView = mInflater.inflate(R.layout.result_list_item, null);
			viewHolder.iv_appIcon = (ImageView)convertView.findViewById(R.id.iv_app_icon);
			viewHolder.tv_appFileName = (TextView)convertView.findViewById(R.id.tv_app_file);
			viewHolder.tv_reason = (TextView)convertView.findViewById(R.id.tv_app_reason);
			convertView.setTag(viewHolder);
			
		}else
		{
			viewHolder = (ViewHolder)convertView.getTag();
		}
		
		String[] tempStr = mShowData.get(position).split("&");
		String appName, appReason;
		if (tempStr != null) {
			appName = tempStr[0];
			appReason = tempStr[1];
		}else
		{
			appName = null;
			appReason = null;
		}
		viewHolder.tv_appFileName.setText(appName);
		
		viewHolder.tv_reason.setText(appReason);
		
		return convertView;
	}
	
	
	
	private final static class ViewHolder
	{
		ImageView iv_appIcon;
		TextView tv_appFileName;
		//应用安装失败原因
		TextView tv_reason;
	}
	

}
