package com.example.pgyl.swtimer_a;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.pgyl.pekislib_a.StringShelfDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static android.content.Context.MODE_PRIVATE;
import static com.example.pgyl.pekislib_a.ClockAppAlarmUtils.dismissClockAppAlarm;
import static com.example.pgyl.pekislib_a.Constants.CRLF;
import static com.example.pgyl.pekislib_a.Constants.DUMMY_VALUE;
import static com.example.pgyl.pekislib_a.Constants.NOT_FOUND;
import static com.example.pgyl.pekislib_a.Constants.SHP_FILE_NAME_SUFFIX;
import static com.example.pgyl.pekislib_a.MiscUtils.toastLong;
import static com.example.pgyl.pekislib_a.TimeDateUtils.HHmmss;
import static com.example.pgyl.pekislib_a.TimeDateUtils.formattedTimeZoneLongTimeDate;
import static com.example.pgyl.swtimer_a.CtRecord.MODE;
import static com.example.pgyl.swtimer_a.CtRecord.VIA_CLOCK_APP;
import static com.example.pgyl.swtimer_a.MainActivity.SWTIMER_SHP_KEY_NAMES;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.chronoTimerRowToCtRecord;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseTables.ctRecordToChronoTimerRow;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.saveChronoTimers;

public class CtRecordsHandler {
    //region Constantes
    private enum ACTIONS_ON_ALL {
        UPDATE_TIME, INVERT_SELECTION, SELECT, COUNT
    }

    private enum ACTIONS_ON_SELECTION {
        START, STOP, SPLIT, RESET, REMOVE, COUNT
    }

    private final String ALARM_SEPARATOR = "£µ$***ALARM***$µ£";
    private final String ALARM_FIELD_SEPARATOR = "£µ$***FIELD***$µ£";
    //endregion
    //region Variables
    private Context context;
    private ArrayList<CtRecord> ctRecords;
    private StringShelfDatabase stringShelfDatabase;
    private String shpFileName;
    private String requestedClockAppAlarmDismisses;
    private long nowm;
    private boolean setClockAppAlarmOnStartTimer;
    //endregion

    public CtRecordsHandler(Context context, StringShelfDatabase stringShelfDatabase) {
        this.context = context;
        this.stringShelfDatabase = stringShelfDatabase;
        shpFileName = context.getPackageName() + SHP_FILE_NAME_SUFFIX;   //  Sans nom d'activité car partagé avec CtDisplayActivity & MainActivity
        init();
    }

    private void init() {
        requestedClockAppAlarmDismisses = getSHPRequestedClockAppAlarmsDismisses();
        processNextRequestedClockAppAlarmDismiss();
        ctRecords = chronoTimerRowsToCtRecords(StringShelfDatabaseUtils.getChronoTimers(stringShelfDatabase));
    }

    public void saveAndclose() {
        saveChronoTimers(stringShelfDatabase, ctRecordsToChronoTimerRows(ctRecords));
        savePreferences();
        stringShelfDatabase = null;
        closeChronoTimers();
        context = null;
    }

    public ArrayList<CtRecord> getChronoTimers() {
        return ctRecords;
    }

    public int createChronoTimer(MODE mode) {
        final String LABEL_INIT_DEFAULT_VALUE = "Label";
        final long TIMEDEFINIT_DEFAULT_VALUE = 0;

        CtRecord ctRecord = new CtRecord(context);
        int idct = getMaxId() + 1;
        ctRecord.setIdct(idct);
        ctRecord.setMode(mode);
        ctRecord.setTimeDefInit(TIMEDEFINIT_DEFAULT_VALUE);
        ctRecord.setTimeDef(TIMEDEFINIT_DEFAULT_VALUE, DUMMY_VALUE);
        ctRecord.setLabelInit(LABEL_INIT_DEFAULT_VALUE + idct);
        ctRecord.setLabel(LABEL_INIT_DEFAULT_VALUE + idct);
        ctRecords.add(ctRecord);
        return ctRecord.getIdct();
    }

    public void sortCtRecords() {
        if (ctRecords.size() >= 2) {
            Collections.sort(ctRecords, new Comparator<CtRecord>() {
                public int compare(CtRecord ctRecord1, CtRecord ctRecord2) {
                    boolean running1 = ctRecord1.isRunning();
                    boolean running2 = ctRecord2.isRunning();
                    int ret = (running1 == running2) ? 0 : (running1 ? -1 : 1);    //  ORDER BY running DESC
                    if (ret == 0) {    //  running1 = running2
                        String mode1 = ctRecord1.getMode().toString();
                        String mode2 = ctRecord2.getMode().toString();
                        ret = mode2.compareTo(mode1);                              //  ORDER BY mode DESC  (Timers puis Chronos )
                        if (ret == 0) {     //  mode1 = mode2
                            long time1 = ctRecord1.getTimeDisplayWithoutSplit();   //  OK si updateTime() appelé pour tous les ctRecords avant le tri
                            long time2 = ctRecord2.getTimeDisplayWithoutSplit();
                            ret = ((time1 == time2) ? 0 : ((time1 > time2) ? 1 : -1));   //  Si Timer  => ORDER BY time ASC   (d'abord les plus petits temps)
                            if (mode1.equals(MODE.CHRONO.toString())) {                  //  Si Chrono => ORDER BY time DESC  (d'abord les plus grands temps)
                                ret = -ret;
                            }
                            if (ret == 0) {     //  time1 = time2
                                String label1 = ctRecord1.getLabel();
                                String label2 = ctRecord2.getLabel();
                                ret = label1.compareTo(label2);           //  ORDER BY label ASC
                            }
                        }
                    }
                    return ret;
                }
            });
        }
    }

    public ArrayList<CtRecord> chronoTimerRowsToCtRecords(String[][] chronoTimerRows) {
        ArrayList<CtRecord> ret = new ArrayList<CtRecord>();
        if (chronoTimerRows != null) {
            for (int i = 0; i <= (chronoTimerRows.length - 1); i = i + 1) {
                ret.add(chronoTimerRowToCtRecord(chronoTimerRows[i], context));
            }
        }
        return ret;
    }

    public String[][] ctRecordsToChronoTimerRows(ArrayList<CtRecord> ctRecords) {
        String[][] ret = null;
        if (!ctRecords.isEmpty()) {
            ret = new String[ctRecords.size()][];
            for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
                ret[i] = ctRecordToChronoTimerRow(ctRecords.get(i));
            }
        }
        return ret;
    }

    public int updateTimeAll(long nowm) {
        this.nowm = nowm;
        return actionOnAll(ACTIONS_ON_ALL.UPDATE_TIME);
    }

    public void invertSelectionAll() {
        actionOnAll(ACTIONS_ON_ALL.INVERT_SELECTION);
    }

    public void selectAll() {
        actionOnAll(ACTIONS_ON_ALL.SELECT);
    }

    public int getCountAll() {
        return actionOnAll(ACTIONS_ON_ALL.COUNT);
    }

    public void startSelection(long nowm, boolean setClockAppAlarmOnStartTimer) {
        this.nowm = nowm;
        this.setClockAppAlarmOnStartTimer = setClockAppAlarmOnStartTimer;
        actionOnSelection(ACTIONS_ON_SELECTION.START);
    }

    public void stopSelection(long nowm) {
        this.nowm = nowm;
        actionOnSelection(ACTIONS_ON_SELECTION.STOP);
    }

    public void splitSelection(long nowm) {
        this.nowm = nowm;
        actionOnSelection(ACTIONS_ON_SELECTION.SPLIT);
    }

    public void resetSelection() {
        actionOnSelection(ACTIONS_ON_SELECTION.RESET);
    }

    public void removeSelection() {
        actionOnSelection(ACTIONS_ON_SELECTION.REMOVE);
    }

    public int getCountSelection() {
        return actionOnSelection(ACTIONS_ON_SELECTION.COUNT);
    }

    private int actionOnAll(ACTIONS_ON_ALL action) {
        int ret = 0;
        if (!ctRecords.isEmpty()) {
            for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
                if (action.equals(ACTIONS_ON_ALL.UPDATE_TIME)) {
                    if (!ctRecords.get(i).updateTime(nowm)) {   //  Timer expiré
                        toastLong("Timer " + ctRecords.get(i).getLabel() + CRLF + "expired @ " + formattedTimeZoneLongTimeDate(ctRecords.get(i).getTimeExp(), HHmmss), context);
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

    private int actionOnSelection(ACTIONS_ON_SELECTION action) {
        int ret = 0;
        if (!ctRecords.isEmpty()) {
            int i = 0;
            do {
                if (ctRecords.get(i).isSelected()) {
                    ret = ret + 1;   //  Compter
                    if (action.equals(ACTIONS_ON_SELECTION.START)) {
                        if (!ctRecords.get(i).start(nowm)) {
                            if (setClockAppAlarmOnStartTimer) {
                                ctRecords.get(i).setClockAppAlarmOn(VIA_CLOCK_APP);
                            }
                        }
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.STOP)) {
                        if (!ctRecords.get(i).stop(nowm)) {
                            RequestAdditionalClockAppAlarmDismiss(ctRecords.get(i));
                        }
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.SPLIT)) {
                        ctRecords.get(i).split(nowm);
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.RESET)) {
                        if (!ctRecords.get(i).reset()) {
                            RequestAdditionalClockAppAlarmDismiss(ctRecords.get(i));
                        }
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.REMOVE)) {
                        if (!ctRecords.get(i).reset()) {
                            RequestAdditionalClockAppAlarmDismiss(ctRecords.get(i));
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
        processNextRequestedClockAppAlarmDismiss();
        return ret;
    }

    private void RequestAdditionalClockAppAlarmDismiss(CtRecord ctRecord) {
        requestedClockAppAlarmDismisses = requestedClockAppAlarmDismisses + ALARM_SEPARATOR + ctRecord.getLabel() + ALARM_FIELD_SEPARATOR + "Dismissing " + ctRecord.getClockAppAlarmDescription();
        ctRecord.setClockAppAlarmOff(!VIA_CLOCK_APP);   //  => setClockAppAlarmOff() ne fera pas appel à dismissClockAppAlarm() (processNextRequestedClockAppAlarmDismiss() le fera)
    }

    private void processNextRequestedClockAppAlarmDismiss() {   //  Une alarme à la fois, la prochaine est traitée au prochain init() de CtRecordsHandler
        String content;

        if (!requestedClockAppAlarmDismisses.equals("")) {
            requestedClockAppAlarmDismisses = requestedClockAppAlarmDismisses.substring(ALARM_SEPARATOR.length());
            int nextAlarmIndex = requestedClockAppAlarmDismisses.indexOf(ALARM_SEPARATOR);
            boolean nextAlarmFound = (nextAlarmIndex != NOT_FOUND);
            content = (nextAlarmFound ? requestedClockAppAlarmDismisses.substring(0, nextAlarmIndex) : requestedClockAppAlarmDismisses);
            requestedClockAppAlarmDismisses = (nextAlarmFound ? requestedClockAppAlarmDismisses.substring(nextAlarmIndex) : "");
            int nextFieldIndex = content.indexOf(ALARM_FIELD_SEPARATOR);   //  ALARM_FIELD_SEPARATOR toujours présent
            String alarmLabel = content.substring(0, nextFieldIndex);
            String toastMessage = content.substring(nextFieldIndex + ALARM_FIELD_SEPARATOR.length());
            dismissClockAppAlarm(context, alarmLabel, toastMessage);
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
        shpEditor.putString(SWTIMER_SHP_KEY_NAMES.REQUESTED_CLOCK_APP_ALARM_DISMISSES.toString(), requestedClockAppAlarmDismisses);
        shpEditor.commit();
    }

    private String getSHPRequestedClockAppAlarmsDismisses() {
        SharedPreferences shp = context.getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getString(SWTIMER_SHP_KEY_NAMES.REQUESTED_CLOCK_APP_ALARM_DISMISSES.toString(), "");
    }

    private void closeChronoTimers() {
        for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
            ctRecords.get(i).close();
        }
        ctRecords.clear();
        ctRecords = null;
    }

}
