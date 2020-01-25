package com.example.pgyl.swtimer_a;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.DotMatrixFont;
import com.example.pgyl.pekislib_a.DotMatrixSymbol;
import com.example.pgyl.pekislib_a.TimeDateUtils;

import static com.example.pgyl.pekislib_a.TimeDateUtils.msToHms;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayBackIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayOffIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayOnLabelIndex;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.getDotMatrixDisplayOnTimeIndex;

public class CtDisplayDotMatrixDisplayUpdater {
    public interface onExpiredTimerListener {
        void onExpiredTimer();
    }

    public void setOnExpiredTimerListener(onExpiredTimerListener listener) {
        mOnExpiredTimerListener = listener;
    }

    private onExpiredTimerListener mOnExpiredTimerListener;

    //region Variables
    private DotMatrixDisplayView dotMatrixDisplayView;
    private DotMatrixFont extraFont;
    private Rect gridRect;
    private Rect displayRect;
    private String[] colors;
    private int onTimeColorIndex;
    private int onLabelColorIndex;
    private int offColorIndex;
    private int backColorIndex;
    private CtRecord currentCtRecord;
    private long updateInterval;
    private boolean inAutomatic;
    private boolean automaticOn;
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
        automaticOn = false;
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

    public void setUpdateInterval(long updateInterval) {
        this.updateInterval = updateInterval;
    }

    public void startAutomatic() {
        handlerTime.postDelayed(runnableTime, updateInterval);
        automaticOn = true;
    }

    public void stopAutomatic() {
        handlerTime.removeCallbacks(runnableTime);
        automaticOn = false;
    }

    public boolean isAutomaticOn() {
        return automaticOn;
    }

    private void automatic() {
        handlerTime.postDelayed(runnableTime, updateInterval);
        if ((!inAutomatic) && (!dotMatrixDisplayView.isDrawing())) {
            inAutomatic = true;
            long nowm = System.currentTimeMillis();
            if (!currentCtRecord.updateTime(nowm)) {    //  Le timer a expiré
                if (mOnExpiredTimerListener != null) {
                    mOnExpiredTimerListener.onExpiredTimer();
                }
            }
            refreshDisplay();
            inAutomatic = false;
        }
    }

    public void setGridDimensions() {       //  La grille doit pouvoir contenir le temps et le label
        final String EXTRA_FONT_TIME_CHARS = "::.";          //  ::.  dans HH:MM:SS.CC
        final String DEFAULT_FONT_TIME_CHARS = "00000000";   //  HHMMSSCC  dans HH:MM:SS.CC

        int displayRectWidth = extraFont.getTextWidth(EXTRA_FONT_TIME_CHARS) + dotMatrixDisplayView.getDefautFont().getTextWidth(DEFAULT_FONT_TIME_CHARS) - dotMatrixDisplayView.getDefautFont().getRightMargin();   //  Largeur du temps sans la dernière marge droite
        int displayRectHeight = Math.max(Math.max(extraFont.getTextHeight(EXTRA_FONT_TIME_CHARS), dotMatrixDisplayView.getDefautFont().getTextHeight(DEFAULT_FONT_TIME_CHARS)), dotMatrixDisplayView.getDefautFont().getTextHeight(currentCtRecord.getLabel()));   //  Hauteur du temps et du label
        int gridRectWidth = displayRectWidth + dotMatrixDisplayView.getDefautFont().getRightMargin() + dotMatrixDisplayView.getDefautFont().getTextWidth(currentCtRecord.getLabel());  //  Largeur du temps et du label, avec marge droite
        int gridRectHeight = displayRectHeight;
        gridRect = new Rect(0, 0, gridRectWidth, gridRectHeight);
        displayRect = new Rect(0, 0, displayRectWidth, displayRectHeight);
        dotMatrixDisplayView.setGridRect(gridRect);  //  La grille entière est de la taille prévue pour le temps et le label
        dotMatrixDisplayView.setDisplayRect(displayRect);  //  la zone à afficher est de la taille prévue pour le temps
        dotMatrixDisplayView.setScrollRect(gridRect);  //  On scrolle la grille entière
    }

    public void setGridColors(String[] colors) {
        this.colors = colors;
        dotMatrixDisplayView.setOnColor(colors[onTimeColorIndex]);
        dotMatrixDisplayView.setOffColor(colors[offColorIndex]);
        dotMatrixDisplayView.setBackColor(colors[backColorIndex]);
    }

    public void displayTimeAndLabel(String timeText, String labelText) {
        dotMatrixDisplayView.fillRectOff(gridRect);
        dotMatrixDisplayView.setSymbolPos(displayRect.left, displayRect.top);
        dotMatrixDisplayView.setOnColor(colors[onTimeColorIndex]);
        dotMatrixDisplayView.writeText(timeText, extraFont);   //  Temps avec police extra
        dotMatrixDisplayView.setOnColor(colors[onLabelColorIndex]);
        dotMatrixDisplayView.writeText(labelText, dotMatrixDisplayView.getDefautFont());   //  Label avec police par défaut
        dotMatrixDisplayView.setOnColor(colors[onTimeColorIndex]);
        dotMatrixDisplayView.invalidate();
    }

    public void displayCurrentRecordTimeAndLabel() {
        displayTimeAndLabel(msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), currentCtRecord.getLabel());
    }

    public void refreshDisplay() {
        if (currentCtRecord.isReset()) {
            dotMatrixDisplayView.scrollLeft();
        } else {
            dotMatrixDisplayView.noScroll();
            dotMatrixDisplayView.fillRectOff(displayRect);
            dotMatrixDisplayView.setSymbolPos(displayRect.left, displayRect.top);
            dotMatrixDisplayView.writeText(msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont);
        }
        dotMatrixDisplayView.invalidate();
    }

    private void setupIndexes() {
        onTimeColorIndex = getDotMatrixDisplayOnTimeIndex();
        onLabelColorIndex = getDotMatrixDisplayOnLabelIndex();
        offColorIndex = getDotMatrixDisplayOffIndex();
        backColorIndex = getDotMatrixDisplayBackIndex();
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
