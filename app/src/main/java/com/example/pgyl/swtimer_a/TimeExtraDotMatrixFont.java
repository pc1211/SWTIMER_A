package com.example.pgyl.swtimer_a;

import com.example.pgyl.pekislib_a.DotMatrixFont;
import com.example.pgyl.pekislib_a.DotMatrixSymbol;

public class TimeExtraDotMatrixFont extends DotMatrixFont {

    private DotMatrixFont defaultFont;

    public TimeExtraDotMatrixFont(DotMatrixFont defaultFont) {
        super();

        this.defaultFont = defaultFont;
        init();
    }

    private void init() {
        //  Caractères redéfinis pour l'affichage du temps ("." et ":") (plus fins que la fonte par défaut de DotMatrixDisplayView)
        final DotMatrixSymbol[] EXTRA_FONT_SYMBOLS = {
                new DotMatrixSymbol('.', new int[][]{{1}}),
                new DotMatrixSymbol(':', new int[][]{{0}, {0}, {1}, {0}, {1}, {0}, {0}})
        };

        final int EXTRA_FONT_RIGHT_MARGIN = 1;

        setSymbols(EXTRA_FONT_SYMBOLS);
        setRightMargin(EXTRA_FONT_RIGHT_MARGIN);
        DotMatrixSymbol symbol = getSymbol('.');
        symbol.setOverwrite(true);   //  Le "." surcharge le symbole précédent (en-dessous dans sa marge droite)
        symbol.setPosOffset(-symbol.getDimensions().width, defaultFont.getDimensions().height);
        symbol = null;
    }

}
