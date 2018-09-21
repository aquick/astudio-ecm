package org.gmplib.test.factor.ecm;

import java.io.*;

public class RandomNumberFile {
    private InputStream fin;
    private int base;
    private String fname;
    private long[] powers;
    private int maxpower;
    private long consumed;

    public RandomNumberFile(String fname, int base)
	    throws IOException
    {
	if (base <= 1) {
	    throw new IllegalArgumentException("base");
	}
	if (fname == null) {
	    throw new IllegalArgumentException("fname");
	}
	this.fname = fname;
	this.base = base;
	this.fin = new BufferedInputStream(new FileInputStream(fname));
	this.consumed = 0;
	this.powers = new long[33];
	this.powers[0] = 1;
	long lv = 1;
	int m = 0;
	for (;;) {
	    this.powers[m] = lv;
	    if (lv >= 0x100000000L) {
		this.maxpower = m;
		break;
	    }
	    lv = base*lv;
	    m++;
	}
    }

    public void close() throws IOException
    {
	if (this.fin != null) {
	    this.fin.close();
	    this.fin = null;
	}
    }

    protected void finalize()
    {
	try {
	    close();
	}
	catch(IOException e) {
	}
    }

    public long consumed()
    {
	return this.consumed;
    }

    public long skip(long n) throws IOException
    {
	long i = 0;
	while (i < n) {
	    if (nextRng() == -1) break;
	    i++;
	}
	return i;
    }

    public void reset() throws IOException
    {
	if (this.base != 0 && this.fname != null) {
	    close();
	    this.fin = new BufferedInputStream(new FileInputStream(this.fname));
	    this.consumed = 0;
	}
    }

    public int nextRng() throws IOException
    {
	boolean found = false;
	boolean eof = false;
	int c = -1;
	int retval = -1;

	while (!found && !eof) {
	    c = this.fin.read();
	    if (c == -1) {
		eof = true;
		break;
	    }
	    if (c > 0 &&
		    c != (int)' ' && c != (int)'\t' &&
		    c != (int)'\r' && c != (int)'\n') {
		found = true;
		break;
	    }
	}
	if (eof) {
	    throw new EOFException("EOF");
	}
	if (found) {
	    this.consumed++;
	    if ((int)'0' <= c && c <= (int)'9') {
		retval = (c - (int)'0');
	    } else if ((int)'A' <= c && c <= (int)'Z') {
		retval = 10 + (c - (int)'A');
	    } else if ((int)'a' <= c && c <= (int)'z') {
		retval = 10 + (c - (int)'a');
	    }
	    if (retval >= base) {
		retval = -1;
	    }
	}
	if (retval < 0) {
	    throw new IOException("bad data format");
	}
	return retval;
    }

    public int nextInt(int ubound) throws IOException
    {
	boolean fast = false;
	int rval = -1;
	long lv;
	int m;
	int i;
	int k;

	if (ubound <= 1) {
	    return -1;
	}
	m = 1;
	while (m <= this.maxpower) {
	    if (this.powers[m] == ubound) {
		fast = true;
		break;
	    }
	    if (this.powers[m] > ubound) break;
	    m++;
	}
	if (fast) {
	    rval = 0;
	    for (i = 0; i < m; i++) {
		k = nextRng();
		rval = this.base*rval + k;
	    }
	    return rval;
	}
	m = this.maxpower;
	lv = 0;
	for (i = 0; i < m; i++) {
	    k = nextRng();
	    lv = this.base*lv + k;
	}
	rval = (int)((double)lv*(double)ubound/(double)this.powers[m]);
	return rval;
    }

    /**
     * For maxpower a power of base.
     */
    public int nextInt() throws IOException
    {
	int rval = -1;
	long lv;
	int m;
	int i;
	int k;

	m = this.maxpower;
	lv = 0;
	for (i = 0; i < m; i++) {
	    k = nextRng();
	    lv = this.base*lv + k;
	}
	rval = (int)lv;
	return rval;
    }
}
