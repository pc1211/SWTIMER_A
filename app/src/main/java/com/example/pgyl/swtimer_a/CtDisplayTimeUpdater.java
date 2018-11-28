package com.example.pgyl.swtimer_a;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.DotMatrixFont;
import com.example.pgyl.pekislib_a.DotMatrixSymbol;
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
    public static final boolean DISPLAY_INITIALIZE = true;
    private final String RESET_MESSAGE = "...Sleep...";
    //endregion
    //region Variables
    private DotMatrixDisplayView ctDisplayTimeView;
    private DotMatrixFont extraFont;
    private CtRecord currentCtRecord;
    private long updateInterval;
    private boolean inAutomatic;
    private Rect displayRect;
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
        displayRect = new Rect(0, 0, getTimeDisplayWidth() - ctDisplayTimeView.getDefautFont().getRightMargin(), getDisplayHeight() + 1);   //  +1 pour la ligne du '.'
        resetRect = new Rect(displayRect.left, displayRect.top, getResetMessageDisplayWidth() + getTimeDisplayWidth(), displayRect.height());
        ctDisplayTimeView.setGridDimensions(displayRect, resetRect);
        inAutomatic = false;
        mOnExpiredTimerListener = null;
    }

    public void close() {
        ctDisplayTimeView = null;
        extraFont.close();
        extraFont = null;
        currentCtRecord = null;
    }

    public void startAutomatic() {
        handlerTime.postDelayed(runnableTime, updateInterval);
    }

    public void stopAutomatic() {
        handlerTime.removeCallbacks(runnableTime);
    }

    public void updateCtDisplayTimeView(boolean displayInitialize) {
        final long UPDATE_INTERVAL_RESET_MS = 40;        //   25 scrolls par seconde = +/- 4 caractères par secondes  (6 scrolls par caractère avec marge droite)
        final long UPDATE_INTERVAL_NON_RESET_MS = 10;    //   Affichage du temps au 1/100e de seconde

        long nowm = System.currentTimeMillis();
        if (!currentCtRecord.updateTime(nowm)) {    //  Le timer a expiré
            if (mOnExpiredTimerListener != null) {
                mOnExpiredTimerListener.onExpiredTimer();
            }
            displayInitialize = true;
        }
        if (displayInitialize) {
            if (currentCtRecord.isReset()) {
                updateInterval = UPDATE_INTERVAL_RESET_MS;
                ctDisplayTimeView.fillRectOff(resetRect);
                ctDisplayTimeView.displayText(0, 0, RESET_MESSAGE, ctDisplayTimeView.getDefautFont());
                ctDisplayTimeView.appendText(msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
            } else {
                updateInterval = UPDATE_INTERVAL_NON_RESET_MS;
                ctDisplayTimeView.fillRectOff(displayRect);
                ctDisplayTimeView.displayText(0, 0, msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
            }
        } else {
            if (currentCtRecord.isReset()) {
                ctDisplayTimeView.scrollLeft(resetRect);
            } else {
                ctDisplayTimeView.fillRectOff(displayRect);
                ctDisplayTimeView.displayText(0, 0, msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
            }
        }
        ctDisplayTimeView.invalidate();
    }

    private void automatic() {
        handlerTime.postDelayed(runnableTime, updateInterval);
        if ((!inAutomatic) && (!ctDisplayTimeView.isDrawing())) {
            inAutomatic = true;
            updateCtDisplayTimeView(!DISPLAY_INITIALIZE);
            inAutomatic = false;
        }
    }

    private int getTimeDisplayWidth() {   //  Largeur nécessaire pour afficher "HH:MM:SS.CC"   (avec marge droite)
        final String EXTRA_FONT_TIME_PATTERN = "::.";
        final String DEFAULT_FONT_TIME_PATTERN = "HHMMSSCC";

        return extraFont.getTextWidth(EXTRA_FONT_TIME_PATTERN) + ctDisplayTimeView.getDefautFont().getTextWidth(DEFAULT_FONT_TIME_PATTERN);
    }

    private int getResetMessageDisplayWidth() {   //  Largeur nécessaire pour afficher le message en situation de Reset  (avec marge droite)
        return ctDisplayTimeView.getDefautFont().getTextWidth(RESET_MESSAGE);
    }

    private int getDisplayHeight() {   //  Hauteur nécessaire pour pouvoir utiliser à la fois extraFont et ctDisplayTimeView.getDefautFont()
        return Math.max(extraFont.getHeight(), ctDisplayTimeView.getDefautFont().getHeight());
    }

    private void setupExtraFont() {
        //  Caractères redéfinis pour l'affichage du temps ("." et ":") (plus fins que la fonte par défaut de DotMatrixDisplayView)
        final DotMatrixSymbol[] EXTRA_FONT_SYMBOLS = {
                new DotMatrixSymbol('.', new int[][]{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 1}}),
                new DotMatrixSymbol(':', new int[][]{{0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 1, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}})
        };

        final int EXTRA_FONT_RIGHT_MARGIN = 1;
        DotMatrixSymbol symbol;

        extraFont = new DotMatrixFont();
        extraFont.setSymbols(EXTRA_FONT_SYMBOLS);
        extraFont.setRightMargin(EXTRA_FONT_RIGHT_MARGIN);
        symbol = extraFont.getCharMap().get('.');
        //  Le "." est affiché sur le caractère précédent:
        symbol.setPosInitialOffset(new Point(-symbol.getWidth() - extraFont.getRightMargin() + 1, 1));
        symbol.setPosFinalOffset(new Point(symbol.getWidth() + extraFont.getRightMargin() - 1, -1));
        //  le ":" est affiché sur une largeur réduite:
        symbol = extraFont.getCharMap().get(':');
        symbol.setPosInitialOffset(new Point(-symbol.getWidth() / 2, 0));
        symbol.setPosFinalOffset(new Point(symbol.getWidth() / 2 + extraFont.getRightMargin() + 1, 0));
        symbol = null;
    }

}
