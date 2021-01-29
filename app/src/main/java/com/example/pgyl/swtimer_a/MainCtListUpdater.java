package com.example.pgyl.swtimer_a;

import android.content.Context;
import android.os.Handler;
import android.widget.ListView;

import com.example.pgyl.pekislib_a.TimeDateUtils.TIME_UNITS;

import static com.example.pgyl.pekislib_a.Constants.CRLF;
import static com.example.pgyl.pekislib_a.MiscUtils.beep;
import static com.example.pgyl.pekislib_a.MiscUtils.toastLong;
import static com.example.pgyl.pekislib_a.TimeDateUtils.HHmmss;
import static com.example.pgyl.pekislib_a.TimeDateUtils.getFormattedTimeZoneLongTimeDate;

public class MainCtListUpdater {
    //region Variables
    private MainCtListItemAdapter mainCtListItemAdapter;
    private ListView mainCtListView;
    private CtRecordsHandler ctRecordsHandler;
    private Context context;
    private long updateInterval;
    private boolean needScrollBar;
    private Handler handlerTime;
    private Runnable runnableTime;
    private Runnable runnableCheckNeedScrollBar;
    private boolean automaticFlag;
    //endregion

    public MainCtListUpdater(ListView mainCtListView, CtRecordsHandler ctRecordsHandler, Context context) {
        super();

        this.mainCtListView = mainCtListView;
        this.ctRecordsHandler = ctRecordsHandler;
        this.context = context;
        init();
    }

    private void init() {
        setupRunnables();
        automaticFlag = false;
        updateInterval = TIME_UNITS.SEC.DURATION_MS();
        needScrollBar = false;
        setScrollBar(needScrollBar);
        setupCtRecordsHandler();
        setupMainCtListAdapter();
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

    public void startAutomatic(long nowm) {
        handlerTime.postDelayed(runnableTime, updateInterval - (System.currentTimeMillis() - nowm));   //  Respecter updateInterval à partir de nowm
    }

    public void stopAutomatic() {
        handlerTime.removeCallbacks(runnableTime);
    }

    private void automatic() {
        handlerTime.postDelayed(runnableTime, updateInterval);
        long nowm = System.currentTimeMillis();
        automaticFlag = true;
        ctRecordsHandler.checkAllTimersRunningExpired(nowm);   //  Déclenchera éventuellement onCtListExpiredTimer
        repaint(nowm);
    }

    private void onCtListExpiredTimer(CtRecord ctRecord) {
        toastLong("Timer " + ctRecord.getLabel() + CRLF + "expired @ " + getFormattedTimeZoneLongTimeDate(ctRecord.getTimeExp(), HHmmss), context);
        if (automaticFlag) {   //  => Pas suite au Reload() appelé par MainActivity au onResume(), qui ne doit pas produire de beep
            beep(context);
        }
    }

    public void reload() {
        ctRecordsHandler.sortCtRecords();
        long nowm = System.currentTimeMillis();
        ctRecordsHandler.checkAllTimersRunningExpired(nowm);   //  Déclenchera éventuellement onCtListExpiredTimer
        mainCtListItemAdapter.setItems(ctRecordsHandler.getChronoTimers());
        mainCtListItemAdapter.notifyDataSetChanged();
        mainCtListView.post(runnableCheckNeedScrollBar);
    }

    public void repaint(long nowm) {
        if (mainCtListView.getChildCount() > 0) {
            int firstVisiblePos = mainCtListView.getFirstVisiblePosition();
            int lastVisiblePos = mainCtListView.getLastVisiblePosition();
            for (int i = firstVisiblePos; i <= lastVisiblePos; i = i + 1) {
                mainCtListItemAdapter.paintView(mainCtListView.getChildAt(i - firstVisiblePos), i, nowm);
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

    private void setupCtRecordsHandler() {
        ctRecordsHandler.setOnExpiredTimerListener(new CtRecordsHandler.onExpiredTimerListener() {
            @Override
            public void onExpiredTimer(CtRecord ctRecord) {
                onCtListExpiredTimer(ctRecord);
            }
        });
    }

    private void setupMainCtListAdapter() {
        mainCtListItemAdapter = (MainCtListItemAdapter) mainCtListView.getAdapter();
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
