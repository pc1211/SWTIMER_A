package com.example.pgyl.swtimer_a;

import static com.example.pgyl.swtimer_a.Constants.SWTIMER_TABLES.COLORS_BACK_SCREEN;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_TABLES.COLORS_TIMEBUTTONS;

public class Constants {
    //region Constantes
    public enum SWTIMER_ACTIVITIES {
        MAIN, CTDISPLAY
    }

    public enum SWTIMER_SHP_KEY_NAMES {SHOW_EXPIRATION_TIME, ADD_NEW_CHRONOTIMER_TO_LIST, KEEP_SCREEN, REQUESTED_CLOCK_APP_ALARMS}

    public enum SWTIMER_TABLES {CHRONO_TIMERS, PRESETS_CT, COLORS_TIMEBUTTONS, COLORS_BACK_SCREEN}

    public enum TABLE_CHRONO_TIMERS_DATA_FIELDS {
        MODE(1), SELECTED(2), RUNNING(3), SPLITTED(4), ALARM_SET(5), MESSAGE(6), MESSAGE_INIT(7), TIME_START(8), TIME_ACC(9), TIME_ACC_UNTIL_SPLIT(10), TIME_DEF(11), TIME_DEF_INIT(12), TIME_EXP(13);

        private int valueIndex;

        TABLE_CHRONO_TIMERS_DATA_FIELDS(int valueIndex) {
            this.valueIndex = valueIndex;
        }

        public int INDEX() {
            return valueIndex;
        }
    }

    public enum TABLE_PRESETS_CT_DATA_FIELDS {
        TIME("Time", 1), MESSAGE("Message", 2);

        private int valueIndex;
        private String valueLabel;

        TABLE_PRESETS_CT_DATA_FIELDS(String valueLabel, int valueIndex) {
            this.valueIndex = valueIndex;
            this.valueLabel = valueLabel;
        }

        public int INDEX() {
            return valueIndex;
        }

        public String LABEL() {
            return valueLabel;
        }
    }

    public enum TABLE_COLORS_TIMEBUTTONS_DATA_FIELDS {
        ON("Light On", 1), OFF("Light Off", 2), BACK("Background", 3);

        private int valueIndex;
        private String valueLabel;

        TABLE_COLORS_TIMEBUTTONS_DATA_FIELDS(String valueLabel, int valueIndex) {
            this.valueIndex = valueIndex;
            this.valueLabel = valueLabel;
        }

        public int INDEX() {
            return valueIndex;
        }

        public String LABEL() {
            return valueLabel;
        }
    }

    public enum TABLE_COLORS_BACK_SCREEN_DATA_FIELDS {
        BACK("Background", 1);

        private int valueIndex;
        private String valueLabel;

        TABLE_COLORS_BACK_SCREEN_DATA_FIELDS(String valueLabel, int valueIndex) {
            this.valueIndex = valueIndex;
            this.valueLabel = valueLabel;
        }

        public int INDEX() {
            return valueIndex;
        }

        public String LABEL() {
            return valueLabel;
        }
    }

    public enum COLOR_ITEMS {
        TIME(COLORS_TIMEBUTTONS), BUTTONS(COLORS_TIMEBUTTONS), BACK_SCREEN(COLORS_BACK_SCREEN);

        private SWTIMER_TABLES valueColorTable;

        COLOR_ITEMS(SWTIMER_TABLES valueColorTable) {
            this.valueColorTable = valueColorTable;
        }

        public String TABLE_NAME() {
            return valueColorTable.toString();
        }
    }

}
