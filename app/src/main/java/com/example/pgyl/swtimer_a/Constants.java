package com.example.pgyl.swtimer_a;

public class Constants {
    //region Constantes
    public enum SWTIMER_ACTIVITIES {
        MAIN, CT_DISPLAY, CT_DISPLAY_COLORS;

        public int INDEX() {
            return ordinal();
        }
    }

    public static final int SWTIMER_ACTIVITIES_REQUEST_CODE_MULTIPLIER = 100;

}
