package com.example.afhq.service;

import java.lang.reflect.Method;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.example.afhq.db.dao.BlackNumberDao;
import com.example.afhq.receiver.InnerSmsReceiver;
import com.example.android.ITelephony;

/**
 * �ֶ������������ط���
 * @author �Ľ�
 *
 */
public class CallSmsSafeService extends Service {
	private InnerSmsReceiver receiver;
	private BlackNumberDao dao;
	//ϵͳ�ṩ�ĵ绰���������绰�����ķ���
	private TelephonyManager tm;
	private MyPhoneListener listener;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		dao = new BlackNumberDao(this);
		tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		listener = new MyPhoneListener();
		tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
		receiver  = new InnerSmsReceiver();
		/**
		 * ���ö����������ȼ�
		 */
		IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
		filter.setPriority(Integer.MAX_VALUE);
		registerReceiver(receiver, filter);
		super.onCreate();
	}
	
	private class MyPhoneListener extends PhoneStateListener{
		@Override
		public void onCallStateChanged(int state, final String incomingNumber) {
			super.onCallStateChanged(state, incomingNumber);
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE://����״̬
				
				break;
			case TelephonyManager.CALL_STATE_RINGING://����״̬
				String mode = dao.findBlockMode(incomingNumber);
				if("1".equals(mode)||"3".equals(mode)){
					Log.i("MyPhoneListener","�Ҷϵ绰");
					//�۲죨����һ��Ӧ�ó������ݿ�ı仯�����м�¼�ı仯��������м�¼�����ˣ��ͰѺ��м�¼��ɾ������
					Uri uri = Uri.parse("content://call_log/calls");
					getContentResolver().registerContentObserver(uri, true, new CallLogObserver(new Handler(), incomingNumber));
					//�ô���Ҷϵ绰��
					endCall();//�绰�Ҷ�֮�󣬻�������һ��Ӧ�ó����������ɺ��м�¼��
					//�����������������ĺ��м�¼
					
				}
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK://��ͨ״̬
				System.out.println("��ͨ״̬");
				break;
			}
		}
	}
	
	
	private class CallLogObserver extends ContentObserver{
		private String incomingNumber;
		public CallLogObserver(Handler handler,String incomingNumber) {
			super(handler);
			this.incomingNumber = incomingNumber;
		}
		//�۲쵽���ݿ����ݱ仯���õķ���
		@Override
		public void onChange(boolean selfChange) {
			Log.i("CallLogObserver","���м�¼���ݿ�����ݱ仯�ˡ�");
			getContentResolver().unregisterContentObserver(this);
			deleteCallLog(incomingNumber);
			super.onChange(selfChange);
		}
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(receiver);
		receiver = null;
		tm.listen(listener, PhoneStateListener.LISTEN_NONE);
		listener = null;
		super.onDestroy();
	}
	
	/**
	 * ������м�¼
	 * @param incomingNumber
	 */
	public void deleteCallLog(String incomingNumber) {
		ContentResolver resolver = getContentResolver();
		Uri uri = Uri.parse("content://call_log/calls");
		resolver.delete(uri, "number=?", new String[]{incomingNumber});
	}

	/**
	 * �Ҷϵ绰
	 */
	public void endCall() {
		try {
			Class clazz = getClassLoader().loadClass("android.os.ServiceManager");
			Method method = clazz.getDeclaredMethod("getService", String.class);
			IBinder iBinder = (IBinder) method.invoke(null, TELEPHONY_SERVICE);
			ITelephony itelephony = ITelephony.Stub.asInterface(iBinder);
			itelephony.endCall();
			//��ͨ����ת�� 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}