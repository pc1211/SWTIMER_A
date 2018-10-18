package com.example.pgyl.swtimer_a;

import android.os.Handler;
import android.view.View;
import android.widget.ListView;

public class MainCtListRobot {
    public interface onExpiredTimersListener {
        void onExpiredTimers();
    }

    public void setOnExpiredTimersListener(onExpiredTimersListener listener) {
        mOnExpiredTimersListener = listener;
    }

    private onExpiredTimersListener mOnExpiredTimersListener;

    //region Variables
    private MainCtListItemAdapter mainCtListItemAdapter;
    private ListView mainCtListView;
    private CtRecordsHandler ctRecordsHandler;
    private long updateInterval;
    private final Handler handlerTime = new Handler();
    private Runnable runnableTime = new Runnable() {
        @Override
        public void run() {
            automatic();
        }
    };
    //endregion

    public MainCtListRobot(ListView mainCtListView, CtRecordsHandler ctRecordsHandler) {
        super();

        this.mainCtListView = mainCtListView;
        this.ctRecordsHandler = ctRecordsHandler;
        init();
    }

    private void init() {
        mOnExpiredTimersListener = null;
        mainCtListItemAdapter = (MainCtListItemAdapter) mainCtListView.getAdapter();
    }

    public void close() {
        mainCtListItemAdapter = null;
        mainCtListView = null;
        ctRecordsHandler = null;
    }

    public void setUpdateInterval(long updateInterval) {
        this.updateInterval = updateInterval;
    }

    public void startAutomatic(long delay) {
        handlerTime.postDelayed(runnableTime, delay);
    }

    public void stopAutomatic() {
        handlerTime.removeCallbacks(runnableTime);
    }

    private void automatic() {
        long nowm = System.currentTimeMillis();
        handlerTime.postDelayed(runnableTime, updateInterval);
        if (ctRecordsHandler.updateTimeAll(nowm) == 0) {
            mainCtListItemAdapter.setItems(ctRecordsHandler.getChronoTimers());
            int first = mainCtListView.getFirstVisiblePosition();
            int last = mainCtListView.getLastVisiblePosition();
            for (int i = first; i <= last; i = i + 1) {
                View view = mainCtListView.getChildAt(i - first);
                mainCtListItemAdapter.paintView(view, i);
            }
        } else {    // Au moins 1 timer a expiré - Evacuation générale
            handlerTime.removeCallbacks(runnableTime);
            if (mOnExpiredTimersListener != null) {
                mOnExpiredTimersListener.onExpiredTimers();
            }
        }
    }

}
