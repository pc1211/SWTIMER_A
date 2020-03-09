package com.example.pgyl.swtimer_a;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.DotMatrixFont;
import com.example.pgyl.pekislib_a.DotMatrixFontUtils;
import com.example.pgyl.pekislib_a.DotMatrixFontUtils.TextDimensions;
import com.example.pgyl.pekislib_a.DotMatrixSymbol;
import com.example.pgyl.pekislib_a.TimeDateUtils;

import static com.example.pgyl.pekislib_a.DotMatrixFontUtils.getTextDimensions;
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
    private DotMatrixFont defaultFont;
    private DotMatrixFont extraFont;
    private Rect gridRect;
    private Rect gridDisplayRect;
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
        setupDefaultFont();
        setupExtraFont();
        setupIndexes();
        inAutomatic = false;
        automaticOn = false;
        mOnExpiredTimerListener = null;
        calcGridDimensions();
    }

    public void close() {
        runnableTime = null;
        handlerTime = null;
        dotMatrixDisplayView = null;
        defaultFont.close();
        defaultFont = null;
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

    public void setColors(String[] colors) {
        this.colors = colors;
        dotMatrixDisplayView.setBackColor(colors[backColorIndex]);
    }

    public void displayTimeAndLabel(String timeText, String labelText) {
        dotMatrixDisplayView.fillGridSubRect(gridRect, colors[offColorIndex]);
        dotMatrixDisplayView.setSymbolPos(gridDisplayRect.left, gridDisplayRect.top);
        dotMatrixDisplayView.writeText(timeText, colors[onTimeColorIndex], extraFont, defaultFont);   //  Temps avec police extra prioritaire
        dotMatrixDisplayView.writeText(labelText, colors[onLabelColorIndex], defaultFont);   //  Label avec police par défaut
        dotMatrixDisplayView.updateDisplay();
    }

    public void displayTime(String timeText) {
        dotMatrixDisplayView.fillGridSubRect(gridDisplayRect, colors[offColorIndex]);
        dotMatrixDisplayView.setSymbolPos(gridDisplayRect.left, gridDisplayRect.top);
        dotMatrixDisplayView.writeText(timeText, colors[onTimeColorIndex], extraFont, defaultFont);   //  Temps avec police extra prioritaire
    }

    public void refreshDisplay() {
        if (currentCtRecord.isReset()) {
            dotMatrixDisplayView.scrollLeft();
        } else {
            dotMatrixDisplayView.noScroll();
            displayTime(msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS));
        }
        dotMatrixDisplayView.updateDisplay();
    }

    private void setupIndexes() {
        onTimeColorIndex = getDotMatrixDisplayOnTimeIndex();
        onLabelColorIndex = getDotMatrixDisplayOnLabelIndex();
        offColorIndex = getDotMatrixDisplayOffIndex();
        backColorIndex = getDotMatrixDisplayBackIndex();
    }

    private void setupDefaultFont() {
        defaultFont = DotMatrixFontUtils.getDefaultFont();
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
        symbol.setPosInitialOffset(new Point(-defaultFont.getRightMargin(), defaultFont.getMaxSymbolHeight()));
        symbol.setPosFinalOffset(new Point(defaultFont.getRightMargin(), -defaultFont.getMaxSymbolHeight()));
        symbol = null;
    }

    private void calcGridDimensions() {       //  La grille (gridRect) contient le temps et le label, et seule une partie est affichée (gridDisplayRect, glissant en cas de scroll)
        TextDimensions timeTextDimensions = getTextDimensions(msToHms(currentCtRecord.getTimeDisplay(), TimeDateUtils.TIMEUNITS.CS), extraFont, defaultFont);  // timeText mélange de l'extraFont (pour les ":" et ".") et defaultFont (pour les chiffres de 0 à 9)
        TextDimensions labelTextDimensions = getTextDimensions(currentCtRecord.getLabel(), defaultFont);   //  labelText est uniquement affiché en defaultFont

        int gridDisplayRectWidth = timeTextDimensions.width - defaultFont.getRightMargin();   //  La fenêtre d'affichage affiche (sur la largeur du temps sans la dernière marge droite) ...
        int gridDisplayRectHeight = Math.max(timeTextDimensions.height, labelTextDimensions.height);   //  ... soit le temps uniquement, soit (via scroll) le temps et le label , sur la hauteur nécessaire
        int gridRectWidth = timeTextDimensions.width + labelTextDimensions.width;   // La grille doit pouvoir contenir le temps et le label sur toute sa largeur ...
        int gridRectHeight = gridDisplayRectHeight;   //  ... et la même hauteur que la fenêtre d'affichage

        gridRect = new Rect(0, 0, gridRectWidth, gridRectHeight);
        gridDisplayRect = new Rect(0, 0, gridDisplayRectWidth, gridDisplayRectHeight);
        dotMatrixDisplayView.setGridRect(gridRect);  //  La grille est de la taille prévue pour le temps et le label
        dotMatrixDisplayView.setGridDisplayRect(gridDisplayRect);  //  la zone à afficher est de la taille prévue pour le temps
        dotMatrixDisplayView.setGridScrollRect(gridRect);  //  On scrolle la grille entière
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
