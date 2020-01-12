package com.example.pgyl.swtimer_a;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.DotMatrixFont;
import com.example.pgyl.pekislib_a.DotMatrixSymbol;
import com.example.pgyl.pekislib_a.TimeDateUtils;

import static com.example.pgyl.pekislib_a.TimeDateUtils.msToHms;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayColorBackIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayColorOffIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayColorOnMessageIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayColorOnTimeIndex;

public class CtDisplayDotMatrixDisplayUpdater {
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
    private DotMatrixDisplayView dotMatrixDisplayView;
    private DotMatrixFont extraFont;
    private Rect gridRect;
    private Rect displayRect;
    private String[] colors;
    private int onTimeColorIndex;
    private int onMessageColorIndex;
    private int offColorIndex;
    private int backColorIndex;
    private CtRecord currentCtRecord;
    private long updateInterval;
    private boolean inAutomatic;
    private Handler handlerTime;
    private Runnable runnableTime;
    //endregion

    public CtDisplayDotMatrixDisplayUpdater(DotMatrixDisplayView dotMatrixDisplayView, CtRecord currentCtRecord) {
        super();

        this.dotMatrixDisplayView = dotMatrixDisplayView;
        this.currentCtRecord = currentCtRecord;
        init();
    }

    private void init() {
        setupRunnableTime();
        setupExtraFont();
        setupIndexes();
        inAutomatic = false;
        mOnExpiredTimerListener = null;
    }

    public void close() {
        runnableTime = null;
        handlerTime = null;
        dotMatrixDisplayView = null;
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
        if ((!inAutomatic) && (!dotMatrixDisplayView.isDrawing())) {
            inAutomatic = true;
            update(!DISPLAY_INITIALIZE);
            inAutomatic = false;
        }
    }

    public void setGridDimensions() {       //  La grille doit pouvoir contenir le message et le temps
        final String EXTRA_FONT_TIME_CHARS = "::.";          //  ::.  dans HH:MM:SS.CC
        final String DEFAULT_FONT_TIME_CHARS = "00000000";   //  HHMMSSCC  dans HH:MM:SS.CC

        int displayRectWidth = extraFont.getTextWidth(EXTRA_FONT_TIME_CHARS) + dotMatrixDisplayView.getDefautFont().getTextWidth(DEFAULT_FONT_TIME_CHARS) - dotMatrixDisplayView.getDefautFont().getRightMargin();   //  Largeur du temps sans marge droite
        int displayRectHeight = Math.max(Math.max(extraFont.getTextHeight(EXTRA_FONT_TIME_CHARS), dotMatrixDisplayView.getDefautFont().getTextHeight(DEFAULT_FONT_TIME_CHARS)), dotMatrixDisplayView.getDefautFont().getTextHeight(currentCtRecord.getMessage()));   //  Hauteur du temps et du message
        int gridRectWidth = displayRectWidth + dotMatrixDisplayView.getDefautFont().getRightMargin() + dotMatrixDisplayView.getDefautFont().getTextWidth(currentCtRecord.getMessage());  //  Largeur du temps et du message, avec marge droite
        int gridRectHeight = displayRectHeight;
        gridRect = new Rect(0, 0, gridRectWidth, gridRectHeight);
        displayRect = new Rect(gridRect.left, gridRect.top, gridRect.left + displayRectWidth, gridRect.top + displayRectHeight);
        dotMatrixDisplayView.setGridRect(gridRect);
        dotMatrixDisplayView.setDisplayRect(displayRect);
        dotMatrixDisplayView.setScrollRect(gridRect);
    }

    public void setGridColors(String[] colors) {
        this.colors = colors;
        dotMatrixDisplayView.setOnColor(colors[onTimeColorIndex]);
        dotMatrixDisplayView.setOffColor(colors[offColorIndex]);
        dotMatrixDisplayView.setBackColor(colors[backColorIndex]);
    }

    public void writeTestText(String timeFontText, String messageFontText) {
        dotMatrixDisplayView.fillRectOff(gridRect);
        dotMatrixDisplayView.setSymbolPos(displayRect.left, displayRect.top);
        dotMatrixDisplayView.setOnColor(colors[onTimeColorIndex]);
        dotMatrixDisplayView.writeText(timeFontText, extraFont);
        dotMatrixDisplayView.setOnColor(colors[onMessageColorIndex]);
        dotMatrixDisplayView.writeText(messageFontText, dotMatrixDisplayView.getDefautFont());
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
                dotMatrixDisplayView.fillRectOff(gridRect);
                dotMatrixDisplayView.setSymbolPos(displayRect.left, displayRect.top);
                dotMatrixDisplayView.setOnColor(colors[onTimeColorIndex]);
                dotMatrixDisplayView.writeText(msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
                dotMatrixDisplayView.setOnColor(colors[onMessageColorIndex]);
                dotMatrixDisplayView.writeText(currentCtRecord.getMessage(), dotMatrixDisplayView.getDefautFont());
                dotMatrixDisplayView.setOnColor(colors[onTimeColorIndex]);
            } else {
                updateInterval = UPDATE_INTERVAL_NO_RESET_MS;
                dotMatrixDisplayView.fillRectOff(displayRect);
                dotMatrixDisplayView.setSymbolPos(displayRect.left, displayRect.top);
                dotMatrixDisplayView.writeText(msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
            }
        } else {
            if (currentCtRecord.isReset()) {
                dotMatrixDisplayView.scrollLeft();
            } else {
                dotMatrixDisplayView.noScroll();
                dotMatrixDisplayView.fillRectOff(displayRect);
                dotMatrixDisplayView.setSymbolPos(displayRect.left, displayRect.top);
                dotMatrixDisplayView.writeText(msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
            }
        }
        dotMatrixDisplayView.invalidate();
    }

    private void setupIndexes() {
        onTimeColorIndex = getDotMatrixDisplayColorOnTimeIndex();
        onMessageColorIndex = getDotMatrixDisplayColorOnMessageIndex();
        offColorIndex = getDotMatrixDisplayColorOffIndex();
        backColorIndex = getDotMatrixDisplayColorBackIndex();
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
        symbol.setPosInitialOffset(new Point(-dotMatrixDisplayView.getDefautFont().getRightMargin(), dotMatrixDisplayView.getDefautFont().getMaxSymbolHeight()));
        symbol.setPosFinalOffset(new Point(dotMatrixDisplayView.getDefautFont().getRightMargin(), -dotMatrixDisplayView.getDefautFont().getMaxSymbolHeight()));
        symbol = null;
    }

    private void setupRunnableTime() {
        handlerTime = new Handler();
        runnableTime = new Runnable() {
            @Override
            public void run() {
                automatic();
            }
        };
    }

}
