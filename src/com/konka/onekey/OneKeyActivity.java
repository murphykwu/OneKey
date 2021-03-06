package com.konka.onekey;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.konka.onekey.AboutActivity;
import com.konka.onekey.R;
import com.konka.adapter.InstallAppAdapter;
import com.konka.utils.ApkDetails;
import com.konka.utils.PackageUtil;
import com.konka.utils.ScanApks;

/**
 * 当T卡拔出状态的时候，是否需要关闭本软件。
 * 因为软件扫描的是T卡上面的apk文件。如果继续显示那么会导致安装失败。
 * 不如直接退出，然后提示用户。
 * @author konka
 *
 */
public class OneKeyActivity extends Activity {
	
	private static final String TAG = "onekey";
	private static final boolean DBG = true;
	private Button bt_selectAll, bt_install;
	private TextView tv_selected_apps;
	private Context mContext;
	//检测点击两次退出按钮才退出程序
	private long mFirstCancelTime = 0;
	
	private ListView lv_installApps;
	//该变量存放待安装和安装成功的应用列表信息。
	private InstallAppAdapter iaa;
	//扫描到的所有apk信息都放在mData里面
	private List<ApkDetails> mData = new ArrayList<ApkDetails>();
	private int apksCounts = 0;
	private WakeLock mWakeLock;
	private PackageManager mPm;
	private int installindex = 0;
	private final int INSTALL_COMPLETE = 1;
	private final int SCAN_COMPLETE = 2;
	private final int UPDATE_BUTTON_TEXT = 3;
	int mInstallComplete = 0;
	//标识长按的listview里面的条目
	private int mLongClickSelectedFileIndex = 0;
	private static final int CHANNELSEARCH = 1;
	private static final int INSTALLINGDIALOG = 2;
	private static final int DIALOG_OPERATION_MENU = 3;
	private static final int DIALOG_YES_NO = 4;
	private static final int DIALOG_RENAME = 5;
	private static final int DIALOG_SHARE = 6;
	private static final int DIALOG_DETAIL = 7;
	
	private static final int OPERATION_DELETE = 0;
	private static final int OPERATION_RENAME = OPERATION_DELETE + 1;
	private static final int OPERATION_SHARE = OPERATION_DELETE + 2;
	private static final int OPERATION_DETAIL = OPERATION_DELETE + 3;
	
	private File mLongClickFile = null;
	private EditText mRename;
	private int mSelectedDialog;
	private ProgressDialog dialog;
	private ProgressDialog proDialog;
	private ProgressDialog searchDialog;
	private ScanApks sa;
	private boolean mTFUnmounted = false;
	//监控apk安装完成这个动作
	private PackageInstallObserver observer = new PackageInstallObserver();
	
	private BroadcastReceiver mUnMountReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if((action.equals("android.intent.action.MEDIA_EJECT"))
					|| (action.equals("android.intent.action.MEDIA_UNMOUNTED"))
					|| (action.equals("android.intent.action.MEDIA_SHARED")))
			{
				Log.i(TAG, "detect sd card action = " + action);
				mTFUnmounted = true;
				Toast.makeText(mContext, getString(R.string.sd_statechange), Toast.LENGTH_LONG).show();
				//可以采取延时关闭程序的方式，避免突然关闭造成很突然的感觉
				unregisterReceiver(mUnMountReceiver);
				finish();
			}else if(action.equals("android.intent.action.MEDIA_MOUNTED"))
			{//检测到载入信息
				mTFUnmounted = false;
			}
			
			mTFUnmounted = true;
		}
	};
	private IntentFilter mUnMountFilter;
	
	/**
	 * A: 该值用来存储所有理论上不重复的应用apk。该应用“包名&版本号”作为唯一的key。
	 * 中间加&可以用来解析包名和版本号。用简单的split即可。用作以后其他用途。
	 * 如果有应用文件名不一样，但是key是一样的，那么不重复显示了，完全没有意义。
	 * 如果有应用包名一样，但是版本号不一样，那么依然显示，因为用户可能会选择装低版本的软件。
	 * 
	 * B: 或者可以用文件路径来作为key。这样所有的apk就都会显示出来。如果之前安装了，继续覆盖安装。
	 * 暂时不需要这个处理过程。因为产品要求所有的apk都要列出来。
	 */
//	private HashMap<String, ApkDetails> allAppMap = new HashMap<String, ApkDetails>();
	
	private Boolean mAllSelected = false;
	private List<String> mTargetLauncher = null;

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		//每次进入的时候必须检查T卡状态。这样比较好。
		String externalState = Environment.getExternalStorageState();
		Log.i(TAG, "onstart，确认修改1externalState = " + externalState);
		if((externalState.equals(Environment.MEDIA_UNMOUNTED)) 
				|| (externalState.equals(Environment.MEDIA_SHARED))
				|| (externalState.equals(Environment.MEDIA_CHECKING)))
		{//T卡已经卸载、处于大容量存储模式、T卡正在载入，请等待载入完毕再运行一键安装程序
			Toast.makeText(mContext, mContext.getString(R.string.sd_unmounted), Toast.LENGTH_LONG ).show();
//			unregisterReceiver(mUnMountReceiver);
			finish();
		}
		
		//注册监听器
//		Log.i(TAG, "onStart Environment.getExternalStorageState() = " + Environment.getExternalStorageState());
		mUnMountFilter = new IntentFilter(Intent.ACTION_MEDIA_UNMOUNTED);
		mUnMountFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		mUnMountFilter.addAction(Intent.ACTION_MEDIA_SHARED);//当以大容量存储模式共享外部存储空间的时候触发
		mUnMountFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		mUnMountFilter.addDataScheme("file");		
		registerReceiver(mUnMountReceiver, mUnMountFilter);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.i(TAG, "---------------onPause");
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "--------onCreate");
        setContentView(R.layout.activity_one_key);
        mContext = this.getApplicationContext();
        PowerManager powerManager = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Installapk");
        bt_selectAll = (Button)findViewById(R.id.bt_select);
        bt_install = (Button) findViewById(R.id.bt_install);
        bt_selectAll.setOnClickListener(cl);
        bt_install.setOnClickListener(cl);
        tv_selected_apps = (TextView)findViewById(R.id.tv_number);
        lv_installApps = (ListView)findViewById(R.id.lv_installApps);
        lv_installApps.setOnItemClickListener(icl);
        //设定长按的响应函数
        lv_installApps.setOnItemLongClickListener(ilcl);
        proDialog = new ProgressDialog(OneKeyActivity.this);
        searchDialog = new ProgressDialog(mContext);
        
        sa = new ScanApks(mContext);
        removeDialog(CHANNELSEARCH);
        showDialog(CHANNELSEARCH);
        //需要开一个新的线程进行文件扫描
        new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				//后面的地址是测试专用
				sa.scanExternalStorage(null);//"/storage/sdcard0/test"
				Message msg = new Message();
				msg.what = SCAN_COMPLETE;
				mHandler.sendMessage(msg);
			}
        }).start();
        iaa = new InstallAppAdapter(getLayoutInflater(), this, mData);
        apksCounts = mData.size();
        lv_installApps.setAdapter(iaa);
    }

   
    OnClickListener cl = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId())
			{
			//安装所有选择的apk文件
			case R.id.bt_install:
				//显示所有在安装列表里面的程序信息，installList
//				installList = iaa.getInstallApks();
				//如果安装个数为0，提示用户选中一个应用再安装
				if(iaa.getCheckedCounts() == 0)/*installList.isEmpty()*/
				{
					Toast.makeText(mContext, mContext.getResources().getString(R.string.no_app_selected), Toast.LENGTH_SHORT).show();
					break;
				}
//				Log.i(TAG, "show the progress Dialog");
				proDialog.setMessage(mContext.getResources().getString(R.string.installing));
				proDialog.show();
//				如果不使用下面的方式，这个progressdialog就不能够显示出来。原因未知
				new Thread(new Runnable(){
					@Override
					public void run() {
						// TODO Auto-generated method stub						
						try {
							//installBatch2();	
//							Log.i(TAG, "onclicklistner start to install apk batch");
					    	mWakeLock.acquire();					    	
					    	Log.i(TAG, "----> installApkBatch mData size = " + mData.size());
					    	mPm = getPackageManager();
					    	//PackageInstallObserver observer = new PackageInstallObserver();
					    	/*每次批量安装之前，必须先将安装完成的这个标志位置零, 并且清空安装成功列表，安装失败列表*/
					    	mInstallComplete = 0;
					    	//初始化当前安装的应用下标
					    	installindex = 0;
					    	iaa.clearSuceesAndFailList();
							installApkBatch(installindex);
						} catch (Exception e) {//InterruptedException
							// TODO Auto-generated catch block
							Log.i(TAG, "e.printStackTrace()");
							e.printStackTrace();
						}						
					}
					
				}).start();
				break;
			//根据当前的选择状态做出全选和取消全选的操作
			case R.id.bt_select:
				//获取当前选中的个数，依据该数据判断是否是全选
				int selectedNum = iaa.getCheckedCounts();
				Log.i(TAG, "selected num = " + selectedNum + "iaa.getAllApkCounts = " + iaa.getAllApkCounts());
				if(selectedNum != iaa.getAllApkCounts())
				{//如果选中个数跟当前不一样，说明点击之前显示的是全选，所以触发的是全选，随后显示取消全选
					iaa.setAllCheckboxStatus(true);
					tv_selected_apps.setVisibility(View.VISIBLE);
					tv_selected_apps.setText("" + iaa.getCheckedCounts());
					bt_selectAll.setText(R.string.unSelectAll);
					setAllListViewState(true);
				}else
				{//如果是一样，说明之前按钮显示的是取消全选，说明当前触发的是取消全选，随后显示全选
					iaa.setAllCheckboxStatus(false);					
					tv_selected_apps.setVisibility(View.INVISIBLE);
					bt_selectAll.setText(R.string.selectAll);
					setAllListViewState(false);
				}		
				break;
			}
		}
	};
	
	/**
	 *  显示安装结果窗口。显示安装成功和失败个数等信息
	 */
	private void showResultScreen()
	{
		mWakeLock.release();
		Bundle bundle = new Bundle();
		bundle.putInt(InstallAppAdapter.INSTALL_COUNTS, iaa.getCheckedCounts());
		bundle.putInt(InstallAppAdapter.INSTALL_SUCCESS_COUNTS, iaa.getSucessCounts());
		bundle.putInt(InstallAppAdapter.INSTALL_FAILURE_COUNTS, iaa.getFailureCounts());
		//将成功和失败列表都传入下一个activity
		bundle.putStringArrayList(InstallAppAdapter.INSTALL_FAILURE_LIST, iaa.getFailureStrList());
		bundle.putStringArrayList(InstallAppAdapter.INSTALL_SUCCESS_LIST, iaa.getSuccessStrList());
		Log.i(TAG, iaa.getCheckedCounts() + "    ," + iaa.getSucessCounts());
		Intent intent = new Intent(OneKeyActivity.this, ResultShowActivity.class);
		intent.putExtras(bundle);
		startActivity(intent);		
	}
	
	
	/**
	 * 用来设置所有的listview项目全选或者取消全选。
	 * @param status 需要设置的状态。true 全选；false 取消全选
	 */
	public void setAllListViewState(boolean status)
	{
		int counts = lv_installApps.getChildCount();
		for(int i = 0; i < counts; i ++)
		{			
			final LinearLayout layout = (LinearLayout)lv_installApps.getChildAt(i);
			CheckBox cb = (CheckBox)layout.findViewById(R.id.cb_checkedApp);
			cb.setChecked(status);
		}
	}
	
	/**
	 * 根据各种点击事件更新按钮上面的文字
	 */
	public void updateBtnText()
	{
		//根据选中条目判断按钮显示的文字，判断选中的数目是否跟显示的数目一样
		int counts = iaa.getCheckedCounts();
		if(counts == apksCounts)
		{
			mAllSelected = true;
			bt_selectAll.setText(R.string.unSelectAll);
		}else
		{
			mAllSelected = false;
			bt_selectAll.setText(R.string.selectAll);
		}
		//显示按钮上面的数字
		if(iaa.getCheckedCounts() == 0)
		{
			tv_selected_apps.setVisibility(View.INVISIBLE);
		} else {
			tv_selected_apps.setVisibility(View.VISIBLE);
			tv_selected_apps.setText(counts + "");
		}
	}

	
	/**
	 * 响应listview中单项的点击响应。当被点击时，需要设置单项的checkbox状态，按钮文字显示，选中的程序列表。
	 */
	OnItemClickListener icl = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub
			//将点击记录放入mCheckedObj列表。没有选中的item position是不会出现在这个列表中的。
			mAllSelected = false;
			iaa.setItemCheckedBoxState(view, position);

			//如果在这里面更新主UI，会导致延迟。所以谨慎在子线程里面更新主线程UI。需要用
			//Message通知主界面更新UI
			//updateBtnText();
			Message msg = new Message();
			msg.what = UPDATE_BUTTON_TEXT;
			mHandler.sendMessage(msg);
//			//根据选中条目判断按钮显示的文字，判断选中的数目是否跟显示的数目一样
//			int counts = iaa.getCheckedCounts();
//			if(counts == apksCounts)
//			{
//				mAllSelected = true;
//				bt_selectAll.setText(R.string.unSelectAll);
//			}else
//			{
//				mAllSelected = false;
//				bt_selectAll.setText(R.string.selectAll);
//			}
//			//显示按钮上面的数字
//			if(iaa.getCheckedCounts() == 0)
//			{
//				tv_selected_apps.setVisibility(View.INVISIBLE);
//			} else {
//				tv_selected_apps.setVisibility(View.VISIBLE);
//				tv_selected_apps.setText(counts + "");
//			}
		}
	};
	
	AdapterView.OnItemLongClickListener ilcl = new AdapterView.OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			// TODO Auto-generated method stub
			
			//该变量非常重要，标识当前点击的是哪个选项所生成的对话框ocl对象
			//既是监听所有生成的AlertDialog的对象
			mSelectedDialog = DIALOG_OPERATION_MENU;
			mLongClickFile = mData.get(arg2).getFile();
			mLongClickSelectedFileIndex = arg2;
//			Log.i(TAG, "arg2 = " + arg2 + ", mLongClickFile = " + mLongClickFile.getName() + ", the File is = " + mData.get(arg2).getFile().toString());
			showDialog(DIALOG_OPERATION_MENU);
			return false;
		}
	};
	
	DialogInterface.OnClickListener ocl = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface arg0, int arg1) {
			// TODO Auto-generated method stub
			switch(mSelectedDialog)
			{
			case DIALOG_OPERATION_MENU:
				dialogLongClickMenuOnClick(arg1);
				break;
			case DIALOG_YES_NO:
				dialogYesNoOnClick(arg1);
				break;
			case DIALOG_RENAME:
				dialogRenameOnClick(arg1);
				break;
			case DIALOG_SHARE:
				break;			
			}
			
		}
	};
	
	private void dialogYesNoOnClick(int which)
	{
		if(DialogInterface.BUTTON_POSITIVE == which)
		{
			delete();
		}
	}
	
	/**
	 * 修改选中的文件。重命名。由于直接取的是apk的应用名称，所以是否有必要重命名还需斟酌。
	 * @param which 指定修改的项
	 */
	private void dialogRenameOnClick(int which)
	{
		if(DialogInterface.BUTTON_POSITIVE == which)
		{
			String newName = mRename.getText().toString().trim();
			//如果用户设置成空，那么就不修改文件名了
			if(newName.equals(""))
			{
//				Log.i(TAG, "the newName is null");
				return;
			}
			//否则在edittext里面字串后面加上.apk，构成整个文件名
			newName = newName + ".apk";
//			Log.i(TAG, "dialogRenameOnClick---------mLongClickFile = " + mLongClickFile.getName() + ", chageFileName = " + newName);
			String desFileName = mLongClickFile.getParent() + "/" + newName;
			File destFile = new File(desFileName);
			
			boolean result = mLongClickFile.renameTo(destFile);
			Log.i(TAG, "dialogRenameOnClick---------sourceFile = " + mLongClickFile.getName() 
					+ ", destFile = " + destFile.getName() + ", result = " + (result?"true":"false"));
			if(result)
			{
				mData.get(mLongClickSelectedFileIndex).setFile(destFile);
			}else
			{/*如果重命名失败，说明有非法字符*/
				Toast.makeText(mContext, mContext.getResources().getString(R.string.illegalStr), Toast.LENGTH_SHORT).show();				
			}
			iaa.setListData(mData);

		}
	}
	
    /**
     * 分享选中的apk文件的代码。
     * 特别注意setType这个。MIMETYPE值。
     * 还有需要传递Intent.EXTRA_STREAM这个值（也就是文件地址）
     * 否者处理这个请求的应用会提示"无效的地址"
     */
    private void shareAPK()
    {
    	Intent intent = new Intent(Intent.ACTION_SEND);
    	intent.setType("application/octet-stream");
    	Uri uri = Uri.fromFile(mLongClickFile);
    	intent.putExtra(Intent.EXTRA_STREAM, uri);
    	startActivity(Intent.createChooser(intent, mContext.getString(R.string.share_file)));    	
    }
	
	
	/**
	 * 删除当前选中的应用文件，并且更新listview
	 * 是从列表中删除还是同时删除磁盘上的文件。
	 * 要保证在一个数据源上面操作，防止发生混乱。
	 */
	private void delete()
	{
		iaa.removeApk(mLongClickSelectedFileIndex);
		mLongClickFile.delete();//删除选中的这个文件
		//如果在这里面更新主UI，会导致延迟。所以谨慎在子线程里面更新主线程UI。需要用
		//Message通知主界面更新UI
		//updateBtnText();
		Message msg = new Message();
		msg.what = UPDATE_BUTTON_TEXT;
		mHandler.sendMessage(msg);
		iaa.setListData(mData);
	}
	
	/**
	 * 针对弹出的ContextItem选择项进行响应。
	 * @param which 选择了哪个菜单
	 */
	private void dialogLongClickMenuOnClick(int which)
	{
		switch(which)
		{
		case OPERATION_DELETE:
			mSelectedDialog = DIALOG_YES_NO;
			showDialog(DIALOG_YES_NO);
			break;
		case OPERATION_RENAME:
			mSelectedDialog = DIALOG_RENAME;
			showDialog(DIALOG_RENAME);
			break;
		case OPERATION_SHARE:
			mSelectedDialog = DIALOG_SHARE;
			showDialog(DIALOG_SHARE);
			break;
		case OPERATION_DETAIL:
			mSelectedDialog = DIALOG_DETAIL;
			showDialog(DIALOG_DETAIL);
			break;
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_one_key, menu);
        return true;
    }

    //处理菜单事件
    public boolean onOptionsItemSelected(MenuItem item)
    {    	
    	int item_id=item.getItemId(); 
    	switch(item_id)
    	{
    		case R.id.about:
	
    		Intent ab_intent =new Intent();
    		ab_intent.setClass(OneKeyActivity.this,AboutActivity.class);
    		startActivity(ab_intent);    		
    		break;
    	}    	
		return true;    	
    }

    /**
     * 批量安装选中的apk
     */
    /**
     * 安装mData中指定下标的apk。
     * @param apkIndex
     */
	public void installApkBatch(int apkIndex) {

		/* 当选定要安装的时候才执行安装动作 */
		if ((mData.get(apkIndex).IsChecked())
				&& (mData.get(apkIndex).getPackage() != null)) {
			Log.i(TAG, "程序被选中，可以安装。apkIndex = " + apkIndex);
			String pkgName = mData.get(apkIndex).getPackage().packageName;
			String[] oldName = mPm
					.canonicalToCurrentPackageNames(new String[] { pkgName });
			if (oldName != null && oldName.length > 0 && oldName[0] != null) {
				pkgName = oldName[0];
				mData.get(apkIndex).getPackage().setPackageName(pkgName);
			}
			try {
				mData.get(apkIndex).setApplicationInfo(
						mPm.getApplicationInfo(pkgName,
								PackageManager.GET_UNINSTALLED_PACKAGES));
			} catch (NameNotFoundException e) {
				mData.get(apkIndex).setApplicationInfo(null);
			}
			ApkDetails mAd = mData.get(apkIndex);
			int iFlag = mAd.getInstallFlag();
			String installerPackagename = getIntent().getStringExtra(
					Intent.EXTRA_INSTALLER_PACKAGE_NAME);
			mPm.installPackage(mAd.getPackageURI(), observer, iFlag,
					installerPackagename);
		} else {
			// 如果本来就是这个apk文件只是有一个apk后缀，但是内部文件是错误的。那么依然需要判断。
			// 这个环节在扫描阶段已经判断了，所以不需要再进行处理，谨慎起见，依然做一个判断
			Log.i(TAG, "有一个apk没有被选中. i = " + apkIndex + ", PackageName = "
					+ mData.get(apkIndex).getPackageName());
			installindex++;
			installApkBatch(installindex);
		}
	}
    
    /**
     * 监控安装状况如何。查看PackageManager.INSTALL_SUCCEEDED，这个类里面有多达20种安装不成功的状况
     * 所以需要注意是否细分。
     * @author konka
     *
     */
    class PackageInstallObserver extends IPackageInstallObserver.Stub
    {
		@Override
		public void packageInstalled(String packageName, int returnCode)
				throws RemoteException {
			// TODO Auto-generated method stub
			//如果安装成功执行如下操作
			if(returnCode == PackageManager.INSTALL_SUCCEEDED)
			{
				Log.i(TAG, "install success =" + packageName + ", installindex = " + installindex
						+ ", apk name = " + mData.get(installindex).getFile().getName());
				//只能存包名了，所以考虑apk的扫描方式
				//由于需要加入针对安装失败文件的处理，所以采用将文件名保存的方式。
				//但是这个对于同名文件，但是不同地址的apk文件支持不够好。中间用&连接，这样还可以监督到安装失败的原因
				iaa.addSuccessList(mData.get(installindex).getFile().getName() + '&' + returnCode);
				//将安装成功的包放入iaa的安装成功列表中
				iaa.addSuccessList(mData.get(installindex));
//				Integer tmp = iaa.getInstallApks().get(mInstallComplete);
//				iaa.addSuccessList(iaa.getInstallApks().get(mInstallComplete));
			}else
			{
				//只能存包名了，所以考虑apk的扫描方式
				//由于需要加入针对安装失败文件的处理，所以采用将文件名保存的方式。
				//但是这个对于同名文件，但是不同地址的apk文件支持不够好。
				iaa.addFailList(mData.get(installindex).getFile().getName() + '&' + returnCode);//mData.get(installindex)
				//将安装失败应用放入安装失败列表中
				iaa.addFailList(mData.get(installindex));
				Log.i(TAG, "install failure =" + packageName + ", installindex = " + installindex
						+ ", returncode = " + returnCode + ", apk name = " + mData.get(installindex).getFile().getName());
			}
			//不管安装成功或者失败，应用的下标都应该新增
			installindex ++;
			//不管安装成功或者失败，都算是安装完成，这个标志位用来判断是否所有选中的应用都安装了。
			mInstallComplete ++;
			Log.i(TAG, "install returnCode =" + returnCode + "， mInstallComplete = " + mInstallComplete + ", checked counts = " + iaa.getCheckedCounts());
			if(mInstallComplete == iaa.getCheckedCounts())
			{//当所有的应用安装完毕后再显示安装结果
				Message msg = mHandler.obtainMessage(INSTALL_COMPLETE);
				mHandler.sendMessage(msg);
				//如果已经发送了安装完成信息，也就是说安装完成个数跟选中的应用个数一样，那么就
				//就判定整个过程完成了，不需要在递归了，所以return。
				return;
			}
			//如果程序没有遍历完应用列表，那么依然继续递归安装过程
			if(installindex < mData.size())
			{
				installApkBatch(installindex);
			}
		}    	
    }
    
    /**
     * 处理扫描完毕和安装完毕之后如何处理
     */
    private Handler mHandler = new Handler()
    {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch(msg.what)
			{
			case INSTALL_COMPLETE:
				Log.i(TAG, "安装完毕，关闭进度条框。");
				//安装完毕先关掉进度条框
				proDialog.cancel();
				//安装完毕之后就执行更新桌面和显示新窗口操作
//				Log.i(TAG, "into refreshDesk");
				//当全选所有应用的时候才刷新桌面，因为这个一般是工厂安装才会用到。
				if(iaa.getCheckedCounts() == iaa.getAllApkCounts())
				{
					//刷新桌面，让一些程序图标显示在正确的位置。
					refreshDesk();
				}
				showResultScreen();
				break;
			case SCAN_COMPLETE:
				//该步骤非常重要，初始化所有扫描到的apk值
//		        apks = sa.getApksFileList();//基本单元是File对象
		        mData = sa.getScanApksList();//sa.getApksNameList();//基本单元是ApkDetails对象
		        apksCounts = mData.size();
		        iaa = new InstallAppAdapter(getLayoutInflater(), mContext, mData);
		        lv_installApps.setAdapter(iaa);
		        lv_installApps.invalidate();
//		        dismissDialog(CHANNELSEARCH);
		        removeDialog(CHANNELSEARCH);
				break;
			case UPDATE_BUTTON_TEXT:
				updateBtnText();
				break;
			
			default:
				break;
			}
		}    	
    };
    
    protected Dialog onCreateDialog(int id)
    {
    	switch(id)
    	{
    	case CHANNELSEARCH:
    		return searchDialog(this, this.getResources().getString(R.string.scanning));
    	case INSTALLINGDIALOG:
    		return searchDialog(this, this.getResources().getString(R.string.installing));
    	case DIALOG_OPERATION_MENU:
//    		Log.i(TAG, "onCreateDialog-----DIALOG_OPERATION_MENU");
			return new AlertDialog.Builder(this).setTitle(mContext.getString(R.string.manipulate))
    				.setIcon(R.drawable.ic_launcher)
    				.setItems(R.array.operation_menu, ocl).create();
			//删除文件
    	case DIALOG_YES_NO:
    		return new AlertDialog.Builder(this).setTitle(mContext.getString(R.string.warning))
    				.setMessage(mContext.getString(R.string.delete_confirm)).setPositiveButton(mContext.getString(R.string.sure), ocl)
    				.setNegativeButton(mContext.getString(R.string.cancel), null).create();
    		//重命名文件
    	case DIALOG_RENAME:
//    		Log.i(TAG, "onCreateDialog-----DIALOG_RENAME");
    		mRename = new EditText(this);
    		mRename.setText(getFileName(mLongClickFile.getName()));
    		return new AlertDialog.Builder(this).setTitle(mContext.getString(R.string.rename))
    				.setView(mRename).setPositiveButton(mContext.getString(R.string.sure), ocl)
    				.setNegativeButton(mContext.getString(R.string.cancel), ocl).create();
    		//分享文件
    	case DIALOG_SHARE:
    		shareAPK();
    		break;
    		//显示详情
    	case DIALOG_DETAIL:
//    		Log.i(TAG, "onCreateDialog-----DIALOG_DETAIL");
    		return showFileDetail();
    	}
    	return null;
    }
    
    /**
     * 
     * @param fileName 比如：微博.apk
     * @return 返回文件名称比如：微博
     */
    private String getFileName(String fileName)
    {
    	if(fileName != null && fileName.endsWith(".apk"))
    	{
    		int ends = fileName.lastIndexOf(".apk");
    		String file_name = fileName.substring(0, ends);
    		Log.i(TAG, "the file name = " + file_name);
    		return file_name;
    	}
    	return null;
    }
    
    @Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		// TODO Auto-generated method stub
    	switch(id)
    	{
    	case DIALOG_OPERATION_MENU:
    	case DIALOG_YES_NO:
    	case DIALOG_RENAME:
    	case DIALOG_SHARE:
    	case DIALOG_DETAIL:
    		removeDialog(id);
    		break;
    	}
    	super.onPrepareDialog(id, dialog);
	}

	private Dialog searchDialog(Context context, String showStr)
    {
    	dialog = new ProgressDialog(context);
//    	CharSequence msg = getResources().getText(R.string.scanning);
    	dialog.setMessage(showStr);
    	dialog.setCanceledOnTouchOutside(false);
//    	ProgressDialog progressdialog = (ProgressDialog)dialog
    	dialog.setOnKeyListener(new OnKeyListener() {			
			@Override
			public boolean onKey(DialogInterface arg0, int arg1, KeyEvent arg2) {
				// TODO Auto-generated method stub
				switch(arg2.getKeyCode())
				{
				case KeyEvent.KEYCODE_BACK:
					if(dialog != null && dialog.isShowing())
						dialog.dismiss();				
					finish();
					break;
				}
				return false;
			}
		});
    	return dialog;
    }
    
    /**
     * 显示选中文件的详细信息
     * @return 返回显示详细信息的字串
     */
    private AlertDialog showFileDetail()
    {
    	AlertDialog detailDialog = null;
    	String name, size, modifiedTime, path, permission;
    	StringBuilder string_builder = new StringBuilder();
//    	Log.i(TAG, "the file detail is = " + mLongClickFile.toString());
		//打印当前mData里面所有的数据。
//		for(int i = 0; i < mData.size(); i ++)
//		{
//			String print = mData.get(i).getFile().getName();
//			Log.i(TAG, "dialogRenameOnClick---------mData[" + i +"] = " + print);
//		}    	
//    	Log.i(TAG, "the detail file is 2= " + mData.get(mLongClickSelectedFileIndex).getFile().toString());
    	
    	//文件名称
    	string_builder.setLength(0);
    	name = string_builder.append(getString(R.string.name))
    			.append(": ").append(mLongClickFile.getName())
    			.append("\n")
    			.toString();
    	
    	//文件大小
    	string_builder.setLength(0);
    	size = string_builder.append(getString(R.string.size))
    			.append(": ").append(transSizeToString(mLongClickFile.length()))
    			.append("\n").toString();
    	
    	//设定修改时间
    	long time = mLongClickFile.lastModified();
    	string_builder.setLength(0);
    	modifiedTime = string_builder.append(getString(R.string.modified_time))
    			.append(": ").append(DateFormat.getDateInstance().format(new Date(time)))
    			.append("\n").toString();
    	
    	//设定路径
    	string_builder.setLength(0);
    	path = string_builder.append(getString(R.string.path)).append(": ")
    		.append(mLongClickFile.getAbsolutePath()).append("\n").toString();

    	//设定权限
    	string_builder.setLength(0);
    	string_builder.append(getString(R.string.readable)).append(": ")
    		.append(mLongClickFile.canRead()?getString(R.string.yes):getString(R.string.no))
    		.append("\n");
    	string_builder.append(getString(R.string.writable)).append(": ")
    		.append(mLongClickFile.canWrite()?getString(R.string.yes):getString(R.string.no))
    		.append("\n");
    	string_builder.append(getString(R.string.executable)).append(": ")
    		.append(mLongClickFile.canExecute()?getString(R.string.yes):getString(R.string.no));
    	permission = string_builder.toString();
    	
    	string_builder.setLength(0);
    	String detail = string_builder.append(name).append(size).append(modifiedTime)
    			.append(path).append(permission).toString();
    	//合成名称、大小、修改日期、路径、可读、可写、可执行这些内容
    	detailDialog = new AlertDialog.Builder(this).setTitle(mContext.getString(R.string.details))
    			.setMessage(detail).setPositiveButton(mContext.getString(R.string.sure), null).create();
    	return detailDialog;
    }
    /**
     * 将文件大小转变成B、KB、MB、GB等
     * @param size
     * @return
     */
    private String transSizeToString(long size)
    {
    	final String UNIT_B = "B";
    	final String UNIT_KB = "KB";
    	final String UNIT_MB = "MB";
    	final String UNIT_GB = "GB";
    	final String UNIT_TB = "TB";
    	final int UNIT_INTERVAL = 1024;
    	final double ROUNDING_OFF = 0.005;
    	final int DECIMAL_NUMBER = 100;
    	
    	String unit = UNIT_B;
    	if(size < DECIMAL_NUMBER)
    	{
    		return Long.toString(size) + " " + unit;
    	}
    	
    	unit = UNIT_KB;
    	double sizeDouble = (double) size / (double) UNIT_INTERVAL;
    	if(sizeDouble > UNIT_INTERVAL)
    	{
    		sizeDouble = (double) sizeDouble / (double) UNIT_INTERVAL;
    		unit = UNIT_MB;
    	}
    	if(sizeDouble > UNIT_INTERVAL)
    	{
    		sizeDouble = (double) sizeDouble / (double) UNIT_INTERVAL;
    		unit = UNIT_GB;
    	}
    	if(sizeDouble > UNIT_INTERVAL)
    	{
    		sizeDouble = (double) sizeDouble / (double) UNIT_INTERVAL;
    		unit = UNIT_TB;
    	}
    	
    	long sizeInt = (long)((sizeDouble + ROUNDING_OFF) * DECIMAL_NUMBER);
    	double formatedSize = ((double) sizeInt) / DECIMAL_NUMBER;
//    	Log.d(TAG, "transSizeToString: " + formatedSize + unit);
    	
    	if(formatedSize == 0)
    	{
    		return "0" + " " + unit;
    	}else{
    		return Double.toString(formatedSize) + " " + unit;
    	}
    }
    /**
     * 重写onDestroy，因为发现在单独程序安装过程中会产生has leaked window错误
     * 参考文献：http://www.cnblogs.com/royenhome/archive/2011/05/20/2051879.html
     */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
//		removeDialog(CHANNELSEARCH);
		if(proDialog != null && proDialog.isShowing())
		{
			proDialog.dismiss();
		}
		super.onDestroy();
	}
	
	/**
	 * 初始化需要重启的Launcher的列表。但是在4.2中似乎没有起作用了。暂时保留
	 */
	private void initTargetLauncher()
	{
		mTargetLauncher = new ArrayList<String>();
		//问果王桌面
		mTargetLauncher.add("com.cooee.Mylauncher");
		//系统桌面，在E900里面，桌面对应的包名是com.android.launcher而不是launcher2
		mTargetLauncher.add("com.android.launcher");
//		mTargetLauncher.add("com.guobi.winguo.hybrid");
	}
	
	/**
	 * 发送命令更新桌面。
	 */
	private void refreshDesk()
	{
		Log.i(TAG, "refreshDesk()----->");
		initTargetLauncher();
		ActivityManager am = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
		
		try {
			Log.i(TAG, "refreshDesk()----->clear user data");
			Method clearUserdata = am.getClass()
					.getDeclaredMethod("clearApplicationUserData", 
							String.class, IPackageDataObserver.class);
			clearUserdata.setAccessible(true);
			clearUserdata.invoke(am, mTargetLauncher.get(1), new PackageDataClearObserver());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	class PackageDataClearObserver implements IPackageDataObserver{

		@Override
		public IBinder asBinder() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void onRemoveCompleted(String arg0, boolean arg1)
				throws RemoteException {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	
	/**
	 * 重构监听按键函数。点击两次back键才退出，防止用户误触back键。
	 */
/*	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			long secondCancelTime = System.currentTimeMillis();
			//如果两次点击间隔小于1000毫秒就退出。
			if(secondCancelTime - mFirstCancelTime > 1000)
			{
				Toast.makeText(mContext, 
						mContext.getString(R.string.more_back_exit), 
						Toast.LENGTH_SHORT).show();
				mFirstCancelTime = secondCancelTime;
				return true;
		
			}else
			{
				System.exit(0);
			}
		}
		return super.onKeyUp(keyCode, event);
	}
	*/

    
}




























