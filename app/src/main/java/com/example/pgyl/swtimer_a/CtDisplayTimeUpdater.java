package com.example.pgyl.swtimer_a;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.DotMatrixFont;
import com.example.pgyl.pekislib_a.TimeDateUtils;

import static com.example.pgyl.pekislib_a.TimeDateUtils.msToHms;

public class CtDisplayTimeUpdater {
    public interface onExpiredTimerListener {
        void onExpiredTimer();
    }

    public void setOnExpiredTimerListener(onExpiredTimerListener listener) {
        mOnExpiredTimerListener = listener;
    }

    private onExpiredTimerListener mOnExpiredTimerListener;

    //region Constantes
    private enum EXTRA_DOT_MATRIX_SYMBOLS_DATA {    //  Caractères redéfinis pour l'affichage du temps ("." et ":") (plus fins que la fonte par défaut de DotMatrixDisplayView)
        ASCII_2E('.', new int[][]{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 1}}),
        ASCII_3A(':', new int[][]{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}});

        private Character valueChar;
        private int[][] valueData;

        EXTRA_DOT_MATRIX_SYMBOLS_DATA(Character valueChar, int[][] valueData) {
            this.valueChar = valueChar;
            this.valueData = valueData;
        }

        public int[][] DATA() {
            return valueData;
        }
    }

    public static final boolean DISPLAY_INITIALIZE = true;
    private final long UPDATE_INTERVAL_RESET_MS = 40;        //   25 scrolls par seconde = +/- 4 caractères par secondes  (6 scrolls par caractère avec marge droite)
    private final long UPDATE_INTERVAL_NON_RESET_MS = 10;    //   Affichage du temps au 1/100e de seconde
    private final String RESET_MESSAGE = "...Sleep...";
    //endregion

    //region Variables
    private DotMatrixDisplayView ctDisplayTimeView;
    private DotMatrixFont extraFont;
    private CtRecord currentCtRecord;
    private long updateInterval;
    private boolean inAutomatic;
    private Rect resetRect;
    private final Handler handlerTime = new Handler();
    private Runnable runnableTime = new Runnable() {
        @Override
        public void run() {
            automatic();
        }
    };
    //endregion

    public CtDisplayTimeUpdater(DotMatrixDisplayView ctDisplayTimeView, CtRecord currentCtRecord) {
        super();

        this.ctDisplayTimeView = ctDisplayTimeView;
        this.currentCtRecord = currentCtRecord;
        init();
    }

    private void init() {
        setupExtraFont();
        resetRect = new Rect(0, 0, ctDisplayTimeView.getDisplayRect().width() + 1 + RESET_MESSAGE.length() * (ctDisplayTimeView.getDefautFont().getWidth() + ctDisplayTimeView.getDefautFont().getRightMargin()), ctDisplayTimeView.getDefautFont().getHeight());
        inAutomatic = false;
        mOnExpiredTimerListener = null;
        updateInterval = UPDATE_INTERVAL_RESET_MS;
    }

    public void close() {
        ctDisplayTimeView = null;
        extraFont.close();
        extraFont = null;
        currentCtRecord = null;
    }

    public void startAutomatic(long delay) {
        handlerTime.postDelayed(runnableTime, delay);
    }

    public void stopAutomatic() {
        handlerTime.removeCallbacks(runnableTime);
    }

    public void updateCtDisplayTimeView(boolean displayInitialize) {
        if (displayInitialize) {
            if (currentCtRecord.isReset()) {
                updateInterval = UPDATE_INTERVAL_RESET_MS;
                ctDisplayTimeView.fillRectOff(resetRect);
                ctDisplayTimeView.displayText(0, 0, RESET_MESSAGE, ctDisplayTimeView.getDefautFont());
                ctDisplayTimeView.appendText(msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
            } else {
                updateInterval = UPDATE_INTERVAL_NON_RESET_MS;
                ctDisplayTimeView.fillRectOff(ctDisplayTimeView.getDisplayRect());
                ctDisplayTimeView.displayText(0, 0, msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
            }
        } else {
            if (currentCtRecord.isReset()) {
                ctDisplayTimeView.scrollLeft(resetRect);
            } else {
                ctDisplayTimeView.fillRectOff(ctDisplayTimeView.getDisplayRect());
                ctDisplayTimeView.displayText(0, 0, msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
            }
        }
        ctDisplayTimeView.invalidate();
    }

    private void automatic() {
        long nowm = System.currentTimeMillis();
        handlerTime.postDelayed(runnableTime, updateInterval);
        if ((!inAutomatic) && (!ctDisplayTimeView.isDrawing())) {
            inAutomatic = true;
            if (!currentCtRecord.updateTime(nowm)) {    //  Le timer a expiré
                if (mOnExpiredTimerListener != null) {
                    mOnExpiredTimerListener.onExpiredTimer();
                }
                updateCtDisplayTimeView(DISPLAY_INITIALIZE);
            } else {
                updateCtDisplayTimeView(!DISPLAY_INITIALIZE);
            }
            inAutomatic = false;
        }
    }

    private void setupExtraFont() {
        final int EXTRA_DOT_MATRIX_FONT_SYMBOL_RIGHT_MARGIN = 1;

        extraFont = new DotMatrixFont();
        for (EXTRA_DOT_MATRIX_SYMBOLS_DATA extraSymbolData : EXTRA_DOT_MATRIX_SYMBOLS_DATA.values()) {
            extraFont.addSymbol(extraSymbolData.valueChar, extraSymbolData.DATA());
        }
        extraFont.setRightMargin(EXTRA_DOT_MATRIX_FONT_SYMBOL_RIGHT_MARGIN);
        //  Le "." surcharge le caractère précédent, le ":" est affiché sur une largeur réduite
        extraFont.getCharMap().get(EXTRA_DOT_MATRIX_SYMBOLS_DATA.ASCII_2E.valueChar).setPosInitialOffset(new Point(-extraFont.getWidth() - extraFont.getRightMargin() + 1, 1));
        extraFont.getCharMap().get(EXTRA_DOT_MATRIX_SYMBOLS_DATA.ASCII_2E.valueChar).setPosFinalOffset(new Point(extraFont.getWidth() + extraFont.getRightMargin() - 1, -1));
        extraFont.getCharMap().get(EXTRA_DOT_MATRIX_SYMBOLS_DATA.ASCII_3A.valueChar).setPosInitialOffset(new Point(-extraFont.getWidth() / 2, 0));
        extraFont.getCharMap().get(EXTRA_DOT_MATRIX_SYMBOLS_DATA.ASCII_3A.valueChar).setPosFinalOffset(new Point(extraFont.getWidth() / 2 + extraFont.getRightMargin() + 1, 0));
    }

}
