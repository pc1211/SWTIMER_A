package com.example.pgyl.swtimer_a;

import android.content.Context;

import com.example.pgyl.pekislib_a.ClockAppAlarmUtils;
import com.example.pgyl.swtimer_a.Constants.TABLE_CHRONO_TIMERS_DATA_FIELDS;

import java.util.Calendar;

import static com.example.pgyl.pekislib_a.Constants.CRLF;
import static com.example.pgyl.pekislib_a.StringShelfDatabase.TABLE_ID_INDEX;
import static com.example.pgyl.pekislib_a.TimeDateUtils.TIMEUNITS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.TIME_HMS_SEPARATOR;
import static com.example.pgyl.pekislib_a.TimeDateUtils.convertMsToHms;
import static com.example.pgyl.pekislib_a.TimeDateUtils.midnightInMillis;

class CtRecord {   //  Données d'un Chrono ou Timer
    // region Constantes
    public enum MODE {
        CHRONO, TIMER
    }

    public static boolean USE_CLOCK_APP = true;

    //endregion
    //region Variables
    private Context context;
    private int idct;                     //  Identifiant du Chrono ou Timer (1, 2, 3, ...)
    private MODE mode;                    //  CHRONO ou TIMER
    private boolean selected;             //  True si sélectionné
    private boolean running;              //  True si en cours (Actif)
    private boolean splitted;             //  True si Split
    private boolean clockAppAlarm;        //  True si alarme active insérée dans Clock app pour l'expiration (si Timer)
    private String message;               //  Message associé
    private String messageInit;           //  Message associé initial (non éditable)
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
        init();
    }

    private void init() {
        idct = 0;
        mode = MODE.CHRONO;
        selected = false;
        running = false;
        splitted = false;
        clockAppAlarm = false;
        message = null;
        messageInit = null;
        timeStart = 0;
        timeAcc = 0;
        timeAccUntilSplit = 0;
        timeDef = 0;
        timeDefInit = 0;
        timeExp = midnightInMillis();
        timeDisplay = 0;
        timeDisplayWithoutSplit = 0;
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

    public void setSelectedOn() {
        selected = true;
    }

    public void setSelectedOff() {
        selected = false;
    }

    public boolean isRunning() {    //  Pas de set
        return running;
    }

    public boolean isSplitted() {   //  Pas de set
        return splitted;
    }

    public boolean hasClockAppAlarm() {
        return clockAppAlarm;
    }

    public String getMessage() {
        return message;
    }

    public boolean setMessage(String newMessage) {
        boolean ret = true;
        if (message != newMessage) {
            if (mode.equals(MODE.TIMER)) {
                if (running) {
                    if (hasClockAppAlarm()) {
                        ret = false;
                    }
                }
            }
            if (ret) {
                message = newMessage;
            }
        }
        return ret;
    }

    public String getMessageInit() {
        return messageInit;
    }

    public boolean setMessageInit(String newMessageInit) {
        messageInit = newMessageInit;
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

    public boolean setTimeDef(long newTimeDef) {
        boolean ret = true;
        if (timeDef != newTimeDef) {
            if (mode.equals(MODE.TIMER)) {
                if (running) {
                    if (hasClockAppAlarm()) {
                        ret = false;
                    }
                } else {
                    reset();
                }
            }
            if (ret) {
                timeDef = newTimeDef;
            }
        }
        return ret;
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

    public String getTimeZoneExpirationMessage() {
        return "Timer " + message + CRLF + "expired @" + convertMsToHms(getTimeZoneExpirationTime(), TIMEUNITS.SEC);
    }

    public long getTimeZoneExpirationTime() {   //  OK TimeZone; Sans les ms de calendar
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeExp);
        long ret = calendar.get(Calendar.HOUR_OF_DAY) * TIMEUNITS.HOUR.MS() + calendar.get(Calendar.MINUTE) * TIMEUNITS.MIN.MS() + calendar.get(Calendar.SECOND) * TIMEUNITS.SEC.MS();
        calendar = null;
        return ret;
    }

    public String getClockAppAlarmTime() {   //  OK TimeZone
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeExp);
        String ret = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)) + TIME_HMS_SEPARATOR + String.format("%02d", calendar.get(Calendar.MINUTE));
        calendar = null;
        return ret;
    }

    public String getClockAppAlarmMessage() {    //  OK TimeZone
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeExp);
        String ret = message + " (in " + String.valueOf(calendar.get(Calendar.SECOND)) + "s)";
        calendar = null;
        return ret;
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
        timeDisplayWithoutSplit = (timeDef + tacc) % TIMEUNITS.DAY.MS();      //  => Max 23h59m59s99c
        if (splitted) {
            timeDisplay = (timeDef + taus) % TIMEUNITS.DAY.MS();
        } else {
            timeDisplay = timeDisplayWithoutSplit;
        }
        return true;
    }

    public boolean start(long nowm) {
        if (!running) {
            if ((mode.equals(MODE.CHRONO)) || (timeDef > 0)) {
                running = true;
                timeStart = nowm;
                if (mode.equals(MODE.TIMER)) {
                    timeExp = nowm + timeDef - timeAcc;
                }
            }
        }
        return true;
    }

    public boolean stop(long nowm) {
        boolean ret = true;
        if (running) {
            running = false;
            timeAcc = timeAcc + nowm - timeStart;
            if (mode.equals(MODE.TIMER)) {
                if (clockAppAlarm) {
                    ret = false;   //  Signaler la nécessité de désactiver l'alarme
                }
            }
        }
        return ret;
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
        boolean ret = true;
        timeAcc = 0;
        splitted = false;
        if (running) {
            if (mode.equals(MODE.TIMER)) {
                if (clockAppAlarm) {
                    ret = false;   //  Signaler la nécessité de désactiver l'alarme
                }
            }
            running = false;
        }
        return ret;
    }

    public void setClockAppAlarmOn(boolean useClockApp) {
        boolean error = false;
        if (useClockApp) {
            if (!ClockAppAlarmUtils.setClockAppAlarm(context, timeExp, getClockAppAlarmMessage())) {
                error = true;
            }
        }
        if (!error) {
            clockAppAlarm = true;
        }
    }

    public void setClockAppAlarmOff(boolean useClockApp) {
        boolean error = false;
        if (useClockApp) {
            if (!ClockAppAlarmUtils.dismissClockAppAlarm(context, getClockAppAlarmMessage())) {
                error = true;
            }
        }
        if (!error) {
            clockAppAlarm = false;
        }
    }

    public CtRecord loadFromChronoTimerRow(String[] chronoTimerRow) {
        idct = Integer.parseInt(chronoTimerRow[TABLE_ID_INDEX]);
        mode = CtRecord.MODE.valueOf(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.MODE.INDEX()]);
        selected = (Integer.parseInt(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.SELECTED.INDEX()]) == 1);
        running = (Integer.parseInt(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.RUNNING.INDEX()]) == 1);
        splitted = (Integer.parseInt(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.SPLITTED.INDEX()]) == 1);
        clockAppAlarm = (Integer.parseInt(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.ALARM_SET.INDEX()]) == 1);
        message = chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.MESSAGE.INDEX()];
        messageInit = chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.MESSAGE_INIT.INDEX()];
        timeStart = Long.parseLong(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_START.INDEX()]);
        timeAcc = Long.parseLong(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_ACC.INDEX()]);
        timeAccUntilSplit = Long.parseLong(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_ACC_UNTIL_SPLIT.INDEX()]);
        timeDef = Long.parseLong(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_DEF.INDEX()]);
        timeDefInit = Long.parseLong(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_DEF_INIT.INDEX()]);
        timeExp = Long.parseLong(chronoTimerRow[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_EXP.INDEX()]);
        timeDisplay = 0;
        timeDisplayWithoutSplit = 0;
        return this;
    }

    public String[] convertToChronoTimerRow() {
        String[] ret = new String[1 + TABLE_CHRONO_TIMERS_DATA_FIELDS.values().length];  //  Champ ID + Données
        ret[TABLE_ID_INDEX] = String.valueOf(idct);
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.MODE.INDEX()] = mode.toString();
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.SELECTED.INDEX()] = String.valueOf(selected ? 1 : 0);
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.RUNNING.INDEX()] = String.valueOf(running ? 1 : 0);
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.SPLITTED.INDEX()] = String.valueOf(splitted ? 1 : 0);
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.ALARM_SET.INDEX()] = String.valueOf(clockAppAlarm ? 1 : 0);
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.MESSAGE.INDEX()] = message;
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.MESSAGE_INIT.INDEX()] = messageInit;
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_START.INDEX()] = String.valueOf(timeStart);
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_ACC.INDEX()] = String.valueOf(timeAcc);
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_ACC_UNTIL_SPLIT.INDEX()] = String.valueOf(timeAccUntilSplit);
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_DEF.INDEX()] = String.valueOf(timeDef);
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_DEF_INIT.INDEX()] = String.valueOf(timeDefInit);
        ret[TABLE_CHRONO_TIMERS_DATA_FIELDS.TIME_EXP.INDEX()] = String.valueOf(timeExp);
        return ret;
    }

}
