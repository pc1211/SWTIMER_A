package com.example.pgyl.swtimer_a;

import android.graphics.Rect;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.DotMatrixFont;
import com.example.pgyl.pekislib_a.DotMatrixFontUtils;

import java.util.Arrays;

import static com.example.pgyl.pekislib_a.DotMatrixFontUtils.getFontTextDimensions;
import static com.example.pgyl.pekislib_a.MiscUtils.BiDimensions;

public class MainDotMatrixDisplayUpdater {
    final String ON_COLOR = "FF9A22";
    final String OFF_COLOR = "404040";
    final String BACK_COLOR = "000000";

    //region Variables
    private DotMatrixDisplayView dotMatrixDisplayView;
    private int maxTextLength;
    private DotMatrixFont defaultFont;
    private Rect margins;
    private Rect gridRect;
    private Rect displayRect;
    //endregion

    public MainDotMatrixDisplayUpdater(DotMatrixDisplayView dotMatrixDisplayView, int maxTextLenght) {
        super();

        this.dotMatrixDisplayView = dotMatrixDisplayView;
        this.maxTextLength = maxTextLenght;
        init();
    }

    private void init() {
        setupDefaultFont();
        setupBackColor();
        setupMargins();
        setupDimensions(maxTextLength);
    }

    public void close() {
        dotMatrixDisplayView = null;
        defaultFont.close();
        defaultFont = null;
    }

    public void displayText(String text) {
        dotMatrixDisplayView.fillRect(displayRect, ON_COLOR, OFF_COLOR);    //  Pressed=ON  Unpressed=OFF
        dotMatrixDisplayView.setSymbolPos(displayRect.left + margins.left, displayRect.top + margins.top);
        dotMatrixDisplayView.writeText(text, ON_COLOR, defaultFont);
        dotMatrixDisplayView.updateDisplay();
    }

    private void setupBackColor() {
        dotMatrixDisplayView.setBackColor(BACK_COLOR);
    }

    private void setupDefaultFont() {
        defaultFont = DotMatrixFontUtils.getDefaultFont();
    }

    private void setupMargins() {    // Marges autour de l'affichage proprement dit
        final int MARGIN_LEFT = 1;
        final int MARGIN_RIGHT = 1;
        final int MARGIN_TOP = 1;
        final int MARGIN_BOTTOM = 1;

        margins = new Rect(MARGIN_LEFT, MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM);
    }

    private void setupDimensions(int maxTextLength) {
        char[] chars = new char[maxTextLength];
        Arrays.fill(chars, '*');
        String maxText = new String(chars);
        BiDimensions textDimensions = getFontTextDimensions(maxText, defaultFont);

        int displayRectWidth = margins.left + textDimensions.width - defaultFont.getRightMargin() + margins.right;   //   margins.right remplace la dernière marge droite
        int displayRectHeight = margins.top + textDimensions.height + margins.bottom;
        int gridRectWidth = displayRectWidth;
        int gridRectHeight = displayRectHeight;

        gridRect = new Rect(0, 0, gridRectWidth, gridRectHeight);
        displayRect = new Rect(gridRect.left, gridRect.top, displayRectWidth, displayRectHeight);  //  Affichage au début de la grille

        dotMatrixDisplayView.setGridRect(gridRect);
        dotMatrixDisplayView.setDisplayRect(displayRect);
        dotMatrixDisplayView.setScrollRect(gridRect);   //  Non utilisé (pas de scroll)
    }

}