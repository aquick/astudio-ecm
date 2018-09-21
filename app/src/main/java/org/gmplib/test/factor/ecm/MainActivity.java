package org.gmplib.test.factor.ecm;

import android.app.Activity;
//import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.StringTokenizer;

import org.gmplib.gmpjni.GMP;
import org.gmplib.gmpjni.ECM;
import org.gmplib.gmpjni.ECM.ECMException;

public class MainActivity extends Activity implements UI {

    private PowerManager.WakeLock mWakeLock;
    private Spinner mSpinner;
    ArrayAdapter<CharSequence> mAdapter;
    private String smethod = "ECM";
    private TextView mView;
    private TextView mNumber;
    private TextView mBound;
    private TextView mCurves;
    private Button mStart;
    private Button mCancel;
    private RandomNumberFile rng;
    private String randfname = "2010-03-02.hex.txt";
    private int base = 16;
    AsyncTask<String, Integer, Integer> task = null;
    private MyHandler mHandler;
    private boolean stop = false;
    private static final String TAG = "MainActivity";

    private class MyHandler extends Handler {

	public static final int DISPLAY_INFO = 0;

	public MyHandler(Looper looper)
	{
	    super(looper);
	}

	@Override
	public void handleMessage(Message inputMessage)
	{
	    int code = inputMessage.what;
	    switch (code) {
		case DISPLAY_INFO:
		    StringBuffer sb = new StringBuffer();
		    String msg = (String) inputMessage.obj;
		    sb.append(msg);
		    sb.append("\n");
		    MainActivity.this.mView.append(sb.toString());
		    break;
		default:
		    break;
	    }
	}
    }

    private class MonitorThread extends Thread {

	public void run()
	{
	    BufferedReader br = null;
	    BufferedReader br2 = null;
	    String s;
	    String root = MainActivity.this.getExternalFilesDir(null).getPath();

	    Log.d("MonitorThread", "starting");
	    try {
		for (;;) {
		    Thread.sleep(2000);
		    /***
		    if (br == null) {
		        try {
			    br = new BufferedReader(new FileReader(root + "/out.txt"));
			}
			catch (IOException ee) {
		            Log.d("MonitorThread", "EXCEPTION: " + ee.toString());
			}
		    }
		    if (br != null) {
		        for (;;) {
			    s = br.readLine();
			    if (s == null) break;
			    MainActivity.this.display(s);
			}
		    }***/
		    if (MainActivity.this.task != null) {
			if (((ECM_Task)MainActivity.this.task).isDone()) {
			    break;
			}
		    }
		}
		MainActivity.this.display("---- err begin ----");
		br2 = new BufferedReader(new FileReader(root + "/err.txt"));
		for (;;) {
		    s = br2.readLine();
		    if (s == null) break;
		    MainActivity.this.display(s);
		}
		MainActivity.this.display("---- err end ----");
	    }
	    catch (Exception e) {
		Log.d("MonitorThread", "EXCEPTION: " + e.toString());
	    }
	    finally {
	        if (br != null) {
	            try {
	                br.close();
		    }
		    catch (IOException ee) {
			Log.d("MonitorThread", "EXCEPTION: " + ee.toString());
		    }
		}
		if (br2 != null) {
		    try {
			br2.close();
		    }
		    catch (IOException ee) {
			Log.d("MonitorThread", "EXCEPTION: " + ee.toString());
		    }
		}
		try {
		    MainActivity.this.cleanup();
		}
		catch (IOException ee) {
		    Log.d("MonitorThread", "EXCEPTION: " + ee.toString());
		}
	    }
	    Log.d("MonitorThread", "terminating");
	}
    }

    protected void startMonitorThread()
    {
	(this.new MonitorThread()).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_main);
	mSpinner = (Spinner) findViewById(R.id.Spinner01);
	mAdapter = ArrayAdapter.createFromResource(this,
		R.array.method_array, android.R.layout.simple_spinner_item);
	mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	mSpinner.setAdapter(mAdapter);
	mView = (TextView) findViewById(R.id.TextView01);
	mNumber = (TextView) findViewById(R.id.TextView02);
	mBound = (TextView) findViewById(R.id.TextView03);
	mCurves = (TextView) findViewById(R.id.TextView04);
	mStart = (Button) findViewById(R.id.Button01);
	mStart.setOnClickListener(
		new View.OnClickListener() {
		    public void onClick(View v) {
			String snum;
			String sbound;
			String scurves;
			String root = MainActivity.this.getExternalFilesDir(null).getPath();

			MainActivity.this.init();
			MainActivity.this.mView.setText("");
			StringBuffer sb = new StringBuffer();
			sb.append(MainActivity.this.mNumber.getText());
			snum = sb.toString().replaceAll("\\s", "");
			sbound = MainActivity.this.mBound.getText().toString();
			scurves = MainActivity.this.mCurves.getText().toString();
			task = new ECM_Task(MainActivity.this, MainActivity.this.rng);
			task.execute(snum, sbound, scurves, MainActivity.this.smethod, root + "/err.txt", root + "/out.txt");
			MainActivity.this.startMonitorThread();
		    }
		});
	mCancel = (Button) findViewById(R.id.Button02);
	mCancel.setOnClickListener(
		new View.OnClickListener() {
		    public void onClick(View v)
		    {
		        synchronized (MainActivity.this) {
		            MainActivity.this.stop = true;
			}
		    }
		});
	mSpinner.setOnItemSelectedListener(
		new AdapterView.OnItemSelectedListener()
		{
		    @Override
		    public void onItemSelected(AdapterView<?> parent,
					       View view,
					       int position,
					       long id)
		    {
			MainActivity.this.smethod = MainActivity.this.mAdapter.getItem(position).toString();
		    }
		    @Override
		    public void onNothingSelected(AdapterView<?> arg0)
		    {
		    }
		});
	mHandler = new MyHandler(Looper.getMainLooper());
	try {
	    GMP.init();
	    ECM.init();
	    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
	} catch (Exception e) {
	    Log.d(TAG, "EXCEPTION: " + e.toString());
	    display("EXCEPTION: " + e.toString());
	    StackTraceElement[] st = e.getStackTrace();
	    for (int m = 0; m < st.length; m++) {
		display(st[m].toString());
	    }
	}
    }

    protected void init()
    {
	String root = MainActivity.this.getExternalFilesDir(null).getPath();
	try {
	    (new File(root + "/out.txt")).delete();
	    (new File(root + "/err.txt")).delete();
	    initRandom();
	    if (mWakeLock != null && !mWakeLock.isHeld()) {
		mWakeLock.acquire();
	    }
	} catch (Exception e) {
	    Log.d(TAG, "EXCEPTION: " + e.toString());
	    display("EXCEPTION: " + e.toString());
	}
    }

    @Override
    protected void onDestroy()
    {
	Log.d(TAG, "onDestroy");
	/***
	try {
	    finiRandom();
	} catch (IOException e) {
	    Log.d(TAG, "EXCEPTION: " + e.toString());
	}***/
	mWakeLock = null;
	super.onDestroy();
    }

    @Override
    protected void onPause()
    {
	Log.d(TAG, "onPause");
	super.onPause();
    }

    @Override
    protected void onResume()
    {
	super.onResume();
	Log.d(TAG, "onResume");
    }

    protected void cleanup() throws IOException
    {
	if (mWakeLock != null && mWakeLock.isHeld()) {
	    mWakeLock.release();
	}
	finiRandom();
    }

    protected void initRandom() throws IOException
    {
	int n = 0;
	String root = this.getExternalFilesDir(null).getPath();
	String fname = root + "/.randseed2";
	BufferedReader fin = new BufferedReader(new FileReader(fname));
	String line = fin.readLine();
	fin.close();
	if (line.length() > 0) {
	    StringTokenizer st = new StringTokenizer(line);
	    if (st.hasMoreTokens()) {
		randfname = st.nextToken();
		if (st.hasMoreTokens()) {
		    base = Integer.parseInt(st.nextToken());
		    if (st.hasMoreTokens()) {
			n = Integer.parseInt(st.nextToken());
		    }
		}
	    }
	}
	rng = new RandomNumberFile(root + "/" + randfname, base);
	rng.skip(n);
    }

    protected void finiRandom() throws IOException
    {
	if (rng != null) {
	    long consumed = rng.consumed();
	    rng.close();
	    String root = this.getExternalFilesDir(null).getPath();
	    String fname = root + "/.randseed2";
	    BufferedWriter fout = new BufferedWriter(new FileWriter(fname));
	    fout.write(randfname + " " + base + " " + consumed);
	    fout.close();
	    rng = null;
	}
    }

    public void display(String line)
    {
	Message msg = mHandler.obtainMessage(
		MyHandler.DISPLAY_INFO,
		line);
	mHandler.sendMessage(msg);
    }

    public boolean isStop()
    {
        boolean ret = false;
        synchronized (this) {
	    ret = this.stop;
	}
	return ret;
    }
}
