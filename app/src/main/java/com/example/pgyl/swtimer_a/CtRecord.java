package com.example.pgyl.swtimer_a;

import android.content.Context;

import com.example.pgyl.pekislib_a.ClockAppAlarmUtils;

import static com.example.pgyl.pekislib_a.Constants.CRLF;
import static com.example.pgyl.pekislib_a.TimeDateUtils.HHmm;
import static com.example.pgyl.pekislib_a.TimeDateUtils.TIME_UNITS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.formattedTimeZoneLongTimeDate;
import static com.example.pgyl.pekislib_a.TimeDateUtils.midnightTimeMillis;

class CtRecord {   //  Données d'un Chrono ou Timer
    // region Constantes
    public enum MODE {
        CHRONO, TIMER
    }

    public static final boolean VIA_CLOCK_APP = true;
    private final long TIME_DEFAULT_VALUE = 0;
    //endregion
    //region Variables
    private Context context;
    private int idct;                     //  Identifiant du Chrono ou Timer (1, 2, 3, ...)
    private MODE mode;                    //  CHRONO ou TIMER
    private boolean selected;             //  True si sélectionné
    private boolean running;              //  True si en cours (Actif)
    private boolean splitted;             //  True si Split
    private boolean clockAppAlarm;        //  True si alarme active insérée dans Clock app pour l'expiration (si Timer)
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

    public CtRecord(Context context) {
        this.context = context;
        fill(0, MODE.CHRONO, false, false, false, false, null, null, TIME_DEFAULT_VALUE, TIME_DEFAULT_VALUE, TIME_DEFAULT_VALUE, TIME_DEFAULT_VALUE, TIME_DEFAULT_VALUE, midnightTimeMillis(), TIME_DEFAULT_VALUE, TIME_DEFAULT_VALUE);
    }

    public CtRecord(Context context, int idct, MODE mode, boolean selected, boolean running, boolean splitted, boolean clockAppAlarm, String label, String labelInit, long timeStart, long timeAcc, long timeAccUntilSplit, long timeDef, long timeDefInit, long timeExp) {  //  pas timeDisplay ni timeDisplayWithoutSplit, toujours mis à TIME_DEFAULT_VALUE à l'initialisation
        this.context = context;
        fill(idct, mode, selected, running, splitted, clockAppAlarm, label, labelInit, timeStart, timeAcc, timeAccUntilSplit, timeDef, timeDefInit, timeExp, TIME_DEFAULT_VALUE, TIME_DEFAULT_VALUE);
    }

    public void close() {
        context = null;
    }

    public int getIdct() {
        return idct;
    }

    public void setIdct(int newIdct) {
        idct = newIdct;
    }

    public MODE getMode() {
        return mode;
    }

    public boolean setMode(MODE newMode) {
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

    public boolean hasClockAppAlarm() {
        return clockAppAlarm;
    }

    public String getLabel() {
        return label;
    }

    public boolean setLabel(String newLabel) {
        boolean setOK = true;
        if (label != newLabel) {
            if (mode.equals(MODE.TIMER)) {
                if (running) {
                    if (hasClockAppAlarm()) {    //  Trop perturbant pour l'utilisateur (Passage par l'interface de Clock App, reprogrammation, ...)
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

    public boolean setLabelInit(String newLabelInit) {
        labelInit = newLabelInit;
        return true;
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
            if (mode.equals(MODE.TIMER)) {
                if (running) {
                    if (hasClockAppAlarm()) {    //  Trop perturbant pour l'utilisateur (Passage par l'interface de Clock App, reprogrammation, ...)
                        setOK = false;
                    } else {
                        long newTimeExp = timeStart + newTimeDef - timeAcc;
                        if (newTimeExp > nowm) {   //  Il est encore temps
                            timeExp = newTimeExp;
                        } else {
                            setOK = false;
                        }
                    }
                } else {
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

    public boolean setTimeDefInit(long newTimeDefInit) {
        timeDefInit = newTimeDefInit;
        return true;
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

    public boolean updateTime(long nowm) {  // Actualiser le Chrono/Timer au moment nowm ("Maintenant") (en ms)
        if (mode.equals(MODE.TIMER)) {
            if (running) {
                if (timeExp < nowm) {    //  Timer expiré => Reset
                    reset();
                    clockAppAlarm = false;
                    timeDisplayWithoutSplit = timeDef;
                    timeDisplay = timeDisplayWithoutSplit;
                    return false;    //  Signaler l'expiration du Timer
                }
            }
        }
        long tacc = timeAcc;
        if (running) {
            tacc = tacc + nowm - timeStart;
        }
        long taus = timeAccUntilSplit;
        if (mode.equals(MODE.TIMER)) {
            tacc = -tacc;
            taus = -taus;
        }
        timeDisplayWithoutSplit = (timeDef + tacc) % TIME_UNITS.DAY.DURATION_MS();      //  => Back to 00:00:00.00 after 23:59:59.99
        timeDisplay = ((splitted) ? (timeDef + taus) % TIME_UNITS.DAY.DURATION_MS() : timeDisplayWithoutSplit);
        return true;
    }

    public boolean start(long nowm) {
        boolean clockAppOK = true;
        if (!running) {
            if ((mode.equals(MODE.CHRONO)) || (timeDef > 0)) {
                running = true;
                timeStart = nowm;
                if (mode.equals(MODE.TIMER)) {
                    timeExp = nowm + timeDef - timeAcc;
                    if (!clockAppAlarm) {
                        clockAppOK = false;   //  Signaler la nécessité éventuelle d'activer l'alarme
                    }
                }
            }
        }
        return clockAppOK;
    }

    public boolean stop(long nowm) {
        boolean clockAppOK = true;
        if (running) {
            running = false;
            timeAcc = timeAcc + nowm - timeStart;
            if (mode.equals(MODE.TIMER)) {
                if (clockAppAlarm) {
                    clockAppOK = false;   //  Signaler la nécessité de désactiver l'alarme
                }
            }
        }
        return clockAppOK;
    }

    public boolean split(long nowm) {
        if (running || splitted) {
            if (!splitted) {  //  => Running
                splitted = true;
                timeAccUntilSplit = timeAcc + nowm - timeStart;
            } else {
                splitted = false;
            }
        }
        return true;
    }

    public boolean reset() {
        boolean clockAppOK = true;
        timeAcc = 0;
        splitted = false;
        if (running) {
            if (mode.equals(MODE.TIMER)) {
                if (clockAppAlarm) {
                    clockAppOK = false;   //  Signaler la nécessité de désactiver l'alarme
                }
            }
            running = false;
        }
        return clockAppOK;
    }

    public String getClockAppAlarmDescription() {
        return "Clock App alarm" + CRLF + label + " @ " + formattedTimeZoneLongTimeDate(timeExp, HHmm);
    }

    public void setClockAppAlarmOn(boolean viaClockApp) {
        boolean error = false;
        if (viaClockApp) {
            if (!ClockAppAlarmUtils.setClockAppAlarm(context, timeExp, label, "Setting " + getClockAppAlarmDescription())) {
                error = true;
            }
        }
        if (!error) {
            clockAppAlarm = true;
        }
    }

    public void setClockAppAlarmOff(boolean viaClockApp) {
        boolean error = false;
        if (viaClockApp) {
            if (!ClockAppAlarmUtils.dismissClockAppAlarm(context, label, "Dismissing " + getClockAppAlarmDescription())) {
                error = true;
            }
        }
        if (!error) {
            clockAppAlarm = false;
        }
    }

    private void fill(int idct, MODE mode, boolean selected, boolean running, boolean splitted, boolean clockAppAlarm, String label, String labelInit, long timeStart, long timeAcc, long timeAccUntilSplit, long timeDef, long timeDefInit, long timeExp, long timeDisplay, long timeDisplayWithoutSplit) {
        this.idct = idct;
        this.mode = mode;
        this.selected = selected;
        this.running = running;
        this.splitted = splitted;
        this.clockAppAlarm = clockAppAlarm;
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
