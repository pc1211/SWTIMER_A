package com.example.pgyl.swtimer_a;

import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pgyl.pekislib_a.DefaultDotMatrixFont;
import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.DotMatrixFont;

import static com.example.pgyl.pekislib_a.DotMatrixFontUtils.getFontTextDimensions;
import static com.example.pgyl.pekislib_a.MiscUtils.BiDimensions;
import static com.example.pgyl.pekislib_a.PointRectUtils.ALIGN_LEFT_HEIGHT;

public class MainDotMatrixDisplayUpdater {

    private enum MESSAGES {
        EMPTY_LIST("List empty"), EMPTY_SELECTION("Blue symbol to select item");
        private String text;

        MESSAGES(String text) {
            this.text = text;
        }

        public String TEXT() {
            return text;
        }
    }

    //region Variables
    private DotMatrixDisplayView dotMatrixDisplayView;
    private DotMatrixFont defaultFont;
    private Rect margins;
    private Rect gridRect;
    private Rect displayRect;
    //endregion

    public MainDotMatrixDisplayUpdater(DotMatrixDisplayView dotMatrixDisplayView) {
        super();

        this.dotMatrixDisplayView = dotMatrixDisplayView;
        init();
    }

    private void init() {
        setupDefaultFont();
        setupBackColor();
        setupMargins();
        setupDimensions();
    }

    public void close() {
        dotMatrixDisplayView = null;
        defaultFont.close();
        defaultFont = null;
    }

    public void displayEmptySelection() {
        displayText(MESSAGES.EMPTY_SELECTION.TEXT());
    }

    public void displayEmptyList() {
        displayText(MESSAGES.EMPTY_LIST.TEXT());
    }

    private void displayText(String text) {
        final String ON_COLOR = "707070";
        final String OFF_COLOR = "404040";

        dotMatrixDisplayView.fillRect(displayRect, ON_COLOR, OFF_COLOR);    //  Pressed=ON  Unpressed=OFF
        dotMatrixDisplayView.setSymbolPos(displayRect.left + margins.left, displayRect.top + margins.top);
        dotMatrixDisplayView.writeText(text, ON_COLOR, defaultFont);
        dotMatrixDisplayView.updateDisplay();
    }

    private void setupBackColor() {
        final String BACK_COLOR = "000000";

        dotMatrixDisplayView.setBackColor(BACK_COLOR);
    }

    private void setupDefaultFont() {
        defaultFont = new DefaultDotMatrixFont();
    }

    private void setupMargins() {    // Marges (en nombre de carrés autour de l'affichage proprement dit)
        margins = new Rect(1, 1, 1, 1);
    }

    private void setupDimensions() {
        final RectF INTERNAL_MARGIN_SIZE_COEFFS = new RectF(0.02f, 0, 0.02f, 0);   //  Marge autour de l'affichage proprement dit (% de largeur)

        String maxText = "";
        for (MESSAGES dotMatrixDisplayMessage : MESSAGES.values()) {
            if (dotMatrixDisplayMessage.TEXT().length() > maxText.length()) {
                maxText = dotMatrixDisplayMessage.TEXT();
            }
        }
        BiDimensions textDimensions = getFontTextDimensions(maxText, defaultFont);

        int displayRectWidth = margins.left + textDimensions.width - defaultFont.getRightMargin() + margins.right;   //   margins.right remplace la dernière marge droite
        int displayRectHeight = margins.top + textDimensions.height + margins.bottom;
        int gridRectWidth = displayRectWidth;
        int gridRectHeight = displayRectHeight;

        gridRect = new Rect(0, 0, gridRectWidth, gridRectHeight);
        displayRect = new Rect(gridRect.left, gridRect.top, displayRectWidth, displayRectHeight);  //  Affichage au début de la grille

        dotMatrixDisplayView.setInternalMarginCoeffs(INTERNAL_MARGIN_SIZE_COEFFS);
        dotMatrixDisplayView.setExternalMarginCoeffs(ALIGN_LEFT_HEIGHT);
        dotMatrixDisplayView.setGridRect(gridRect);
        dotMatrixDisplayView.setDisplayRect(displayRect);
    }

}