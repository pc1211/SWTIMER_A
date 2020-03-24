package com.example.pgyl.swtimer_a;

import android.os.Handler;
import android.widget.ListView;

import com.example.pgyl.pekislib_a.TimeDateUtils.TIME_UNITS;

public class MainCtListUpdater {
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
    private boolean needScrollBar;
    private Handler handlerTime;
    private Runnable runnableTime;
    private Runnable runnableCheckNeedScrollBar;
    //endregion

    public MainCtListUpdater(ListView mainCtListView, CtRecordsHandler ctRecordsHandler) {
        super();

        this.mainCtListView = mainCtListView;
        this.ctRecordsHandler = ctRecordsHandler;
        init();
    }

    private void init() {
        setupRunnables();
        updateInterval = TIME_UNITS.SEC.DURATION_MS();
        mOnExpiredTimersListener = null;
        needScrollBar = false;
        setScrollBar(needScrollBar);
        mainCtListItemAdapter = (MainCtListItemAdapter) mainCtListView.getAdapter();
    }

    public void close() {
        mainCtListView.removeCallbacks(runnableCheckNeedScrollBar);
        handlerTime.removeCallbacks(runnableTime);
        runnableTime = null;
        handlerTime = null;
        runnableCheckNeedScrollBar = null;
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
        mainCtListView.post(runnableCheckNeedScrollBar);   //  Tous les items de la listview sont alors complètement dessinés
    }

    public void update() {
        long nowm = System.currentTimeMillis();
        if (ctRecordsHandler.updateTimeAll(nowm) == 0) {
            mainCtListItemAdapter.setItems(ctRecordsHandler.getChronoTimers());
            if (mainCtListView.getChildCount() > 0) {
                int firstVisiblePos = mainCtListView.getFirstVisiblePosition();
                int lastVisiblePos = mainCtListView.getLastVisiblePosition();
                for (int i = firstVisiblePos; i <= lastVisiblePos; i = i + 1) {
                    mainCtListItemAdapter.paintView(mainCtListView.getChildAt(i - firstVisiblePos), i);
                }
            }
        } else {    // Au moins 1 timer a expiré - Evacuation générale
            handlerTime.removeCallbacks(runnableTime);
            if (mOnExpiredTimersListener != null) {
                mOnExpiredTimersListener.onExpiredTimers();
            }
        }
    }

    public void checkNeedScrollBar() {
        if (mainCtListView.getChildCount() > 0) {
            int firstVisiblePos = mainCtListView.getFirstVisiblePosition();
            int lastVisiblePos = mainCtListView.getLastVisiblePosition();
            int firstFullVisiblePos = firstVisiblePos;
            int lastFullVisiblePos = lastVisiblePos;
            if (mainCtListView.getChildAt(0).getTop() < 0) {   //  Le 1er item visible ne l'est que partiellement
                firstFullVisiblePos = firstFullVisiblePos + 1;
            }
            if (mainCtListView.getChildAt(lastVisiblePos - firstVisiblePos).getBottom() > mainCtListView.getHeight()) {   //  Le dernier item visible ne l'est que partiellement
                lastFullVisiblePos = lastFullVisiblePos - 1;
            }
            int n = mainCtListView.getCount();
            boolean b = (((firstFullVisiblePos == 0) && (lastFullVisiblePos == (mainCtListView.getCount() - 1))) ? false : true);  // false si toute la liste est entièrement visible
            if (b != needScrollBar) {
                needScrollBar = b;
                setScrollBar(needScrollBar);
            }
        }
    }

    private void setScrollBar(boolean enabled) {
        mainCtListView.setFastScrollEnabled(enabled);
        mainCtListView.setFastScrollAlwaysVisible(enabled);
    }

    private void setupRunnables() {
        handlerTime = new Handler();
        runnableTime = new Runnable() {
            @Override
            public void run() {
                automatic();
            }
        };
        runnableCheckNeedScrollBar = new Runnable() {
            @Override
            public void run() {
                checkNeedScrollBar();
            }
        };
    }

}
