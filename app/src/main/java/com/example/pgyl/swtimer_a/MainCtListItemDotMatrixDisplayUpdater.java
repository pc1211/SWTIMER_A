package com.example.pgyl.swtimer_a;

import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pgyl.pekislib_a.DefaultDotMatrixFont;
import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.DotMatrixFont;

import static com.example.pgyl.pekislib_a.DotMatrixFontUtils.getFontTextDimensions;
import static com.example.pgyl.pekislib_a.MiscUtils.BiDimensions;
import static com.example.pgyl.pekislib_a.PointRectUtils.ALIGN_LEFT_HEIGHT;
import static com.example.pgyl.pekislib_a.TimeDateUtils.HHmmss;
import static com.example.pgyl.pekislib_a.TimeDateUtils.ROUND_TO_TIME_UNIT_PRECISION;
import static com.example.pgyl.pekislib_a.TimeDateUtils.TIME_UNITS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.getFormattedTimeZoneLongTimeDate;
import static com.example.pgyl.pekislib_a.TimeDateUtils.msToTimeFormatD;
import static com.example.pgyl.swtimer_a.Constants.APP_TIME_UNIT_PRECISION;

public class MainCtListItemDotMatrixDisplayUpdater {
    //region Variables
    private DotMatrixFont defaultFont;
    private DotMatrixFont extraFont;
    private Rect margins;
    private Rect gridRect;
    private Rect displayRect;
    private Rect timeDisplayRect;
    private Rect labelDisplayRect;
    //endregion

    //region Constantes
    final int LABEL_MARGIN_TOP = 1;  //  Marge avant la 2e ligne
    final String FILLER_LABEL = "ABCDEFGHIJ";   //  Prévoir la place pour un label de taille maximale
    final long FILLER_TIME_MS = 0;   //  correspondant à 00:00:00.0 si TS, 00:00:00 si SEC
    //endregion

    public MainCtListItemDotMatrixDisplayUpdater() {    //  Général pour tous les MainCtListItems
        super();

        init();
    }

    private void init() {
        setupDefaultFont();
        setupExtraFont();
        setupMargins();
    }

    public void close() {
        defaultFont.close();
        defaultFont = null;
        extraFont.close();
        extraFont = null;
    }

    public void displayTimeAndLabel(DotMatrixDisplayView dotMatrixDisplayView, CtRecord ctRecord, boolean showExpirationTime, long nowm) {
        final String TIME_ON_COLOR = "FFFF00";   //  Couleur de HH:MM:SS
        final String TIME_EXP_ON_COLOR = "FF0000";    //  Couleur si Temps d'expiration (si timer)
        final String LABEL_ON_COLOR = "C0C0C0";
        final String OFF_COLOR = "404040";
        String timeText;
        String color;

        dotMatrixDisplayView.fillRect(displayRect, TIME_ON_COLOR, OFF_COLOR);    //  Pressed=ON  Unpressed=OFF
        dotMatrixDisplayView.setSymbolPos(timeDisplayRect.left + margins.left, timeDisplayRect.top + margins.top);
        if ((showExpirationTime) && (ctRecord.getMode().equals(CtRecord.MODES.TIMER))) {   //  Afficher heure d'expiration du timer HH:MM:SS
            timeText = getFormattedTimeZoneLongTimeDate(ctRecord.getTimeExp(), HHmmss);
            color = TIME_EXP_ON_COLOR;
        } else {  //  Affichage normal
            TIME_UNITS timeUnit = ((ctRecord.isRunning()) && (!ctRecord.isSplitted())) ? TIME_UNITS.SEC : APP_TIME_UNIT_PRECISION;
            timeText = msToTimeFormatD(ctRecord.getTimeDisplay(nowm), timeUnit, ROUND_TO_TIME_UNIT_PRECISION);   //  HH:MM:SS ou HH:MM:SS.T
            color = TIME_ON_COLOR;
        }
        dotMatrixDisplayView.writeText(timeText, color, extraFont, defaultFont);   //  Temps avec police extra prioritaire
        dotMatrixDisplayView.setSymbolPos(labelDisplayRect.left + margins.left, labelDisplayRect.top + LABEL_MARGIN_TOP);
        String label = ctRecord.getLabel();
        dotMatrixDisplayView.writeText(label.substring(0, Math.min(label.length(), FILLER_LABEL.length())), LABEL_ON_COLOR, defaultFont);   //  Label avec police par défaut
        dotMatrixDisplayView.updateDisplay();
    }

    public void setupDimensions(DotMatrixDisplayView dotMatrixDisplayView) {       //  La grille (gridRect) contient le temps (1e ligne) et le label (2e ligne)
        final RectF INTERNAL_MARGIN_SIZE_COEFFS = new RectF(0, 0, 0, 0);   //  Marge autour de l'affichage proprement dit (% de largeur)
        int displayRectWidth;
        int displayRectHeight;

        BiDimensions fillerTimeTextDimensions = getFontTextDimensions(msToTimeFormatD(FILLER_TIME_MS, APP_TIME_UNIT_PRECISION, ROUND_TO_TIME_UNIT_PRECISION), extraFont, defaultFont);  // timeText mélange de l'extraFont (pour les ":" et ".") et defaultFont (pour les chiffres de 0 à 9)
        BiDimensions fillerLabelTextDimensions = getFontTextDimensions(FILLER_LABEL, defaultFont);   //  labelText est uniquement affiché en defaultFont

        displayRectWidth = margins.left + fillerLabelTextDimensions.width - defaultFont.getRightMargin() + margins.right;   //   Affichage sur la largeur du label maximum; margins.right remplace la dernière marge droite
        displayRectHeight = margins.top + fillerTimeTextDimensions.height + extraFont.getSymbol('.').getDimensions().height + LABEL_MARGIN_TOP + fillerLabelTextDimensions.height + margins.bottom;
        int gridRectWidth = displayRectWidth;
        int gridRectHeight = displayRectHeight;

        gridRect = new Rect(0, 0, gridRectWidth, gridRectHeight);
        displayRect = new Rect(gridRect.left, gridRect.top, displayRectWidth, displayRectHeight);  //  Affichage au début de la grille
        timeDisplayRect = new Rect(displayRect.left, displayRect.top, displayRect.right, margins.top + fillerTimeTextDimensions.height + extraFont.getSymbol('.').getDimensions().height);  //  Affichage sur la 1e ligne
        labelDisplayRect = new Rect(displayRect.left, timeDisplayRect.bottom, displayRect.right, displayRect.bottom);  //  Affichage sur la 2e ligne

        dotMatrixDisplayView.setInternalMarginCoeffs(INTERNAL_MARGIN_SIZE_COEFFS);
        dotMatrixDisplayView.setExternalMarginCoeffs(ALIGN_LEFT_HEIGHT);
        dotMatrixDisplayView.setGridRect(gridRect);
        dotMatrixDisplayView.setDisplayRect(displayRect);
    }

    public void setupBackColor(DotMatrixDisplayView dotMatrixDisplayView) {
        final String BACK_COLOR = "000000";

        dotMatrixDisplayView.setBackColor(BACK_COLOR);
    }

    private void setupDefaultFont() {
        defaultFont = new DefaultDotMatrixFont();
    }

    private void setupExtraFont() {
        extraFont = new TimeExtraDotMatrixFont(defaultFont);
    }

    private void setupMargins() {    // Marges autour de l'affichage proprement dit
        final int MARGIN_LEFT = 1;
        final int MARGIN_RIGHT = 1;
        final int MARGIN_TOP = 1;
        final int MARGIN_BOTTOM = 1;

        margins = new Rect(MARGIN_LEFT, MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM);
    }

}