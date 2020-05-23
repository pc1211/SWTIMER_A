package com.example.pgyl.swtimer_a;

import static com.example.pgyl.pekislib_a.Constants.CRLF;
import static com.example.pgyl.pekislib_a.TimeDateUtils.HHmm;
import static com.example.pgyl.pekislib_a.TimeDateUtils.TIME_UNITS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.getFormattedTimeZoneLongTimeDate;
import static com.example.pgyl.pekislib_a.TimeDateUtils.getMidnightTimeMillis;

class CtRecord {   //  Données d'un Chrono ou Timer
    public interface onExpiredTimerListener {
        void onExpiredTimer(CtRecord ctRecord);
    }

    public void setOnExpiredTimerListener(onExpiredTimerListener listener) {
        mOnExpiredTimerListener = listener;
    }

    private onExpiredTimerListener mOnExpiredTimerListener;

    public interface onRequestClockAppAlarmSwitchListener {
        void onRequestClockAppAlarmSwitch(CtRecord ctRecord, CLOCK_APP_ALARM_SWITCHES clockAppAlarmSwitch);
    }

    public void setOnRequestClockAppAlarmSwitchListener(onRequestClockAppAlarmSwitchListener listener) {
        mOnRequestClockAppAlarmSwitchListener = listener;
    }

    private onRequestClockAppAlarmSwitchListener mOnRequestClockAppAlarmSwitchListener;

    // region Constantes
    public enum MODES {
        CHRONO, TIMER
    }

    public enum CLOCK_APP_ALARM_SWITCHES {
        ON, OFF
    }

    private final long TIME_DEFAULT_VALUE = 0;
    //endregion
    //region Variables
    private int idct;                     //  Identifiant du Chrono ou Timer (1, 2, 3, ...)
    private MODES mode;                    //  CHRONO ou TIMER
    private boolean selected;             //  True si sélectionné
    private boolean running;              //  True si en cours (Actif)
    private boolean splitted;             //  True si Split
    private boolean clockAppAlarmOn;      //  True si alarme a été sollicitée dans Clock App pour l'expiration (si Timer)
    private String label;                 //  Label associé
    private String labelInit;             //  Label associé initial (non éditable)
    private long timeStart;               //  Temps mesuré lors du dernier Start (en ms)
    private long timeAcc;                 //  Temps actif écoulé jusqu'au dernier Stop (en ms)
    private long timeAccUntilSplit;       //  Temps actif écoulé jusqu'au dernier Split (en ms)
    private long timeDef;                 //  Temps par défaut (en ms)
    private long timeDefInit;             //  Temps par défaut initial (non éditable) (en ms)
    private long timeExp;                 //  Temps d'expiration (si Timer) (en ms)
    private long timeDisplay;             //  Temps à afficher (écoulé (si Chrono) ou restant (si Timer)) (en ms)
    private long timeDisplayWithoutSplit; //  Idem timeDisplay mais sans tenir compte du Split (pour Tri de liste dans MainActivity) (en ms)
    //endregion

    public CtRecord() {
        fill(0, MODES.CHRONO, false, false, false, false, null, null, TIME_DEFAULT_VALUE, TIME_DEFAULT_VALUE, TIME_DEFAULT_VALUE, TIME_DEFAULT_VALUE, TIME_DEFAULT_VALUE, getMidnightTimeMillis(), TIME_DEFAULT_VALUE, TIME_DEFAULT_VALUE);
    }

    public CtRecord(int idct, MODES mode, boolean selected, boolean running, boolean splitted, boolean clockAppAlarmOn, String label, String labelInit, long timeStart, long timeAcc, long timeAccUntilSplit, long timeDef, long timeDefInit, long timeExp) {  //  pas timeDisplay ni timeDisplayWithoutSplit, toujours mis à TIME_DEFAULT_VALUE à l'initialisation
        fill(idct, mode, selected, running, splitted, clockAppAlarmOn, label, labelInit, timeStart, timeAcc, timeAccUntilSplit, timeDef, timeDefInit, timeExp, TIME_DEFAULT_VALUE, TIME_DEFAULT_VALUE);
    }

    public int getIdct() {
        return idct;
    }

    public void setIdct(int newIdct) {
        idct = newIdct;
    }

    public MODES getMode() {
        return mode;
    }

    public boolean setMode(MODES newMode) {
        if (!mode.equals(newMode)) {
            if (!running) {
                mode = newMode;
                timeDef = timeDefInit;
                reset();
                return true;
            }
        }
        return false;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelectedOn(boolean selectedOn) {
        selected = selectedOn;
    }

    public boolean isRunning() {    //  Pas de set
        return running;
    }

    public boolean isSplitted() {   //  Pas de set
        return splitted;
    }

    public boolean isReset() {
        return ((timeAcc == 0) && (!running));
    }

    public boolean isClockAppAlarmOn() {
        return clockAppAlarmOn;
    }

    public void setClockAppAlarmOn(boolean clockAppAlarmOn) {
        if (this.clockAppAlarmOn != clockAppAlarmOn) {
            if (mode.equals(MODES.TIMER)) {
                if (running) {
                    this.clockAppAlarmOn = clockAppAlarmOn;
                    if (mOnRequestClockAppAlarmSwitchListener != null) {
                        mOnRequestClockAppAlarmSwitchListener.onRequestClockAppAlarmSwitch(this, clockAppAlarmOn ? CLOCK_APP_ALARM_SWITCHES.ON : CLOCK_APP_ALARM_SWITCHES.OFF);    //   Signaler la nécessité d'activer ou non l'alarme dans l'application Clock (si nécessaire)
                    }
                }
            }
        }
    }

    public String getLabel() {
        return label;
    }

    public boolean setLabel(String newLabel) {
        boolean setOK = true;
        if (label != newLabel) {
            if (mode.equals(MODES.TIMER)) {
                if (running) {
                    if (isClockAppAlarmOn()) {    //  Trop perturbant pour l'utilisateur (Passage par l'interface de Clock App, reprogrammation, ...)
                        setOK = false;
                    }
                }
            }
            if (setOK) {
                label = newLabel;
            }
        }
        return setOK;
    }

    public String getLabelInit() {
        return labelInit;
    }

    public void setLabelInit(String labelInit) {
        this.labelInit = labelInit;
    }

    public long getTimeStart() {   //  Pas de set
        return timeStart;
    }

    public long getTimeAcc() {   //  Pas de set
        return timeAcc;
    }

    public long getTimeAccUntilSplit() {   //  Pas de set
        return timeAccUntilSplit;
    }

    public long getTimeDef() {
        return timeDef;
    }

    public boolean setTimeDef(long newTimeDef, long nowm) {
        boolean setOK = true;
        if (timeDef != newTimeDef) {
            if (mode.equals(MODES.TIMER)) {
                if (running) {
                    if (isClockAppAlarmOn()) {    //  Trop perturbant pour l'utilisateur (Passage par l'interface de Clock App, reprogrammation, ...)
                        setOK = false;
                    } else {
                        long newTimeExp = timeStart + newTimeDef - timeAcc;
                        if (newTimeExp > nowm) {   //  Il est encore temps
                            timeExp = newTimeExp;
                        } else {
                            setOK = false;
                        }
                    }
                } else {   //  Pas Running
                    reset();
                }
            }
            if (setOK) {
                timeDef = newTimeDef;
            }
        }
        return setOK;
    }


    public long getTimeDefInit() {
        return timeDefInit;
    }

    public void setTimeDefInit(long timeDefInit) {
        this.timeDefInit = timeDefInit;
    }

    public long getTimeExp() {   //  Pas de set
        return timeExp;
    }

    public long getTimeDisplay() {   //  Pas de set
        return timeDisplay;
    }

    public long getTimeDisplayWithoutSplit() {   //  Pas de set
        return timeDisplayWithoutSplit;
    }

    public void updateTime(long nowm) {  // Actualiser le Chrono/Timer au moment nowm ("Maintenant") (en ms)
        boolean expired = false;
        if (mode.equals(MODES.TIMER)) {
            if (running) {
                if (timeExp < nowm) {    //  Timer expiré => Comme un Reset, mais sans la demande de désactivation éventuelle de l'alarme dans Clock App car elle a déjà dû sonner et être désactivée par l'utilisateur
                    running = false;
                    splitted = false;
                    timeAcc = 0;
                    timeDisplayWithoutSplit = timeDef;
                    timeDisplay = timeDisplayWithoutSplit;
                    clockAppAlarmOn = false;
                    if (mOnExpiredTimerListener != null) {
                        mOnExpiredTimerListener.onExpiredTimer(this);
                    }
                    expired = true;
                }
            }
        }
        if (!expired) {
            long tacc = timeAcc;
            if (running) {
                tacc = tacc + nowm - timeStart;
            }
            long taus = timeAccUntilSplit;
            if (mode.equals(MODES.TIMER)) {
                tacc = -tacc;
                taus = -taus;
            }
            timeDisplayWithoutSplit = (timeDef + tacc) % TIME_UNITS.DAY.DURATION_MS();      //  => Retour à 00:00:00.00 après 23:59:59.99
            timeDisplay = ((splitted) ? (timeDef + taus) % TIME_UNITS.DAY.DURATION_MS() : timeDisplayWithoutSplit);
        }
    }

    public void start(long nowm, boolean setClockAppAlarmOnStartTimer) {
        if (!running) {
            if ((mode.equals(MODES.CHRONO)) || (timeDef > 0)) {
                running = true;
                timeStart = nowm;
                if (mode.equals(MODES.TIMER)) {
                    timeExp = nowm + timeDef - timeAcc;
                    if (setClockAppAlarmOnStartTimer) {
                        if (!clockAppAlarmOn) {
                            clockAppAlarmOn = true;
                            if (mOnRequestClockAppAlarmSwitchListener != null) {
                                mOnRequestClockAppAlarmSwitchListener.onRequestClockAppAlarmSwitch(this, CLOCK_APP_ALARM_SWITCHES.ON);    //   Signaler la nécessité de désactiver l'alarme dans l'application Clock (si nécessaire)
                            }
                        }
                    }
                }
            }
        }
    }

    public void stop(long nowm) {
        if (running) {
            running = false;
            timeAcc = timeAcc + nowm - timeStart;
            if (mode.equals(MODES.TIMER)) {
                if (clockAppAlarmOn) {
                    clockAppAlarmOn = false;
                    if (mOnRequestClockAppAlarmSwitchListener != null) {
                        mOnRequestClockAppAlarmSwitchListener.onRequestClockAppAlarmSwitch(this, CLOCK_APP_ALARM_SWITCHES.OFF);    //   Signaler la nécessité de désactiver l'alarme dans l'application Clock (si nécessaire)
                    }
                }
            }
        }
    }

    public void split(long nowm) {
        if (running || splitted) {
            splitted = !splitted;
            if (splitted) {  //  => Running
                timeAccUntilSplit = timeAcc + nowm - timeStart;
            }
        }
    }

    public void reset() {
        timeAcc = 0;
        splitted = false;
        if (running) {
            running = false;
            if (mode.equals(MODES.TIMER)) {
                if (clockAppAlarmOn) {
                    clockAppAlarmOn = false;
                    if (mOnRequestClockAppAlarmSwitchListener != null) {
                        mOnRequestClockAppAlarmSwitchListener.onRequestClockAppAlarmSwitch(this, CLOCK_APP_ALARM_SWITCHES.OFF);    //   Signaler la nécessité de désactiver l'alarme dans l'application Clock (si nécessaire)
                    }
                }
            }
        }
    }

    public String getClockAppAlarmDescription() {
        return "Clock App alarm" + CRLF + label + " @ " + getFormattedTimeZoneLongTimeDate(timeExp, HHmm);
    }

    private void fill(int idct, MODES mode, boolean selected, boolean running, boolean splitted, boolean clockAppAlarmOn, String label, String labelInit, long timeStart, long timeAcc, long timeAccUntilSplit, long timeDef, long timeDefInit, long timeExp, long timeDisplay, long timeDisplayWithoutSplit) {
        this.idct = idct;
        this.mode = mode;
        this.selected = selected;
        this.running = running;
        this.splitted = splitted;
        this.clockAppAlarmOn = clockAppAlarmOn;
        this.label = label;
        this.labelInit = labelInit;
        this.timeStart = timeStart;
        this.timeAcc = timeAcc;
        this.timeAccUntilSplit = timeAccUntilSplit;
        this.timeDef = timeDef;
        this.timeDefInit = timeDefInit;
        this.timeExp = timeExp;
        this.timeDisplay = timeDisplay;
        this.timeDisplayWithoutSplit = timeDisplayWithoutSplit;
    }

}
