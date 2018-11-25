package com.example.pgyl.swtimer_a;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.DotMatrixFont;
import com.example.pgyl.pekislib_a.TimeDateUtils;

import static com.example.pgyl.pekislib_a.TimeDateUtils.msToHms;

public class CtDisplayTimeRobot {
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

    public static final boolean FORCE_RESET_STATE_CHANGE = true;
    private final long UPDATE_INTERVAL_RESET_MS = 40;        //   25 scrolls par seconde = +/- 4 caractères par secondes  (6 scrolls par caractère avec marge droite)
    private final long UPDATE_INTERVAL_NON_RESET_MS = 10;    //   Affichage du temps au 1/100e de seconde
    private final String RESET_MESSAGE = "...Sleeping...";
    //endregion

    //region Variables
    private DotMatrixDisplayView ctDisplayTimeView;
    private DotMatrixFont extraFont;
    private CtRecord currentCtRecord;
    private long updateInterval;
    private boolean inAutomatic;
    private boolean resetState;
    private Rect resetRect;
    private boolean oldResetState;
    private final Handler handlerTime = new Handler();
    private Runnable runnableTime = new Runnable() {
        @Override
        public void run() {
            automatic();
        }
    };
    //endregion

    public CtDisplayTimeRobot(DotMatrixDisplayView ctDisplayTimeView, CtRecord currentCtRecord) {
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
        oldResetState = false;
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

    public void updateCtDisplayTimeView(boolean forceResetStateChange) {
        resetState = currentCtRecord.isReset();
        if (forceResetStateChange || (resetState != oldResetState)) {
            ctDisplayTimeView.fillRectOff(resetRect);
            if (resetState) {
                ctDisplayTimeView.displayText(0, 0, RESET_MESSAGE, ctDisplayTimeView.getDefautFont());
                ctDisplayTimeView.appendText(msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
                updateInterval = UPDATE_INTERVAL_RESET_MS;

            } else {
                ctDisplayTimeView.displayText(0, 0, msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
                updateInterval = UPDATE_INTERVAL_NON_RESET_MS;
            }
            oldResetState = resetState;
        } else {
            if (resetState) {
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
            }
            updateCtDisplayTimeView(!FORCE_RESET_STATE_CHANGE);
            inAutomatic = false;
        }
    }

    private void setupExtraFont() {
        final int EXTRA_DOT_MATRIX_FONT_SYMBOL_RIGHT_MARGIN = 1;

        extraFont = new DotMatrixFont();
        for (EXTRA_DOT_MATRIX_SYMBOLS_DATA extraSymbolData : EXTRA_DOT_MATRIX_SYMBOLS_DATA.values()) {
            extraFont.addSymbol(extraSymbolData.valueChar, extraSymbolData.DATA());
        }
        extraFont.setWidth(EXTRA_DOT_MATRIX_SYMBOLS_DATA.ASCII_2E.DATA()[0].length);   //  Tous les caractères ont la même largeur et hauteur
        extraFont.setHeight(EXTRA_DOT_MATRIX_SYMBOLS_DATA.ASCII_2E.DATA().length);
        extraFont.setRightMargin(EXTRA_DOT_MATRIX_FONT_SYMBOL_RIGHT_MARGIN);
        //  Le "." surcharge le caractère précédent, le ":" est affiché sur une largeur réduite
        extraFont.getCharMap().get(EXTRA_DOT_MATRIX_SYMBOLS_DATA.ASCII_2E.valueChar).posInitialOffset = new Point(-extraFont.getWidth() - extraFont.getRightMargin() + 1, 1);
        extraFont.getCharMap().get(EXTRA_DOT_MATRIX_SYMBOLS_DATA.ASCII_2E.valueChar).posFinalOffset = new Point(extraFont.getWidth() + extraFont.getRightMargin() - 1, -1);
        extraFont.getCharMap().get(EXTRA_DOT_MATRIX_SYMBOLS_DATA.ASCII_3A.valueChar).posInitialOffset = new Point(-extraFont.getWidth() / 2, 0);
        extraFont.getCharMap().get(EXTRA_DOT_MATRIX_SYMBOLS_DATA.ASCII_3A.valueChar).posFinalOffset = new Point(extraFont.getWidth() / 2 + extraFont.getRightMargin() + 1, 0);
    }

}
