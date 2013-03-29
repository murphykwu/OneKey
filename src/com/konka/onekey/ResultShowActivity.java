package com.konka.onekey;

import com.konka.adapter.InstallAppAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
/**
 * 用来显示安装结果的，安装成功和失败的个数
 * @author konka
 *
 */
public class ResultShowActivity extends Activity {
	private static final String TAG = "onekey";
	private int install_counts = 0;
	private int install_success_counts = 0;
	private int install_failure_counts = 0;
	private TextView tv_install_counts;
	private TextView tv_install_success_counts;
	private TextView tv_install_failure_counts;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_result_show);
		tv_install_counts = (TextView)this.findViewById(R.id.tv_install_counts);
		tv_install_success_counts = (TextView)this.findViewById(R.id.tv_install_success);
		tv_install_failure_counts = (TextView)this.findViewById(R.id.tv_install_failure);
		
		Bundle bundle = getIntent().getExtras();
		install_counts = bundle.getInt(InstallAppAdapter.INSTALL_COUNTS);
		install_success_counts = bundle.getInt(InstallAppAdapter.INSTALL_SUCESS_COUNTS);
		install_failure_counts = bundle.getInt(InstallAppAdapter.INSTALL_FAILURE_COUNTS);
		Log.i(TAG, "the counts = " + install_counts + ", the sucess counts = " + install_success_counts
				+ ", the failure counts = " + install_failure_counts);
		tv_install_counts.setText(Integer.toString(bundle.getInt(InstallAppAdapter.INSTALL_COUNTS)));
		tv_install_success_counts.setText(Integer.toString(bundle.getInt(InstallAppAdapter.INSTALL_SUCESS_COUNTS)));
		tv_install_failure_counts.setText(Integer.toString(bundle.getInt(InstallAppAdapter.INSTALL_FAILURE_COUNTS)));
		
		
		
		
		
	}

	
}



















