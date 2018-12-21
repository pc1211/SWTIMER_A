package com.example.pgyl.swtimer_a;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.DotMatrixFont;
import com.example.pgyl.pekislib_a.DotMatrixSymbol;
import com.example.pgyl.pekislib_a.TimeDateUtils;

import static com.example.pgyl.pekislib_a.TimeDateUtils.msToHms;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getMessageOnResetIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getTimeColorBackIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getTimeColorOffIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getTimeColorOnMessageIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getTimeColorOnTimeIndex;

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
    //endregion
    //region Variables
    private DotMatrixDisplayView timeDotMatrixDisplayView;
    private DotMatrixFont extraFont;
    private Rect gridRect;
    private Rect displayRect;
    private String[] colors;
    private String[] messages;
    private int onTimeColorIndex;
    private int onMessageColorIndex;
    private int offColorIndex;
    private int backColorIndex;
    private int messageOnResetIndex;
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

    public CtDisplayTimeUpdater(DotMatrixDisplayView timeDotMatrixDisplayView, CtRecord currentCtRecord) {
        super();

        this.timeDotMatrixDisplayView = timeDotMatrixDisplayView;
        this.currentCtRecord = currentCtRecord;
        init();
    }

    private void init() {
        setupExtraFont();
        setupIndexes();
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

    private void automatic() {
        handlerTime.postDelayed(runnableTime, updateInterval);
        if ((!inAutomatic) && (!timeDotMatrixDisplayView.isDrawing())) {
            inAutomatic = true;
            update(!DISPLAY_INITIALIZE);
            inAutomatic = false;
        }
    }

    public void setGridDimensions(String[] messages) {       //  La grille doit pouvoir contenir le message (on reset) et le temps
        final String EXTRA_FONT_TIME_CHARS = "::.";          //  ::.  dans HH:MM:SS.CC
        final String DEFAULT_FONT_TIME_CHARS = "00000000";   //  HHMMSSCC  dans HH:MM:SS.CC

        this.messages = messages;
        int displayRectWidth = extraFont.getTextWidth(EXTRA_FONT_TIME_CHARS) + timeDotMatrixDisplayView.getDefautFont().getTextWidth(DEFAULT_FONT_TIME_CHARS) - timeDotMatrixDisplayView.getDefautFont().getRightMargin();   //  Largeur du temps sans marge droite
        int displayRectHeight = Math.max(Math.max(extraFont.getTextHeight(EXTRA_FONT_TIME_CHARS), timeDotMatrixDisplayView.getDefautFont().getTextHeight(DEFAULT_FONT_TIME_CHARS)), timeDotMatrixDisplayView.getDefautFont().getTextHeight(messages[messageOnResetIndex]));   //  Hauteur du temps et du message
        int gridRectWidth = displayRectWidth + timeDotMatrixDisplayView.getDefautFont().getRightMargin() + timeDotMatrixDisplayView.getDefautFont().getTextWidth(messages[messageOnResetIndex]);  //  Largeur du temps et du message, avec marge droite
        int gridRectHeight = displayRectHeight;
        gridRect = new Rect(0, 0, gridRectWidth, gridRectHeight);
        displayRect = new Rect(gridRect.left, gridRect.top, gridRect.left + displayRectWidth, gridRect.top + displayRectHeight);
        timeDotMatrixDisplayView.setGridRect(gridRect);
        timeDotMatrixDisplayView.setDisplayRect(displayRect);
        timeDotMatrixDisplayView.setScrollRect(gridRect);
    }

    public void setGridColors(String[] colors) {
        this.colors = colors;
        timeDotMatrixDisplayView.setOnColor(colors[onTimeColorIndex]);
        timeDotMatrixDisplayView.setOffColor(colors[offColorIndex]);
        timeDotMatrixDisplayView.setBackColor(colors[backColorIndex]);
    }

    public void update(boolean displayInitialize) {
        final long UPDATE_INTERVAL_RESET_MS = 40;       //   25 scrolls par seconde = +/- 4 caractères par secondes  (6 scrolls par caractère avec marge droite)
        final long UPDATE_INTERVAL_NO_RESET_MS = 10;    //   Affichage du temps au 1/100e de seconde

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
                timeDotMatrixDisplayView.fillRectOff(gridRect);
                timeDotMatrixDisplayView.setOnColor(colors[onMessageColorIndex]);
                timeDotMatrixDisplayView.setSymbolPos(displayRect.left, displayRect.top);
                timeDotMatrixDisplayView.writeText(messages[messageOnResetIndex], timeDotMatrixDisplayView.getDefautFont());
                timeDotMatrixDisplayView.setOnColor(colors[onTimeColorIndex]);
                timeDotMatrixDisplayView.writeText(msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
            } else {
                updateInterval = UPDATE_INTERVAL_NO_RESET_MS;
                timeDotMatrixDisplayView.fillRectOff(displayRect);
                timeDotMatrixDisplayView.setSymbolPos(displayRect.left, displayRect.top);
                timeDotMatrixDisplayView.writeText(msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
            }
        } else {
            if (currentCtRecord.isReset()) {
                timeDotMatrixDisplayView.scrollLeft();
            } else {
                timeDotMatrixDisplayView.noScroll();
                timeDotMatrixDisplayView.fillRectOff(displayRect);
                timeDotMatrixDisplayView.setSymbolPos(displayRect.left, displayRect.top);
                timeDotMatrixDisplayView.writeText(msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
            }
        }
        timeDotMatrixDisplayView.invalidate();
    }

    private void setupIndexes() {
        onTimeColorIndex = getTimeColorOnTimeIndex();
        onMessageColorIndex = getTimeColorOnMessageIndex();
        offColorIndex = getTimeColorOffIndex();
        backColorIndex = getTimeColorBackIndex();
        messageOnResetIndex = getMessageOnResetIndex();
    }

    private void setupExtraFont() {
        //  Caractères redéfinis pour l'affichage du temps ("." et ":") (plus fins que la fonte par défaut de DotMatrixDisplayView)
        final DotMatrixSymbol[] EXTRA_FONT_SYMBOLS = {
                new DotMatrixSymbol('.', new int[][]{{1}}),
                new DotMatrixSymbol(':', new int[][]{{0}, {0}, {1}, {0}, {1}, {0}, {0}})
        };

        final int EXTRA_FONT_RIGHT_MARGIN = 1;
        DotMatrixSymbol symbol;

        extraFont = new DotMatrixFont();
        extraFont.setSymbols(EXTRA_FONT_SYMBOLS);
        extraFont.setRightMargin(EXTRA_FONT_RIGHT_MARGIN);
        symbol = extraFont.getSymbol('.');     //  Le "." est affiché en-dessous à droite du caractère précédent:
        symbol.setPosInitialOffset(new Point(-timeDotMatrixDisplayView.getDefautFont().getRightMargin(), timeDotMatrixDisplayView.getDefautFont().getMaxSymbolHeight()));
        symbol.setPosFinalOffset(new Point(timeDotMatrixDisplayView.getDefautFont().getRightMargin(), -timeDotMatrixDisplayView.getDefautFont().getMaxSymbolHeight()));
        symbol = null;
    }

}
