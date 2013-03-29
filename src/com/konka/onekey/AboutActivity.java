package com.konka.onekey;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * 关于 菜单 Demo
 * 2013.1.18
 * @author zhangshaopeng
 */

public class AboutActivity extends Activity{
    /** Called when the activity is first created. */
	
	private String TAG="AboutActivity";
	
	/*返回到上一层界面*/
	private ImageView about_return;
	/*显示软件版本号*/
	private TextView show_sv;
	//Muse官方论坛
	private ImageView MS_bbs;
	//新浪微博地址
	private ImageView XL_wb;
	//腾讯微博地址
	private ImageView QQ_wb;
	//关于文字
	private TextView about_text;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.about_activity_main);
        
        Log.i(TAG,"entry AboutActivity");
        
        
       about_init(); 
       aboutreturn();
       show_software_version();
       link_musebbs();
       link_xlwb();
       link_qqwb();
       
       Link_abtext();
       
    }

	private void about_init() {
	
		about_return=(ImageView)findViewById(R.id.ab_return);
        show_sv=(TextView)findViewById(R.id.ab_st_version);
		MS_bbs=(ImageView)findViewById(R.id.museui_bbs);
		XL_wb=(ImageView)findViewById(R.id.xinlang_wb);
		QQ_wb=(ImageView)findViewById(R.id.qq_wb);
		
		about_text=(TextView)findViewById(R.id.myAboutTextView);
		
	}
	//-----------------------------
	/*返回到上一层界面*/
	private void aboutreturn() {
		
		about_return.setOnClickListener(return_click);
		             
	}
	private OnClickListener return_click=new Button.OnClickListener(){

		@Override
		public void onClick(View v) {
			
//			Intent intent=new Intent();
//			intent.setClass(AboutActivity.this, AboutDemoActivity.class);
//			startActivity(intent);
			AboutActivity.this.finish();
		}
	};

	
	//-----------------------------
	//由于返回的图片太小，现设定按“关于”时也是返回功能
	private void Link_abtext(){
		about_text.setOnClickListener(returntext_click);
		
	}
	private OnClickListener returntext_click=new Button.OnClickListener(){

		@Override
		public void onClick(View v) {
			
//			Intent intent=new Intent();
//			intent.setClass(AboutActivity.this, AboutDemoActivity.class);
//			startActivity(intent);
			AboutActivity.this.finish();
		}
		
	};
	
   //------------------------------
	/*显示软件版本号*/

	private void show_software_version() {

		show_sv.setText(getVersionName());
	}
	
	   //	获取当前应用的版本号：

	private String getVersionName()
	{
	// 获取packagemanager的实例
	    PackageManager packageManager = getPackageManager();
	// getPackageName()是你当前类的包名，0代表是获取版本信息
	    PackageInfo packInfo = null;
		try {
			packInfo = packageManager.getPackageInfo(getPackageName(),0);
		} catch (NameNotFoundException e) {

			e.printStackTrace();
		}
	    String version = packInfo.versionName;
	    if(version == null)
	    {
	    	version = "";
	    }
	
   	Log.i(TAG,"software version="+version);
	
     	return version;
	}  	
    //-------------------------------
    //MuseUI官方论坛地址
	private void link_musebbs() {
		
		MS_bbs.setOnClickListener(ms_click);
		
	}
	private OnClickListener ms_click=new Button.OnClickListener(){
		@Override
		public void onClick(View v) {
			
			Uri uri = Uri.parse("http://www.museui.com");  
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
		}
	};
	
	//------------------------------------
	//新浪微博MuseUI官方地址
	private void link_xlwb() {
		
		XL_wb.setOnClickListener(xl_click);
		
	}
	private OnClickListener xl_click=new Button.OnClickListener(){
		@Override
		public void onClick(View v) {
			
			Uri uri = Uri.parse("http://e.weibo.com/u/2916538744");  
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
		}
	};
	
	//------------------------------------
   // 腾讯微博MuseUI官方地址

	private void link_qqwb() {
		
		QQ_wb.setOnClickListener(qq_lick);
	}
	private OnClickListener qq_lick=new Button.OnClickListener(){
		@Override
		public void onClick(View v) {
			
			Uri uri = Uri.parse("http://e.t.qq.com/heidi_KONKA");  
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
		}
	};
	

}
