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
    private boolean automaticOn;
    private Handler handlerTime;
    private Runnable runnableTime;
    private Runnable runnableCheckNeedScrollBar;
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
        updateInterval = TIME_UNITS.SEC.DURATION_MS();
        needScrollBar = false;
        automaticOn = false;
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

    public void startAutomatic() {
        automaticOn = true;
        handlerTime.postDelayed(runnableTime, updateInterval);
    }

    public void stopAutomatic() {
        automaticOn = false;
        handlerTime.removeCallbacks(runnableTime);
    }

    private void automatic() {
        handlerTime.postDelayed(runnableTime, updateInterval);
        long nowm = System.currentTimeMillis();
        ctRecordsHandler.updateTimeAll(nowm);
        repaint();
    }

    private void onCtListItemButtonClick() {    //  Reprogrammer le timer automatique
        stopAutomatic();
        startAutomatic();
    }

    private void onCtListExpiredTimer(CtRecord ctRecord) {
        long nowm = System.currentTimeMillis();
        ctRecordsHandler.updateTimeAll(nowm);
        repaint();
        toastLong("Timer " + ctRecord.getLabel() + CRLF + "expired @ " + getFormattedTimeZoneLongTimeDate(ctRecord.getTimeExp(), HHmmss), context);
        if (automaticOn) {   //  => Pas de Beep au Resume suite à reload de MainCtList()
            beep(context);
        }
    }

    public void reload() {
        ctRecordsHandler.sortCtRecords();
        mainCtListItemAdapter.setItems(ctRecordsHandler.getChronoTimers());
        mainCtListItemAdapter.notifyDataSetChanged();
        long nowm = System.currentTimeMillis();
        ctRecordsHandler.updateTimeAll(nowm);
        mainCtListView.post(runnableCheckNeedScrollBar);
    }

    public void repaint() {
        if (mainCtListView.getChildCount() > 0) {
            int firstVisiblePos = mainCtListView.getFirstVisiblePosition();
            int lastVisiblePos = mainCtListView.getLastVisiblePosition();
            for (int i = firstVisiblePos; i <= lastVisiblePos; i = i + 1) {
                mainCtListItemAdapter.paintView(mainCtListView.getChildAt(i - firstVisiblePos), i);
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
        mainCtListItemAdapter.setOnItemButtonClick(new MainCtListItemAdapter.onButtonClickListener() {
            @Override
            public void onButtonClick() {
                onCtListItemButtonClick();
            }
        });
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
