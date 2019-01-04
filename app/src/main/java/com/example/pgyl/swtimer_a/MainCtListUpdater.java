package com.example.pgyl.swtimer_a;

import android.os.Handler;
import android.view.View;
import android.widget.ListView;

public class MainCtListUpdater {
    public interface onExpiredTimersListener {
        void onExpiredTimers();
    }

    public void setOnExpiredTimersListener(onExpiredTimersListener listener) {
        mOnExpiredTimersListener = listener;
    }

    private onExpiredTimersListener mOnExpiredTimersListener;

    //region Constantes
    private final int UPDATE_MAIN_CTLIST_TIME_INTERVAL_MS = 1000;
    //endregion
    //region Variables
    private MainCtListItemAdapter mainCtListItemAdapter;
    private ListView mainCtListView;
    private CtRecordsHandler ctRecordsHandler;
    private long updateInterval;
    private boolean needScrollBar;
    private final Handler handlerTime = new Handler();
    private Runnable runnableTime = new Runnable() {
        @Override
        public void run() {
            automatic();
        }
    };
    //endregion

    public MainCtListUpdater(ListView mainCtListView, CtRecordsHandler ctRecordsHandler) {
        super();

        this.mainCtListView = mainCtListView;
        this.ctRecordsHandler = ctRecordsHandler;
        init();
    }

    private void init() {
        updateInterval = UPDATE_MAIN_CTLIST_TIME_INTERVAL_MS;
        mOnExpiredTimersListener = null;
        needScrollBar = false;
        setScrollBar(needScrollBar);
        mainCtListItemAdapter = (MainCtListItemAdapter) mainCtListView.getAdapter();
    }

    public void close() {
        mainCtListItemAdapter = null;
        mainCtListView = null;
        ctRecordsHandler = null;
    }

    public void startAutomatic() {
        handlerTime.postDelayed(runnableTime, updateInterval);
    }

    public void stopAutomatic() {
        handlerTime.removeCallbacks(runnableTime);
    }

    private void automatic() {
        handlerTime.postDelayed(runnableTime, updateInterval);
        update();
    }

    public void reload() {
        mainCtListItemAdapter.setItems(ctRecordsHandler.getChronoTimers());
        mainCtListItemAdapter.notifyDataSetChanged();
    }

    public void update() {
        long nowm = System.currentTimeMillis();
        if (ctRecordsHandler.updateTimeAll(nowm) == 0) {
            mainCtListItemAdapter.setItems(ctRecordsHandler.getChronoTimers());
            int firstVisiblePos = mainCtListView.getFirstVisiblePosition();
            int lastVisiblePos = mainCtListView.getLastVisiblePosition();
            int firstFullVisiblePos = firstVisiblePos;
            int lastFullVisiblePos = lastVisiblePos;
            for (int i = firstVisiblePos; i <= lastVisiblePos; i = i + 1) {
                View view = mainCtListView.getChildAt(i - firstVisiblePos);
                if ((i == 0) && (view.getTop() < 0)) {   //  Le 1er item visible ne l'est que partiellement
                    firstFullVisiblePos = firstFullVisiblePos + 1;
                }
                if ((i == lastVisiblePos) && (view.getBottom() > mainCtListView.getHeight())) {   //  Le dernier item visible ne l'est que partiellement
                    lastFullVisiblePos = lastFullVisiblePos - 1;
                }
                mainCtListItemAdapter.paintView(view, i);
            }
            boolean b = (((firstFullVisiblePos == 0) && (lastFullVisiblePos == (mainCtListView.getCount() - 1))) ? false : true);  // false si toute la liste est entièrement visible
            if (b != needScrollBar) {
                needScrollBar = b;
                setScrollBar(needScrollBar);
            }
        } else {    // Au moins 1 timer a expiré - Evacuation générale
            handlerTime.removeCallbacks(runnableTime);
            if (mOnExpiredTimersListener != null) {
                mOnExpiredTimersListener.onExpiredTimers();
            }
        }
    }

    private void setScrollBar(boolean enabled) {
        mainCtListView.setFastScrollEnabled(enabled);
        mainCtListView.setFastScrollAlwaysVisible(enabled);
    }

}
