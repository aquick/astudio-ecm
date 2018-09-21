package org.gmplib.test.factor.ecm;

import android.os.AsyncTask;
import android.util.Log;

import org.gmplib.gmpjni.GMP;
import org.gmplib.gmpjni.GMP.mpz_t;
import org.gmplib.gmpjni.GMP.GMPException;
import org.gmplib.gmpjni.ECM;
import org.gmplib.gmpjni.ECM.ECMException;
import org.gmplib.gmpjni.ECM.ecm_params;
import org.gmplib.gmpjni.ECM.ecm_stop_callback;

/**
 * AsyncTask for using native library ECM.
 */
public class ECM_Task extends AsyncTask<String, Integer, Integer> {

    private static final String TAG = "ECM_Task";
    private StringBuffer result;
    private StringBuffer[] progressInfo;
    private UI uinterface;
    private String failmsg;
    private String nstr;
    private RandomNumberFile rng;
    private double B1;
    private long elapsedTime;
    private boolean done;

    /**
     * Instance of ecm_stop_callback for stopping ECM.factor native method.
     */
    private class my_stop_callback implements ecm_stop_callback
    {
        public boolean stop()
	{
	    return ECM_Task.this.uinterface.isStop();
	}
    }

    public ECM_Task(UI ui, RandomNumberFile rng)
    {
	this.uinterface = ui;
	this.rng = rng;
	this.result = new StringBuffer();
	this.failmsg = null;
	this.nstr = null;
	this.B1 = 0.0;
	this.elapsedTime = 0;
	this.done = false;
    }

    public synchronized boolean isDone()
    {
        return this.done;
    }

    /**
     * Call native library method ECM.factor with a specified number of curves.
     */
    @Override
    protected Integer doInBackground(String... params)
    {
	int rc = -1;
	long st;
	ecm_params p = null;
	if (params.length < 4) {
	    return Integer.valueOf(-1);
	}
	st = System.currentTimeMillis();
	try {
	    mpz_t n = new mpz_t();
	    mpz_t f = new mpz_t();
	    mpz_t zero = new mpz_t();
	    long seed;
	    int i;
	    int iterations = 100;
	    int method = ecm_params.ECM;

	    GMP.mpz_set_ui(zero, 0);
	    GMP.mpz_set_str(n, params[0], 10);
	    this.nstr = GMP.mpz_get_str(n,10);
	    this.B1 = Double.parseDouble(params[1]);
	    iterations = Integer.parseInt(params[2]);
	    p = new ecm_params();
	    seed = this.rng.nextInt();
	    if (seed < 0) {
		seed = 0x100000000L + seed;
	    }
	    String s = "seed=" + seed;
	    Log.d(TAG, s);
	    if (params[3].equals("P-1")) {
	        Log.d(TAG, "method is P-1");
		method = ecm_params.PM1;
	    } else if (params[3].equals("P+1")) {
		Log.d(TAG, "method is P+1");
		method = ecm_params.PP1;
	    } else {
		Log.d(TAG, "method is ECM");
	    }
	    p.set_method(method);
	    p.set_verbose(2);
	    p.set_rng_seed(seed);
	    p.set_err_filename(params[4]);
	    p.set_out_filename(params[5]);
	    p.set_stop_callback(this.new my_stop_callback());

	    onProgressUpdate(0);
	    for (i = 0; i < iterations; i++) {
		onProgressUpdate(i + 1);
		rc = ECM.factor(f, n, B1, p);
		if (rc < 0) {
		    break;
		}
		if (rc > 0) {
		    this.result.append(GMP.mpz_get_str(f, 10));
		    break;
		}
		p.set_x(zero);
		//p.set_y(zero);
		p.set_sigma(zero);
	    }
	}
	catch (GMPException e) {
	    Log.d(TAG, e.toString());
	    this.failmsg = "GMPException [" + e.getCode() + "] " + e.getMessage();
	    rc = -1;
	}
	catch (ECMException e) {
	    Log.d(TAG, e.toString());
	    this.failmsg = "ECMException [" + e.getCode() + "] " + e.getMessage();
	    rc = -1;
	}
	catch (Exception e) {
	    Log.d(TAG, e.toString());
	    this.failmsg = e.getMessage();
	    rc = -1;
	}
	finally {
	    if (p != null) {
	        try {
		    p.set_stop_callback(null);
		    p.clear();
		}
		catch (ECMException ee) {
	            Log.d(TAG, ee.toString());
		}
	    }
	}
	synchronized (this) {
	    this.done = true;
	}
	this.elapsedTime = System.currentTimeMillis() - st;
	return Integer.valueOf(rc);
    }

    @Override
    protected void onPostExecute(Integer result)
    {
	uinterface.display("===========");
	if (result < 0) {
	    if (this.failmsg != null) {
		uinterface.display("ERROR: " + this.failmsg);
	    } else {
		uinterface.display("UNKNOWN ERROR");
	    }
	} else if (result == 0) {
	    uinterface.display("no factor found");
	    Log.d(TAG, "no factor found");
	} else if (result == 1) {
	    uinterface.display("factor found at stage 1");
	    uinterface.display("factor is " + this.result.toString());
	    Log.d(TAG, "factor found at stage 1");
	    Log.d(TAG,"factor is " + this.result.toString());
	} else if (result == 2) {
	    uinterface.display("factor found at stage 2");
	    uinterface.display("factor is " + this.result.toString());
	    Log.d(TAG, "factor found at stage 2");
	    Log.d(TAG, "factor is " + this.result.toString());
	}
	uinterface.display("random digits consumed so far: " + this.rng.consumed());
	uinterface.display("elapsed time: " + this.elapsedTime + " milliseconds");
	uinterface.display("===========");
    }

    @Override
    protected void onPreExecute()
    {
	uinterface.display(TAG);
	uinterface.display("ECM version is " + ECM.getVersion());
    }

    @Override
    protected void onProgressUpdate(Integer... progress)
    {
	int i = progress[0];
	if (i == 0 && this.nstr != null) {
	    uinterface.display("n=" + this.nstr);
	} else {
	    uinterface.display("==> " + i);
	}
    }
}
