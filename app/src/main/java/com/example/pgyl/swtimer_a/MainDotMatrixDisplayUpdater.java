package com.example.pgyl.swtimer_a;

import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pgyl.pekislib_a.ColorBox;
import com.example.pgyl.pekislib_a.ColorUtils;
import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.DotMatrixFont;
import com.example.pgyl.pekislib_a.DotMatrixFontDefault;

import static com.example.pgyl.pekislib_a.ColorUtils.DOT_MATRIX_COLOR_TYPES;
import static com.example.pgyl.pekislib_a.DotMatrixFontUtils.getFontTextDimensions;
import static com.example.pgyl.pekislib_a.MiscUtils.BiDimensions;
import static com.example.pgyl.pekislib_a.PointRectUtils.ALIGN_LEFT_HEIGHT;

public class MainDotMatrixDisplayUpdater {

    private enum MESSAGES {
        EMPTY_LIST("List empty"), EMPTY_SELECTION("No items selected      ");
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
    private ColorBox colorBox;
    private DotMatrixFont defaultFont;
    private Rect margins;
    private Rect displayRect;
    //endregion

    public MainDotMatrixDisplayUpdater(DotMatrixDisplayView dotMatrixDisplayView) {
        super();

        this.dotMatrixDisplayView = dotMatrixDisplayView;
        init();
    }

    private void init() {
        final String BACK_COLOR = "000000";

        colorBox = dotMatrixDisplayView.getColorBox();
        colorBox.setColor(ColorUtils.DOT_MATRIX_COLOR_TYPES.BACK_SCREEN.INDEX(), BACK_COLOR);
        setupDefaultFont();
        setupMargins();
        setupDimensions();
    }

    public void close() {
        colorBox.close();
        colorBox = null;
        defaultFont.close();
        defaultFont = null;
    }

    public void displayEmptySelection() {
        displayText(MESSAGES.EMPTY_SELECTION.TEXT());
        dotMatrixDisplayView.updateDisplay();
    }

    public void displayEmptyList() {
        displayText(MESSAGES.EMPTY_LIST.TEXT());
        dotMatrixDisplayView.updateDisplay();
    }

    private void displayText(String text) {
        final String ON_COLOR = "707070";
        final String OFF_COLOR = "404040";

        colorBox.setColor(DOT_MATRIX_COLOR_TYPES.UNPRESSED_BACK.INDEX(), OFF_COLOR);
        colorBox.setColor(DOT_MATRIX_COLOR_TYPES.PRESSED_BACK.INDEX(), ON_COLOR);
        dotMatrixDisplayView.drawBackRect(displayRect);

        colorBox.setColor(DOT_MATRIX_COLOR_TYPES.UNPRESSED_FRONT.INDEX(), ON_COLOR);
        colorBox.setColor(DOT_MATRIX_COLOR_TYPES.PRESSED_FRONT.INDEX(), OFF_COLOR);
        dotMatrixDisplayView.setSymbolPos(displayRect.left + margins.left, displayRect.top + margins.top);
        dotMatrixDisplayView.drawFrontText(text, null, defaultFont);
    }

    private void setupDefaultFont() {
        defaultFont = new DotMatrixFontDefault();
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
        BiDimensions textDimensions = getFontTextDimensions(maxText, null, defaultFont);

        int displayRectWidth = margins.left + textDimensions.width - defaultFont.getRightMargin() + margins.right;   //   margins.right remplace la dernière marge droite
        int displayRectHeight = margins.top + textDimensions.height + margins.bottom;

        Rect gridRect = new Rect(0, 0, displayRectWidth, displayRectHeight);
        displayRect = new Rect(gridRect.left, gridRect.top, displayRectWidth, displayRectHeight);  //  Affichage au début de la grille

        dotMatrixDisplayView.setInternalMarginCoeffs(INTERNAL_MARGIN_SIZE_COEFFS);
        dotMatrixDisplayView.setExternalMarginCoeffs(ALIGN_LEFT_HEIGHT);
        dotMatrixDisplayView.setGridRect(gridRect);
        dotMatrixDisplayView.setDisplayRect(displayRect);
    }

}