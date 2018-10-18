package com.example.pgyl.swtimer_a;

import android.os.Handler;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;

import static com.example.pgyl.pekislib_a.TimeDateUtils.TIMEUNITS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.convertMsToHms;

public class CtDisplayTimeRobot {
    public interface onExpiredTimerListener {
        void onExpiredTimer();
    }

    public void setOnExpiredTimerListener(onExpiredTimerListener listener) {
        mOnExpiredTimerListener = listener;
    }

    private onExpiredTimerListener mOnExpiredTimerListener;

    //region Variables
    private DotMatrixDisplayView timeDotMatrixDisplayView;
    private CtRecord currentCtRecord;
    private long updateInterval;
    private boolean inAutomatic;
    private final Handler handlerTime = new Handler();
    private Runnable runnableTime = new Runnable() {
        @Override
        public void run() {
            automatic();
        }
    };
    //endregion

    public CtDisplayTimeRobot(DotMatrixDisplayView timeDotMatrixDisplayView, CtRecord currentCtRecord) {
        super();

        this.timeDotMatrixDisplayView = timeDotMatrixDisplayView;
        this.currentCtRecord = currentCtRecord;
        init();
    }

    private void init() {
        inAutomatic = false;
        mOnExpiredTimerListener = null;
    }

    public void close() {
        timeDotMatrixDisplayView = null;
        currentCtRecord = null;
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
        if (currentCtRecord.isRunning()) {
            handlerTime.postDelayed(runnableTime, updateInterval);
            if ((!inAutomatic) && (!timeDotMatrixDisplayView.isDrawing())) {
                inAutomatic = true;
                if (currentCtRecord.updateTime(nowm)) {
                    if (!currentCtRecord.isSplitted()) {
                        timeDotMatrixDisplayView.fillGridOff();
                        timeDotMatrixDisplayView.drawText(0, 0, convertMsToHms(currentCtRecord.getTimeDisplay(), TIMEUNITS.CS));
                        timeDotMatrixDisplayView.invalidate();
                    }
                } else {    //  Le timer a expiré - Evacuation générale
                    handlerTime.removeCallbacks(runnableTime);
                    if (mOnExpiredTimerListener != null) {
                        mOnExpiredTimerListener.onExpiredTimer();
                    }
                }
                inAutomatic = false;
            }
        }
    }

}
