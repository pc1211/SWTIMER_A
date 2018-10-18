package com.example.pgyl.swtimer_a;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.example.pgyl.pekislib_a.StringShelfDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static android.content.Context.MODE_PRIVATE;
import static com.example.pgyl.pekislib_a.ClockAppAlarmUtils.dismissClockAppAlarm;
import static com.example.pgyl.pekislib_a.ClockAppAlarmUtils.setClockAppAlarm;
import static com.example.pgyl.pekislib_a.Constants.DUMMY_VALUE;
import static com.example.pgyl.pekislib_a.Constants.NOT_FOUND;
import static com.example.pgyl.pekislib_a.Constants.SHP_FILE_NAME_SUFFIX;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_SHP_KEY_NAMES;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_TABLES;
import static com.example.pgyl.swtimer_a.CtRecord.MODE;
import static com.example.pgyl.swtimer_a.CtRecord.USE_CLOCK_APP;

public class CtRecordsHandler {
    //region Constantes
    private enum ACTIONS_ON_ALL {
        UPDATE_TIME, INVERT_SELECTION, SELECT, COUNT
    }

    private enum ACTIONS_ON_SELECTION {
        START, STOP, SPLIT, RESET, REMOVE, COUNT
    }

    private enum ACTIONS_ON_CLOCK_APP_ALARM {
        SET, DISMISS
    }

    private enum ALARM_SEPARATORS {
        ALARM, ACTION, MESSAGE, TIME_EXP;

        public String SID() {
            return "£µ$" + this.toString() + "$µ£";
        }
    }

    //endregion
    //region Variables
    private Context context;
    private ArrayList<CtRecord> ctRecords;
    private StringShelfDatabase stringShelfDatabase;
    private String shpFileName;
    private String requestedClockAppAlarmActions;
    //endregion

    public CtRecordsHandler(Context context, StringShelfDatabase stringShelfDatabase) {
        this.context = context;
        this.stringShelfDatabase = stringShelfDatabase;
        shpFileName = context.getPackageName() + SHP_FILE_NAME_SUFFIX;   //  Sans nom d'activité car partagé avec CtDisplayActivity & MainActivity
        init();
    }

    private void init() {
        requestedClockAppAlarmActions = getSHPRequestedClockAppAlarms();
        processNextRequestedClockAppAlarmAction();
        ctRecords = convertChronoTimerRowsToCtRecords(getChronoTimerRows());
    }

    public void saveAndclose() {
        saveChronoTimers();
        savePreferences();
        stringShelfDatabase = null;
        closeChronoTimers();
        context = null;
    }

    public ArrayList<CtRecord> getChronoTimers() {
        return ctRecords;
    }

    public int createNewChronoTimer(MODE mode, long timeDefInit, String messageInit) {
        CtRecord ctRecord = new CtRecord(context);
        int idct = getMaxId() + 1;
        ctRecord.setIdct(idct);
        ctRecord.setMode(mode);
        ctRecord.setTimeDefInit(timeDefInit);
        ctRecord.setTimeDef(timeDefInit);
        ctRecord.setMessageInit(messageInit + idct);
        ctRecord.setMessage(messageInit + idct);
        ctRecords.add(ctRecord);
        return ctRecord.getIdct();
    }

    public void sortCtRecords() {
        if (ctRecords.size() >= 2) {
            Collections.sort(ctRecords, new Comparator<CtRecord>() {
                public int compare(CtRecord ctRecord1, CtRecord ctRecord2) {
                    boolean running1 = ctRecord1.isRunning();
                    boolean running2 = ctRecord2.isRunning();
                    int ret = (running1 == running2) ? 0 : (running1 ? -1 : 1);    //  SORT running DESC
                    if (ret == 0) {    //  running1 = running2
                        String mode1 = ctRecord1.getMode().toString();
                        String mode2 = ctRecord2.getMode().toString();
                        ret = mode2.compareTo(mode1);                              //  SORT mode DESC  (Timers puis Chronos )
                        if (ret == 0) {     //  mode1 = mode2
                            long time1 = ctRecord1.getTimeDisplayWithoutSplit();   //  OK si updateTime() appelé pour tous les ctRecords avant le tri
                            long time2 = ctRecord2.getTimeDisplayWithoutSplit();
                            ret = ((time1 == time2) ? 0 : ((time1 > time2) ? 1 : -1));   //  Si Timer  => SORT time ASC   (d'abord les plus petits temps)
                            if (mode1.equals(MODE.CHRONO.toString())) {                  //  Si Chrono => SORT time DESC  (d'abord les plus grands temps)
                                ret = -ret;
                            }
                            if (ret == 0) {     //  time1 = time2
                                String message1 = ctRecord1.getMessage();
                                String message2 = ctRecord2.getMessage();
                                ret = message1.compareTo(message2);           //  SORT message ASC
                            }
                        }
                    }
                    return ret;
                }
            });
        }
    }

    public ArrayList<CtRecord> convertChronoTimerRowsToCtRecords(String[][] chronoTimerRows) {
        ArrayList<CtRecord> ret = new ArrayList<CtRecord>();
        if (chronoTimerRows != null) {
            for (int i = 0; i <= (chronoTimerRows.length - 1); i = i + 1) {
                ret.add(new CtRecord(context).loadFromChronoTimerRow(chronoTimerRows[i]));
            }
        }
        return ret;
    }

    public String[][] convertCtRecordsToChronoTimerRows(ArrayList<CtRecord> ctRecords) {
        String[][] ret = null;
        if (!ctRecords.isEmpty()) {
            ret = new String[ctRecords.size()][];
            for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
                ret[i] = ctRecords.get(i).convertToChronoTimerRow();
            }
        }
        return ret;
    }

    public int updateTimeAll(long nowm) {
        return actionOnAll(ACTIONS_ON_ALL.UPDATE_TIME, nowm);
    }

    public void invertSelectionAll() {
        actionOnAll(ACTIONS_ON_ALL.INVERT_SELECTION, DUMMY_VALUE);
    }

    public void selectAll() {
        actionOnAll(ACTIONS_ON_ALL.SELECT, DUMMY_VALUE);
    }

    public int getCountAll() {
        return actionOnAll(ACTIONS_ON_ALL.COUNT, DUMMY_VALUE);
    }

    public void startSelection(long nowm) {
        actionOnSelection(ACTIONS_ON_SELECTION.START, nowm);
    }

    public void stopSelection(long nowm) {
        actionOnSelection(ACTIONS_ON_SELECTION.STOP, nowm);
    }

    public void splitSelection(long nowm) {
        actionOnSelection(ACTIONS_ON_SELECTION.SPLIT, nowm);
    }

    public void resetSelection() {
        actionOnSelection(ACTIONS_ON_SELECTION.RESET, DUMMY_VALUE);
    }

    public void removeSelection() {
        actionOnSelection(ACTIONS_ON_SELECTION.REMOVE, DUMMY_VALUE);
    }

    public int getCountSelection() {
        return actionOnSelection(ACTIONS_ON_SELECTION.COUNT, DUMMY_VALUE);
    }

    private int actionOnAll(ACTIONS_ON_ALL action, long nowm) {
        int ret = 0;
        if (!ctRecords.isEmpty()) {
            for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
                if (action.equals(ACTIONS_ON_ALL.UPDATE_TIME)) {
                    if (!ctRecords.get(i).updateTime(nowm)) {   //  Timer expiré
                        Toast.makeText(context, ctRecords.get(i).getTimeZoneExpirationMessage(), Toast.LENGTH_LONG).show();
                        ret = ret + 1;
                    }
                }
                if (action.equals(ACTIONS_ON_ALL.INVERT_SELECTION)) {
                    if (ctRecords.get(i).isSelected()) {
                        ctRecords.get(i).setSelectedOff();
                    } else {
                        ctRecords.get(i).setSelectedOn();
                    }
                }
                if (action.equals(ACTIONS_ON_ALL.SELECT)) {
                    ctRecords.get(i).setSelectedOn();
                }
            }
            if (!action.equals(ACTIONS_ON_ALL.UPDATE_TIME)) {
                ret = ctRecords.size();
            }
        }
        return ret;
    }

    private int actionOnSelection(ACTIONS_ON_SELECTION action, long nowm) {
        int ret = 0;
        if (!ctRecords.isEmpty()) {
            int i = 0;
            do {
                if (ctRecords.get(i).isSelected()) {
                    ret = ret + 1;   //  Compter
                    if (action.equals(ACTIONS_ON_SELECTION.START)) {
                        ctRecords.get(i).start(nowm);
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.STOP)) {
                        if (!ctRecords.get(i).stop(nowm)) {
                            RequestAdditionalClockAppAlarmAction(ctRecords.get(i), ACTIONS_ON_CLOCK_APP_ALARM.DISMISS);
                        }
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.SPLIT)) {
                        ctRecords.get(i).split(nowm);
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.RESET)) {
                        if (!ctRecords.get(i).reset()) {
                            RequestAdditionalClockAppAlarmAction(ctRecords.get(i), ACTIONS_ON_CLOCK_APP_ALARM.DISMISS);
                        }
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.REMOVE)) {
                        if (!ctRecords.get(i).reset()) {
                            RequestAdditionalClockAppAlarmAction(ctRecords.get(i), ACTIONS_ON_CLOCK_APP_ALARM.DISMISS);
                        }
                        ctRecords.remove(i);
                        if (ctRecords.isEmpty()) {
                            break;   //  Evacuation générale
                        }
                        i = i - 1;   //  Compenser le remove
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.COUNT)) {     //  cf ret
                        //  NOP
                    }
                }
                i = i + 1;
            }
            while (i < ctRecords.size());
        }
        processNextRequestedClockAppAlarmAction();
        return ret;
    }

    //  Ajouter  £µ$ALARM$µ££µ$<SET/DISMISS>$µ££µ$<Message>$µ££µ$<Expiration time>$µ£  à  requestedClockAppAlarmActions
    private void RequestAdditionalClockAppAlarmAction(CtRecord ctRecord, ACTIONS_ON_CLOCK_APP_ALARM action) {
        requestedClockAppAlarmActions = requestedClockAppAlarmActions + ALARM_SEPARATORS.ALARM.SID() + ALARM_SEPARATORS.ACTION.SID() + action.toString() + ALARM_SEPARATORS.MESSAGE.SID() + ctRecord.getClockAppAlarmMessage() + ALARM_SEPARATORS.TIME_EXP.SID() + String.valueOf(ctRecord.getTimeExp());
        if (action.equals(ACTIONS_ON_CLOCK_APP_ALARM.SET)) {
            ctRecord.setClockAppAlarm(!USE_CLOCK_APP);
        }
        if (action.equals(ACTIONS_ON_CLOCK_APP_ALARM.DISMISS)) {
            ctRecord.dismissClockAppAlarm(!USE_CLOCK_APP);
        }
    }

    private void processNextRequestedClockAppAlarmAction() {   //  Une alarme à la fois, la prochaine est traitée au prochain init() de CtRecordsHandler
        String clockAppAlarmString;

        if (!requestedClockAppAlarmActions.equals("")) {
            requestedClockAppAlarmActions = requestedClockAppAlarmActions.substring(ALARM_SEPARATORS.ALARM.SID().length());
            int nextAlarmIndex = requestedClockAppAlarmActions.indexOf(ALARM_SEPARATORS.ALARM.SID());
            if (nextAlarmIndex != NOT_FOUND) {
                clockAppAlarmString = requestedClockAppAlarmActions.substring(0, nextAlarmIndex);
                requestedClockAppAlarmActions = requestedClockAppAlarmActions.substring(nextAlarmIndex);
            } else {
                clockAppAlarmString = requestedClockAppAlarmActions;
                requestedClockAppAlarmActions = "";
            }
            int actionIndex = clockAppAlarmString.indexOf(ALARM_SEPARATORS.ACTION.SID());
            int messageIndex = clockAppAlarmString.indexOf(ALARM_SEPARATORS.MESSAGE.SID());
            int timeExpIndex = clockAppAlarmString.indexOf(ALARM_SEPARATORS.TIME_EXP.SID());
            String action = clockAppAlarmString.substring(actionIndex + ALARM_SEPARATORS.ACTION.SID().length(), messageIndex);
            String message = clockAppAlarmString.substring(messageIndex + ALARM_SEPARATORS.MESSAGE.SID().length(), timeExpIndex);
            if (action.equals(ACTIONS_ON_CLOCK_APP_ALARM.SET.toString())) {
                long timeExp = Long.parseLong(clockAppAlarmString.substring(timeExpIndex + ALARM_SEPARATORS.TIME_EXP.SID().length()));
                Toast.makeText(context, "Setting Clock alarm " + message, Toast.LENGTH_LONG).show();
                setClockAppAlarm(context, timeExp, message);
            }
            if (action.equals(ACTIONS_ON_CLOCK_APP_ALARM.DISMISS.toString())) {
                Toast.makeText(context, "Dismissing Clock alarm " + message, Toast.LENGTH_LONG).show();
                dismissClockAppAlarm(context, message);
            }
        }
    }

    private int getMaxId() {
        int ret = 0;
        if (!ctRecords.isEmpty()) {
            for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
                if (ctRecords.get(i).getIdct() > ret) {
                    ret = ctRecords.get(i).getIdct();
                }
            }
        }
        return ret;
    }

    private void savePreferences() {
        SharedPreferences shp = context.getSharedPreferences(shpFileName, MODE_PRIVATE);
        SharedPreferences.Editor shpEditor = shp.edit();
        shpEditor.putString(SWTIMER_SHP_KEY_NAMES.REQUESTED_CLOCK_APP_ALARMS.toString(), requestedClockAppAlarmActions);
        shpEditor.commit();
    }

    private String getSHPRequestedClockAppAlarms() {
        SharedPreferences shp = context.getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getString(SWTIMER_SHP_KEY_NAMES.REQUESTED_CLOCK_APP_ALARMS.toString(), "");
    }

    private void closeChronoTimers() {
        for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
            ctRecords.get(i).close();
        }
        ctRecords.clear();
        ctRecords = null;
    }

    private String[][] getChronoTimerRows() {
        return stringShelfDatabase.selectRows(SWTIMER_TABLES.CHRONO_TIMERS.toString(), null);
    }

    private void saveChronoTimers() {
        stringShelfDatabase.deleteRows(SWTIMER_TABLES.CHRONO_TIMERS.toString(), null);
        stringShelfDatabase.insertOrReplaceRows(SWTIMER_TABLES.CHRONO_TIMERS.toString(), convertCtRecordsToChronoTimerRows(ctRecords));
    }

}
