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
    private final String SLEEP_MESSAGE = "...Sleep...";
    final String EXTRA_FONT_TIME_CHARS = "::.";
    final String DEFAULT_FONT_TIME_CHARS = "00000000";   //  HHMMSSCC
    //endregion
    //region Variables
    private DotMatrixDisplayView timeDotMatrixDisplayView;
    private DotMatrixFont extraFont;
    private CtRecord currentCtRecord;
    private long updateInterval;
    private boolean inAutomatic;
    private Rect displayRect;
    private Rect extendedRect;
    private final Handler handlerTime = new Handler();
    private Runnable runnableTime = new Runnable() {
        @Override
        public void run() {
            automatic();
        }
    };
    //endregion

    public CtDisplayTimeUpdater(DotMatrixDisplayView timeDotMatrixDisplayView, CtRecord currentCtRecord) {
        super();

        this.timeDotMatrixDisplayView = timeDotMatrixDisplayView;
        this.currentCtRecord = currentCtRecord;
        init();
    }

    private void init() {
        setupExtraFont();
        displayRect = new Rect(0, 0, getTimeDisplayWidth() - timeDotMatrixDisplayView.getDefautFont().getRightMargin(), Math.max(getTimeDisplayHeight(), getMessageDisplayHeight()));   //  +1 pour la ligne du '.'
        extendedRect = new Rect(displayRect.left, displayRect.top, getMessageDisplayWidth() + getTimeDisplayWidth(), displayRect.height());
        timeDotMatrixDisplayView.setGridDimensions(displayRect, extendedRect);
        inAutomatic = false;
        mOnExpiredTimerListener = null;
    }

    public void close() {
        timeDotMatrixDisplayView = null;
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
        final long UPDATE_INTERVAL_SLEEP_MS = 40;        //   25 scrolls par seconde = +/- 4 caractères par secondes  (6 scrolls par caractère avec marge droite)
        final long UPDATE_INTERVAL_NO_SLEEP_MS = 10;    //   Affichage du temps au 1/100e de seconde

        long nowm = System.currentTimeMillis();
        if (!currentCtRecord.updateTime(nowm)) {    //  Le timer a expiré
            if (mOnExpiredTimerListener != null) {
                mOnExpiredTimerListener.onExpiredTimer();
            }
            displayInitialize = true;
        }
        if (displayInitialize) {
            if (currentCtRecord.isReset()) {
                updateInterval = UPDATE_INTERVAL_SLEEP_MS;
                timeDotMatrixDisplayView.fillRectOff(extendedRect);
                timeDotMatrixDisplayView.displayText(0, 0, SLEEP_MESSAGE, timeDotMatrixDisplayView.getDefautFont());
                timeDotMatrixDisplayView.appendText(msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
            } else {
                updateInterval = UPDATE_INTERVAL_NO_SLEEP_MS;
                timeDotMatrixDisplayView.fillRectOff(displayRect);
                timeDotMatrixDisplayView.displayText(0, 0, msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
            }
        } else {
            if (currentCtRecord.isReset()) {
                timeDotMatrixDisplayView.scrollLeft(extendedRect);
            } else {
                timeDotMatrixDisplayView.fillRectOff(displayRect);
                timeDotMatrixDisplayView.displayText(0, 0, msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
            }
        }
        timeDotMatrixDisplayView.invalidate();
    }

    private void automatic() {
        handlerTime.postDelayed(runnableTime, updateInterval);
        if ((!inAutomatic) && (!timeDotMatrixDisplayView.isDrawing())) {
            inAutomatic = true;
            updateCtDisplayTimeView(!DISPLAY_INITIALIZE);
            inAutomatic = false;
        }
    }

    private int getTimeDisplayWidth() {   //  Largeur nécessaire pour afficher "HH:MM:SS.CC"   (avec marge droite)
        return extraFont.getTextWidth(EXTRA_FONT_TIME_CHARS) + timeDotMatrixDisplayView.getDefautFont().getTextWidth(DEFAULT_FONT_TIME_CHARS);
    }

    private int getTimeDisplayHeight() {   //  Hauteur
        return Math.max(extraFont.getTextHeight(EXTRA_FONT_TIME_CHARS), timeDotMatrixDisplayView.getDefautFont().getTextHeight(DEFAULT_FONT_TIME_CHARS));
    }

    private int getMessageDisplayWidth() {   //  Largeur nécessaire pour afficher le message en situation de Reset  (avec marge droite)
        return timeDotMatrixDisplayView.getDefautFont().getTextWidth(SLEEP_MESSAGE);
    }

    private int getMessageDisplayHeight() {   //  Hauteur
        return timeDotMatrixDisplayView.getDefautFont().getTextHeight(SLEEP_MESSAGE);
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
        symbol = extraFont.getSymbol('.');
        //  Le "." est affiché sur le caractère précédent:
        symbol.setPosInitialOffset(new Point(-symbol.getWidth() - extraFont.getRightMargin() + 1, 1));
        symbol.setPosFinalOffset(new Point(symbol.getWidth() + extraFont.getRightMargin() - 1, -1));
        //  le ":" est affiché sur une largeur réduite:
        symbol = extraFont.getSymbol(':');
        symbol.setPosInitialOffset(new Point(-symbol.getWidth() / 2, 0));
        symbol.setPosFinalOffset(new Point(symbol.getWidth() / 2 + extraFont.getRightMargin() + 1, 0));
        symbol = null;
    }

}
