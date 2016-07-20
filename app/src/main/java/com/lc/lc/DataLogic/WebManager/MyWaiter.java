package com.lc.lc.DataLogic.WebManager;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.CUPCAKE)
public class MyWaiter extends AsyncTask<Object, Object, Object> {
    private int mTaskId;
    private boolean mWorking;

    public int GetTaskId() {
        return mTaskId;
    }

    public boolean isWorking() {
        return mWorking;
    }

    public static abstract class WaiterDelegate {
        public boolean OnWaiterStart(MyWaiter waiter) {
            return true;
        }

        public void OnWaiterCancel(MyWaiter waiter, Object resultObj) {
        }

        public void OnWaiterStop(MyWaiter waiter, Object resultObj) {
        }

        public Object OnWaiterWork(MyWaiter waiter, Object... parms) {
            return null;
        }

        public void OnWaiterUpdate(MyWaiter waiter, Object... params) {
        }
    }

    private WaiterDelegate mDelegate;
    private Context mContext;
    private ProgressDialog mDlg = null;
    private String mStrTitle;
    private String mStrMsg;
    public boolean mNoDlg = false;
    public int m_tag;
    public MyWaiter(Context ctx, WaiterDelegate del, String strMsg) {
        this(ctx, del, strMsg, DEFAULT_TASKID);
    }

    public MyWaiter(Context ctx, WaiterDelegate del) {
        this(ctx, del, DEFAULT_TASKID);
    }

    public MyWaiter(Context ctx, WaiterDelegate del, int id) {
        this(ctx, del, null, null, id);
        mNoDlg = true;
    }

    public MyWaiter(Context ctx, WaiterDelegate del, String strMsg, int id) {
        this(ctx, del, null, strMsg, id);
    }

    public MyWaiter(Context ctx, WaiterDelegate del, String strTitle, String strMsg) {
        this(ctx, del, strTitle, strMsg, DEFAULT_TASKID);
    }

    public MyWaiter(Context ctx, WaiterDelegate del, String strTitle, String strMsg, int id) {
        mContext = ctx;
        mDelegate = del;

        mStrTitle = strTitle;
        mStrMsg = strMsg;
        mTaskId = id;
        mWorking = false;
    }

    @Override
    protected void onPreExecute() {
        if (mDelegate == null || !mDelegate.OnWaiterStart(this)) {
            cancel(false);
        } else {
            if (!mNoDlg)
                mDlg = ProgressDialog.show(mContext, mStrTitle, mStrMsg);
            super.onPreExecute();
            mWorking = true;
        }
    }

    @Override
    protected void onPostExecute(Object resultObj) {
        super.onPostExecute(resultObj);
        if (mDlg != null) {
            mDlg.dismiss();
            mDlg = null;
        }
        mWorking = false;
        mDelegate.OnWaiterStop(this, resultObj);
        mDelegate = null;
    }

    @Override
    protected Object doInBackground(Object... param) {
        return mDelegate.OnWaiterWork(this, param);
    }

    public void UpdateWaiter(Object... values) {
        publishProgress(values);
    }

    public void SetTitleAndMessage(String title, String msg) {
        if (mDlg != null) {
            mDlg.setTitle(title);
            mDlg.setMessage(msg);
        }
    }

    @Override
    protected void onProgressUpdate(Object... values) {
        super.onProgressUpdate(values);
        mDelegate.OnWaiterUpdate(this, values);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCancelled(Object o) {
        mDelegate.OnWaiterCancel(this, o);
        mWorking = false;
        super.onCancelled(o);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mWorking = false;
        mDelegate.OnWaiterCancel(this, null);
    }

    public static final int DEFAULT_TASKID = 0;
}
