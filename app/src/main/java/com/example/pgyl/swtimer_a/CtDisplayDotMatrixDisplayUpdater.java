package com.example.pgyl.swtimer_a;

import android.graphics.Rect;
import android.os.Handler;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.DotMatrixFont;
import com.example.pgyl.pekislib_a.DotMatrixFontUtils;
import com.example.pgyl.pekislib_a.DotMatrixSymbol;

import static com.example.pgyl.pekislib_a.DotMatrixDisplayView.SCROLL_DIRECTIONS;
import static com.example.pgyl.pekislib_a.DotMatrixFontUtils.getFontTextDimensions;
import static com.example.pgyl.pekislib_a.MiscUtils.BiDimensions;
import static com.example.pgyl.pekislib_a.TimeDateUtils.MILLISECONDS_PER_SECOND;
import static com.example.pgyl.pekislib_a.TimeDateUtils.msToTimeFormatD;
import static com.example.pgyl.swtimer_a.Constants.APP_TIME_UNIT_PRECISION;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayColorsBackIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayColorsOffIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayColorsOnLabelIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayColorsOnTimeIndex;

public class CtDisplayDotMatrixDisplayUpdater {
    //region Variables
    private DotMatrixDisplayView dotMatrixDisplayView;
    private DotMatrixFont defaultFont;
    private DotMatrixFont extraFont;
    private Rect margins;
    private Rect gridRect;
    private Rect displayRect;
    private Rect halfDisplayRect;
    private Rect scrollRect;
    private Rect labelRect;
    private String[] colors;
    private int onTimeColorIndex;
    private int onLabelColorIndex;
    private int offColorIndex;
    private int backColorIndex;
    private CtRecord currentCtRecord;
    private int dotsPerSecond;
    private long updateInterval;
    private SCROLL_DIRECTIONS scrollDirection;
    private int scrollCount;
    private boolean automaticScrollOn;
    private boolean inAutomatic;
    private long timeStart;
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
        final long DOTS_PER_SECOND_DEFAULT = 25;       //   25 points par seconde => +/- 4 caractères par secondes  (car un caractère avec marge droite a une largeur de 6 points)

        setupRunnableTime();
        setupDefaultFont();
        setupExtraFont();
        setupColorIndexes();
        setupMargins();
        setupDimensions();
        resetScroll();
        setScrollSpeed(String.valueOf(DOTS_PER_SECOND_DEFAULT));
        inAutomatic = false;
    }

    public void close() {
        stopAutomatic();
        runnableTime = null;
        handlerTime = null;
        dotMatrixDisplayView = null;
        defaultFont.close();
        defaultFont = null;
        extraFont.close();
        extraFont = null;
        currentCtRecord = null;
    }

    public void setColors(String[] colors) {
        this.colors = colors;
        dotMatrixDisplayView.setBackColor(colors[backColorIndex]);
    }

    public void setDotSpacingCoeff(String dotSpacingCoeff) {
        dotMatrixDisplayView.setDotSpacingCoeff(dotSpacingCoeff);
    }

    public void setDotCornerRadiusCoeff(String dotCornerRadiusCoeff) {
        dotMatrixDisplayView.setDotCornerRadiusCoeff(dotCornerRadiusCoeff);
    }

    public void setScrollSpeed(String scrollSpeed) {
        dotsPerSecond = Integer.parseInt(scrollSpeed);
        updateInterval = getUpdateInterval(dotsPerSecond);
    }

    public void resetScroll() {
        scrollCount = 0;
        scrollDirection = SCROLL_DIRECTIONS.LEFT;
        dotMatrixDisplayView.resetScrollOffset();
    }

    public void rebuildStructure() {
        dotMatrixDisplayView.rebuildStructure();
    }   //  A appeler uniquement si MAJ en temps réel

    public void displayCurrentTimeAndLabel() {
        displayTimeAndLabel(msToTimeFormatD(currentCtRecord.getTimeDisplay(), APP_TIME_UNIT_PRECISION), currentCtRecord.getLabel());
    }

    public void displayInitTimeAndLabel() {
        displayTimeAndLabel(msToTimeFormatD(currentCtRecord.getTimeDefInit(), APP_TIME_UNIT_PRECISION), currentCtRecord.getLabel());
    }

    public void displayHalfInitTimeAndInitLabel() {
        displayHalfTimeAndLabel(msToTimeFormatD(currentCtRecord.getTimeDefInit(), APP_TIME_UNIT_PRECISION), currentCtRecord.getLabelInit());
    }

    public void startAutomatic(boolean automaticScrollOn) {
        stopAutomatic();   //  On efface tout et on recommence
        this.automaticScrollOn = automaticScrollOn;
        updateInterval = automaticScrollOn ? getUpdateInterval(dotsPerSecond) : APP_TIME_UNIT_PRECISION.DURATION_MS();   //  Si pas de scroll => Rafraichissement à la fréquence correspondant à la précision du chrono/timer
        timeStart = System.currentTimeMillis();
        handlerTime.postDelayed(runnableTime, updateInterval);   //  Go !
    }

    public void stopAutomatic() {
        handlerTime.removeCallbacks(runnableTime);
    }

    private void automatic() {   // automatic() continue d'être appelé même si dotsPerSecond = 0 (cf getUpdateInterval()) mais ne doit pas scroller
        handlerTime.postDelayed(runnableTime, updateInterval);
        long nowm = System.currentTimeMillis();
        if ((!inAutomatic) && (!dotMatrixDisplayView.isDrawing())) {   //  OK pour rafraîchir l'affichage
            inAutomatic = true;
            currentCtRecord.updateTime(nowm);     //  Mise à jour du temps
            automaticDisplay(nowm);
            timeStart = nowm;   //  Mettre à jour le moment du dernier rafraichissement d'affichage
            inAutomatic = false;
        }
    }

    private void automaticDisplay(long nowm) {
        final int MAX_SCROLL_COUNT = 2 * gridRect.width();   //  Scroll de 2 grilles complètes avant changement de sens

        if (automaticScrollOn) {
            if (dotsPerSecond != 0) {   //  Scroll à effectuer
                int dotsElapsed = (int) ((nowm - timeStart + (updateInterval / 2)) / updateInterval);   //  Arrondir le nombre de points écoulés depuis timeStart
                int scrollDiff = dotsElapsed % MAX_SCROLL_COUNT;
                scrollCount = scrollCount + scrollDiff;
                if (scrollCount > MAX_SCROLL_COUNT) {
                    scrollCount = scrollCount - MAX_SCROLL_COUNT;
                    scrollDiff = 2 * scrollCount - scrollDiff;   //  Poursuivre au max dans le même sens (scrollDiff - scrollCount) et le reste dans l'autre sens (scrollCount) => en net: 2 * scrollCount - scrollDiff, dans l'autre sens
                    scrollDirection = (scrollDirection.equals(SCROLL_DIRECTIONS.LEFT)) ? SCROLL_DIRECTIONS.RIGHT : SCROLL_DIRECTIONS.LEFT;   //  Changer le sens du scroll
                }
                dotMatrixDisplayView.scroll(scrollDirection, scrollDiff);
                dotMatrixDisplayView.updateDisplay();
            }
        } else {
            displayTime(msToTimeFormatD(currentCtRecord.getTimeDisplay(), APP_TIME_UNIT_PRECISION));
        }
    }

    private long getUpdateInterval(int dotsPerSecond) {
        final long UPDATE_INTERVAL_ONGOING = MILLISECONDS_PER_SECOND;   //  Pour que automatic() continue d'être appelé même si dotsPerSecond = 0, mais sans scroll

        return (dotsPerSecond != 0) ? MILLISECONDS_PER_SECOND / dotsPerSecond : UPDATE_INTERVAL_ONGOING;
    }

    private void displayTimeAndLabel(String timeText, String labelText) {
        dotMatrixDisplayView.fillRect(displayRect, colors[onTimeColorIndex], colors[offColorIndex]);    //  Pressed=ON TIME  Unpressed=OFF
        dotMatrixDisplayView.setSymbolPos(displayRect.left + margins.left, displayRect.top + margins.top);
        dotMatrixDisplayView.writeText(timeText, colors[onTimeColorIndex], extraFont, defaultFont);   //  Temps avec police extra prioritaire
        dotMatrixDisplayView.fillRect(labelRect, colors[onLabelColorIndex], colors[offColorIndex]);   //  Pressed=ON LABEL  Unpressed=OFF
        dotMatrixDisplayView.setSymbolPos(labelRect.left, labelRect.top + margins.top);
        dotMatrixDisplayView.writeText(labelText, colors[onLabelColorIndex], defaultFont);   //  Label avec police par défaut
        dotMatrixDisplayView.updateDisplay();
    }

    private void displayTime(String timeText) {
        dotMatrixDisplayView.fillRect(displayRect, colors[onTimeColorIndex], colors[offColorIndex]);   //  Pressed=ON TIME  Unpressed=OFF
        dotMatrixDisplayView.setSymbolPos(displayRect.left + margins.left, displayRect.top + margins.top);
        dotMatrixDisplayView.writeText(timeText, colors[onTimeColorIndex], extraFont, defaultFont);   //  Temps avec police extra prioritaire
        dotMatrixDisplayView.updateDisplay();
    }

    private void displayHalfTimeAndLabel(String timeText, String labelText) {   //  Partager l'affichage entre Temps et Label (utilisé pour le réglage des couleurs dans CtDisplayColorsActivity)
        dotMatrixDisplayView.fillRect(displayRect, colors[onTimeColorIndex], colors[offColorIndex]);   //  Pressed=ON TIME  Unpressed=OFF
        dotMatrixDisplayView.setSymbolPos(displayRect.left + margins.left, displayRect.top + margins.top);
        dotMatrixDisplayView.writeText(timeText, colors[onTimeColorIndex], extraFont, defaultFont);   //  Temps avec police extra prioritaire
        dotMatrixDisplayView.fillRect(halfDisplayRect, colors[onLabelColorIndex], colors[offColorIndex]);   //  Effacer la 2e moitié du temps    Pressed=ON LABEL  Unpressed=OFF
        dotMatrixDisplayView.setSymbolPos(halfDisplayRect.left, halfDisplayRect.top + margins.top);
        dotMatrixDisplayView.writeText(labelText, colors[onLabelColorIndex], defaultFont);   //  Label avec police par défaut
        dotMatrixDisplayView.updateDisplay();
    }

    private void setupColorIndexes() {
        onTimeColorIndex = getDotMatrixDisplayColorsOnTimeIndex();
        onLabelColorIndex = getDotMatrixDisplayColorsOnLabelIndex();
        offColorIndex = getDotMatrixDisplayColorsOffIndex();
        backColorIndex = getDotMatrixDisplayColorsBackIndex();
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
        symbol.setPosOffset(-symbol.getDimensions().width, defaultFont.getDimensions().height);
        symbol = null;
    }

    private void setupMargins() {    // Marges autour de l'affichage proprement dit
        final int MARGIN_LEFT = 1;
        final int MARGIN_RIGHT = 1;
        final int MARGIN_TOP = 1;

        int marginBottom = extraFont.getSymbol('.').getDimensions().height;  // Le  "." est affiché dans la marge inférieure
        margins = new Rect(MARGIN_LEFT, MARGIN_TOP, MARGIN_RIGHT, marginBottom);
    }

    private void setupDimensions() {       //  La grille (gridRect) contient le temps et le label, et seule une partie est affichée (gridDisplayRect, glissant en cas de scroll)
        BiDimensions timeTextDimensions = getFontTextDimensions(msToTimeFormatD(currentCtRecord.getTimeDisplay(), APP_TIME_UNIT_PRECISION), extraFont, defaultFont);  // timeText mélange de l'extraFont (pour les ":" et ".") et defaultFont (pour les chiffres de 0 à 9)
        BiDimensions labelTextDimensions = getFontTextDimensions(currentCtRecord.getLabel(), defaultFont);   //  labelText est uniquement affiché en defaultFont

        int displayRectWidth = margins.left + timeTextDimensions.width - defaultFont.getRightMargin() + margins.right;   //   Affichage sur la largeur du temps, avec margins.right remplaçant la dernière marge droite)
        int displayRectHeight = margins.top + Math.max(timeTextDimensions.height, labelTextDimensions.height) + margins.bottom;   //  Affichage du temps uniquement ou (via scroll) du temps et du label , sur la hauteur nécessaire
        int gridRectWidth = displayRectWidth + labelTextDimensions.width - defaultFont.getRightMargin();   // La grille doit pouvoir contenir le temps et le label sur toute sa largeur ...
        int gridRectHeight = displayRectHeight;   //  ... et la même hauteur que la fenêtre d'affichage

        gridRect = new Rect(0, 0, gridRectWidth, gridRectHeight);
        displayRect = new Rect(gridRect.left, gridRect.top, displayRectWidth, displayRectHeight);  //  Affichage au début de la grille
        halfDisplayRect = new Rect(displayRect.right / 2, displayRect.top, displayRect.right, displayRect.bottom);  //  Pour affichage partagé dans CtDisplayColorsActivity
        labelRect = new Rect(displayRect.right, gridRect.top, gridRect.right, gridRect.bottom);   //  Espace restant de la grille
        scrollRect = new Rect(gridRect);   //  On scrolle la grille entière (margins.left servira de margins.right)

        dotMatrixDisplayView.setGridRect(gridRect);
        dotMatrixDisplayView.setDisplayRect(displayRect);
        dotMatrixDisplayView.setScrollRect(scrollRect);
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