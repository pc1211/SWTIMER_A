package com.example.pgyl.swtimer_a;

import android.graphics.Rect;
import android.os.Handler;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.DotMatrixFont;
import com.example.pgyl.pekislib_a.DotMatrixFontUtils;
import com.example.pgyl.pekislib_a.DotMatrixSymbol;
import com.example.pgyl.pekislib_a.PointRectUtils.RectDimensions;

import static com.example.pgyl.pekislib_a.DotMatrixDisplayView.SCROLL_DIRECTIONS;
import static com.example.pgyl.pekislib_a.DotMatrixFontUtils.getFontRectDimensions;
import static com.example.pgyl.pekislib_a.TimeDateUtils.msToTimeFormatD;
import static com.example.pgyl.swtimer_a.Constants.TIME_UNIT_PRECISION;
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
    private Rect gridHalfDisplayRect;
    private Rect gridLabelRect;
    private SCROLL_DIRECTIONS scrollDirection;
    private int scrollCount;
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

    public void setColors(String[] colors) {
        this.colors = colors;
        dotMatrixDisplayView.setBackColor(colors[backColorIndex]);
    }

    public void displayTimeAndLabel(String timeText, String labelText) {
        dotMatrixDisplayView.fillRect(gridDisplayRect, colors[onTimeColorIndex], colors[offColorIndex]);
        dotMatrixDisplayView.setSymbolPos(gridDisplayRect.left, gridDisplayRect.top);
        dotMatrixDisplayView.writeText(timeText, colors[onTimeColorIndex], extraFont, defaultFont);   //  Temps avec police extra prioritaire
        dotMatrixDisplayView.fillRect(gridLabelRect, colors[onLabelColorIndex], colors[offColorIndex]);
        dotMatrixDisplayView.writeText(labelText, colors[onLabelColorIndex], defaultFont);   //  Label avec police par défaut
        dotMatrixDisplayView.updateDisplay();
    }

    public void displayTime(String timeText) {
        dotMatrixDisplayView.fillRect(gridDisplayRect, colors[onTimeColorIndex], colors[offColorIndex]);
        dotMatrixDisplayView.setSymbolPos(gridDisplayRect.left, gridDisplayRect.top);
        dotMatrixDisplayView.writeText(timeText, colors[onTimeColorIndex], extraFont, defaultFont);   //  Temps avec police extra prioritaire
        dotMatrixDisplayView.updateDisplay();
    }

    public void displayHalfTimeAndLabel(String timeText, String labelText) {   //  Partager l'affichage entre Temps et Label (utilisé pour le réglage des couleurs dans CtDisplayColorsActivity)
        dotMatrixDisplayView.fillRect(gridDisplayRect, colors[onTimeColorIndex], colors[offColorIndex]);
        dotMatrixDisplayView.setSymbolPos(gridDisplayRect.left, gridDisplayRect.top);
        dotMatrixDisplayView.writeText(timeText, colors[onTimeColorIndex], extraFont, defaultFont);   //  Temps avec police extra prioritaire
        dotMatrixDisplayView.fillRect(gridHalfDisplayRect, colors[onLabelColorIndex], colors[offColorIndex]);   //  Effacer la 2e moitié du temps
        dotMatrixDisplayView.setSymbolPos(gridHalfDisplayRect.left, gridDisplayRect.top);
        dotMatrixDisplayView.writeText(labelText, colors[onLabelColorIndex], defaultFont);   //  Label avec police par défaut
        dotMatrixDisplayView.updateDisplay();
    }

    public void startAutomatic() {
        scrollCount = 0;
        scrollDirection = SCROLL_DIRECTIONS.LEFT;
        automaticOn = true;
        handlerTime.postDelayed(runnableTime, updateInterval);
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
            automaticDisplay();
            inAutomatic = false;
        }
    }

    private void automaticDisplay() {
        if (currentCtRecord.isReset()) {
            if (scrollCount == 2 * gridRect.width()) {  //  Changer le sens du scroll après 2 grilles complètes
                scrollCount = 0;
                scrollDirection = (scrollDirection.equals(SCROLL_DIRECTIONS.LEFT)) ? SCROLL_DIRECTIONS.RIGHT : SCROLL_DIRECTIONS.LEFT;
            }
            dotMatrixDisplayView.scroll(scrollDirection);
            scrollCount = scrollCount + 1;
            dotMatrixDisplayView.updateDisplay();
        } else {
            dotMatrixDisplayView.noScroll();
            displayTime(msToTimeFormatD(currentCtRecord.getTimeDisplay(), TIME_UNIT_PRECISION));
        }
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
        symbol = extraFont.getSymbol('.');
        symbol.setOverwrite(true);   //  Le "." surcharge le symbole précédent (en-dessous dans sa marge droite)
        symbol.setPosOffset(-symbol.getDimensions().width, defaultFont.getSymbolDimensions().height);
        symbol = null;
    }

    private void calcGridDimensions() {       //  La grille (gridRect) contient le temps et le label, et seule une partie est affichée (gridDisplayRect, glissant en cas de scroll)
        RectDimensions timeTextDimensions = getFontRectDimensions(msToTimeFormatD(currentCtRecord.getTimeDisplay(), TIME_UNIT_PRECISION), extraFont, defaultFont);  // timeText mélange de l'extraFont (pour les ":" et ".") et defaultFont (pour les chiffres de 0 à 9)
        RectDimensions labelTextDimensions = getFontRectDimensions(currentCtRecord.getLabel(), defaultFont);   //  labelText est uniquement affiché en defaultFont

        int gridDisplayRectWidth = timeTextDimensions.width - defaultFont.getRightMargin();   //  La fenêtre d'affichage affiche (sur la largeur du temps sans la dernière marge droite) ...
        int gridDisplayRectHeight = Math.max(timeTextDimensions.height, labelTextDimensions.height) + extraFont.getSymbol('.').getDimensions().height;   //  ... soit le temps uniquement, soit (via scroll) le temps et le label , sur la hauteur nécessaire (en ajoutant la hauteur du "." affiché en-dessous)
        int gridRectWidth = timeTextDimensions.width + labelTextDimensions.width;   // La grille doit pouvoir contenir le temps et le label sur toute sa largeur ...
        int gridRectHeight = gridDisplayRectHeight;   //  ... et la même hauteur que la fenêtre d'affichage

        gridRect = new Rect(0, 0, gridRectWidth, gridRectHeight);
        gridDisplayRect = new Rect(0, 0, gridDisplayRectWidth, gridDisplayRectHeight);
        gridHalfDisplayRect = new Rect(gridDisplayRect.right / 2, gridDisplayRect.top, gridDisplayRect.right, gridDisplayRect.bottom);
        gridLabelRect = new Rect(gridDisplayRect.right, gridRect.top, gridRect.right, gridRect.bottom);
        dotMatrixDisplayView.setGridRect(gridRect);
        dotMatrixDisplayView.setGridDisplayRect(gridDisplayRect);
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