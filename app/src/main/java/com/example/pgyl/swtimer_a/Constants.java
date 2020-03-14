package com.example.pgyl.swtimer_a;

import com.example.pgyl.pekislib_a.TimeDateUtils.TIMEUNITS;

public class Constants {
    //region Constantes
    public enum SWTIMER_ACTIVITIES {
        MAIN, CT_DISPLAY, CT_DISPLAY_COLORS;

        public int INDEX() {
            return ordinal();
        }
    }

    public static final TIMEUNITS TIME_UNIT_PRECISION = TIMEUNITS.TS;  //  Précision souhaitée dans l'affichage du temps
    public static final int SWTIMER_ACTIVITIES_REQUEST_CODE_MULTIPLIER = 100;

}
