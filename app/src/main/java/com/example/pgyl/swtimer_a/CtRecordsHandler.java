package com.example.pgyl.swtimer_a;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.pgyl.pekislib_a.StringDB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static android.content.Context.MODE_PRIVATE;
import static com.example.pgyl.pekislib_a.ClockAppAlarmUtils.dismissClockAppAlarm;
import static com.example.pgyl.pekislib_a.ClockAppAlarmUtils.setClockAppAlarm;
import static com.example.pgyl.pekislib_a.Constants.CRLF;
import static com.example.pgyl.pekislib_a.Constants.DUMMY_VALUE;
import static com.example.pgyl.pekislib_a.Constants.NOT_FOUND;
import static com.example.pgyl.pekislib_a.Constants.SHP_FILE_NAME_SUFFIX;
import static com.example.pgyl.pekislib_a.Constants.SWITCHES;
import static com.example.pgyl.pekislib_a.StringDBUtils.getDefaultsBase;
import static com.example.pgyl.pekislib_a.TimeDateUtils.HHmm;
import static com.example.pgyl.pekislib_a.TimeDateUtils.HHmmss;
import static com.example.pgyl.pekislib_a.TimeDateUtils.TIME_UNITS;
import static com.example.pgyl.pekislib_a.TimeDateUtils.getFormattedTimeZoneLongTimeDate;
import static com.example.pgyl.pekislib_a.TimeDateUtils.msToTimeUnit;
import static com.example.pgyl.pekislib_a.TimeDateUtils.timeFormatDToMs;
import static com.example.pgyl.swtimer_a.CtRecord.MODES;
import static com.example.pgyl.swtimer_a.MainActivity.SWTIMER_SHP_KEY_NAMES;
import static com.example.pgyl.swtimer_a.StringDBTables.chronoTimerRowsToCtRecords;
import static com.example.pgyl.swtimer_a.StringDBTables.ctRecordsToChronoTimerRows;
import static com.example.pgyl.swtimer_a.StringDBTables.getPresetsCTLabelIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getPresetsCTTableName;
import static com.example.pgyl.swtimer_a.StringDBTables.getPresetsCTTimeIndex;
import static com.example.pgyl.swtimer_a.StringDBUtils.saveDBChronoTimers;

public class CtRecordsHandler {
    public interface onExpiredTimerListener {
        void onExpiredTimer(CtRecord ctRecord);
    }

    public void setOnExpiredTimerListener(onExpiredTimerListener listener) {
        mOnExpiredTimerListener = listener;
    }

    private onExpiredTimerListener mOnExpiredTimerListener;

    //region Constantes
    private enum ACTIONS_ON_ALL {
        UPDATE_TIME, INVERT_SELECTION, SELECT, COUNT_CHRONOS, COUNT_TIMERS, COUNT
    }

    private enum ACTIONS_ON_SELECTION {
        UPDATE_TIME, START, STOP, SPLIT, RESET, REMOVE, COUNT
    }

    private final String ALARM_SEPARATOR = "£µ$***ALARM***$µ£";
    private final String ALARM_FIELD_SEPARATOR = "£µ$***FIELD***$µ£";
    //endregion
    //region Variables
    private Context context;
    private ArrayList<CtRecord> ctRecords;
    private StringDB stringDB;
    private String shpFileName;
    private String requestedClockAppAlarmDismisses;
    private long nowm;
    private boolean setClockAppAlarmOnStartTimer;
    private int selectedRunningTimersWithClockAppAlarm;
    private int clockAppAlarmSwitchOffRequestsForSelection;
    //endregion

    public CtRecordsHandler(Context context, StringDB stringDB) {
        this.context = context;
        this.stringDB = stringDB;
        setupCtRecords();
        shpFileName = context.getPackageName() + SHP_FILE_NAME_SUFFIX;   //  Sans nom d'activité car partagé avec CtDisplayActivity & MainActivity
        init();
    }

    private void init() {
        requestedClockAppAlarmDismisses = getSHPRequestedClockAppAlarmsDismisses();
        processNextRequestedClockAppAlarmDismiss();
        selectedRunningTimersWithClockAppAlarm = 0;
        clockAppAlarmSwitchOffRequestsForSelection = 0;
    }

    public void saveAndclose() {
        saveDBChronoTimers(stringDB, ctRecordsToChronoTimerRows(ctRecords));
        savePreferences();
        stringDB = null;
        ctRecords.clear();
        ctRecords = null;
        context = null;
    }

    public ArrayList<CtRecord> getChronoTimers() {
        return ctRecords;
    }

    public int createChronoTimer(MODES mode) {
        CtRecord ctRecord = new CtRecord();
        int idct = getMaxId() + 1;
        ctRecord.setIdct(idct);
        ctRecord.setMode(mode);
        String[] defaultsBase = getDefaultsBase(stringDB, getPresetsCTTableName());   //  En particulier "Label"
        long defaultTime = Long.parseLong(defaultsBase[getPresetsCTTimeIndex()]);
        ctRecord.setTimeDefInit(defaultTime);
        ctRecord.setTimeDef(defaultTime, DUMMY_VALUE);
        String defaultLabel = defaultsBase[getPresetsCTLabelIndex()];
        ctRecord.setLabelInit(defaultLabel + idct);   //  "Label<idct>"
        ctRecord.setLabel(defaultLabel + idct);
        setupCtRecordListener(ctRecord);
        ctRecords.add(ctRecord);
        return ctRecord.getIdct();
    }

    public void sortCtRecords() {
        if (ctRecords.size() >= 2) {
            Collections.sort(ctRecords, new Comparator<CtRecord>() {
                public int compare(CtRecord ctRecord1, CtRecord ctRecord2) {
                    int idct1 = ctRecord1.getIdct();
                    int idct2 = ctRecord2.getIdct();
                    int sortResult = ((idct1 == idct2) ? 0 : ((idct1 > idct2) ? 1 : -1));   //  Tri par n° idct ASC
                    return sortResult;
                }
            });
        }
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

    public int getCountAllChronos() {
        return actionOnAll(ACTIONS_ON_ALL.COUNT_CHRONOS);
    }

    public int getCountAllTimers() {
        return actionOnAll(ACTIONS_ON_ALL.COUNT_TIMERS);
    }

    public void updateTimeSelection(long nowm) {
        this.nowm = nowm;
        actionOnSelection(ACTIONS_ON_SELECTION.UPDATE_TIME);
    }

    public void startSelection(long nowm, boolean setClockAppAlarmOnStartTimer) {
        this.nowm = nowm;
        this.setClockAppAlarmOnStartTimer = setClockAppAlarmOnStartTimer;
        actionOnSelection(ACTIONS_ON_SELECTION.START);
    }

    public void stopSelection(long nowm) {
        this.nowm = nowm;
        selectedRunningTimersWithClockAppAlarm = getSelectedRunningTimersWithClockAppAlarm();   //  Ces  timers feront une demande de supression de Clock App alarme via onRequestClockAppAlarmSwitch()
        actionOnSelection(ACTIONS_ON_SELECTION.STOP);
    }

    public void splitSelection(long nowm) {
        this.nowm = nowm;
        actionOnSelection(ACTIONS_ON_SELECTION.SPLIT);
    }

    public void resetSelection() {
        selectedRunningTimersWithClockAppAlarm = getSelectedRunningTimersWithClockAppAlarm();
        actionOnSelection(ACTIONS_ON_SELECTION.RESET);
    }

    public void removeSelection() {
        selectedRunningTimersWithClockAppAlarm = getSelectedRunningTimersWithClockAppAlarm();
        actionOnSelection(ACTIONS_ON_SELECTION.REMOVE);
    }

    public int getCountSelection() {
        return actionOnSelection(ACTIONS_ON_SELECTION.COUNT);
    }

    private void onRequestClockAppAlarmSwitch(CtRecord ctRecord, SWITCHES clockAppAlarmSwitch) {   //  Créer ou désactiver une alarme dans Clock App; Evénement normalement déclenché par CtRecord
        if (clockAppAlarmSwitch.equals(SWITCHES.ON)) {   //  On peut immédiatement demander à Clock App de créer l'alarme, sans devoir quitter SwTimer App
            long gap = msToTimeUnit(timeFormatDToMs(getFormattedTimeZoneLongTimeDate(ctRecord.getTimeExp(), HHmmss)) - timeFormatDToMs(getFormattedTimeZoneLongTimeDate(ctRecord.getTimeExp(), HHmm)), TIME_UNITS.SEC);
            String message = "Setting " + ctRecord.getClockAppAlarmDescription() + CRLF + "(" + gap + "s before exact end)";
            setClockAppAlarm(context, ctRecord.getTimeExp(), ctRecord.getLabel(), message);
        } else {   //  OFF  ;  A chaque timer actif avec Clock App alarme correspondra une demande de suppression d'alarme Clock App si (stop, reset ou remove) via sélection ou via bouton individuel
            RequestAdditionalClockAppAlarmDismiss(ctRecord);
            boolean processOK = true;
            if (selectedRunningTimersWithClockAppAlarm > 0) {   //  => Via sélection
                clockAppAlarmSwitchOffRequestsForSelection = clockAppAlarmSwitchOffRequestsForSelection + 1;
                if (clockAppAlarmSwitchOffRequestsForSelection >= selectedRunningTimersWithClockAppAlarm) {   // On attend de réceptionner dans onRequestAdditionalClockAppAlarmDismiss() toutes les demandes de suppression d'alarme pour la sélection
                    selectedRunningTimersWithClockAppAlarm = 0;
                    clockAppAlarmSwitchOffRequestsForSelection = 0;
                } else {   //  Des demandes de suppression d'alarme pour la sélection sont encore attendues
                    processOK = false;
                }
            }
            if (processOK) {
                processNextRequestedClockAppAlarmDismiss();   //  => Fermeture MainActivity => Lancement Clock App (sans pouvoir éviter de quitter SwTimer App) => Revenir à SwTimer App => Réouverture de MainActivity => Init CtRecordsHandler => processNextRequestedClockAppAlarmDismiss() => ... le carrousel continue jusqu'à avoir traité toutes les demandes de suppression d'alarme
            }
        }
    }

    private void onExpiredTimer(CtRecord ctRecord) {
        if (mOnExpiredTimerListener != null) {   //  Faites passer: Timer expiré => Message avertissement
            mOnExpiredTimerListener.onExpiredTimer(ctRecord);
        }
    }

    private int actionOnAll(ACTIONS_ON_ALL action) {
        int count = 0;
        if (!ctRecords.isEmpty()) {
            for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
                if (action.equals(ACTIONS_ON_ALL.UPDATE_TIME)) {
                    ctRecords.get(i).updateTime(nowm);
                    count = count + 1;
                }
                if (action.equals(ACTIONS_ON_ALL.INVERT_SELECTION)) {
                    ctRecords.get(i).setSelectedOn(!ctRecords.get(i).isSelected());
                }
                if (action.equals(ACTIONS_ON_ALL.SELECT)) {
                    ctRecords.get(i).setSelectedOn(true);
                }
                if (action.equals(ACTIONS_ON_ALL.COUNT_CHRONOS)) {
                    if (ctRecords.get(i).getMode().equals(MODES.CHRONO)) {
                        count = count + 1;
                    }
                }
                if (action.equals(ACTIONS_ON_ALL.COUNT_TIMERS)) {
                    if (ctRecords.get(i).getMode().equals(MODES.TIMER)) {
                        count = count + 1;
                    }
                }
                if (action.equals(ACTIONS_ON_ALL.COUNT)) {
                    count = count + 1;
                }
            }
        }
        return count;
    }

    private int actionOnSelection(ACTIONS_ON_SELECTION action) {
        int count = 0;
        if (!ctRecords.isEmpty()) {
            int i = 0;
            do {
                if (ctRecords.get(i).isSelected()) {
                    count = count + 1;   //  Compter
                    if (action.equals(ACTIONS_ON_SELECTION.UPDATE_TIME)) {
                        ctRecords.get(i).updateTime(nowm);
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.START)) {
                        ctRecords.get(i).start(nowm, setClockAppAlarmOnStartTimer);
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.STOP)) {
                        ctRecords.get(i).stop(nowm);
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.SPLIT)) {
                        ctRecords.get(i).split(nowm);
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.RESET)) {
                        ctRecords.get(i).reset();
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.REMOVE)) {
                        ctRecords.get(i).reset();
                        ctRecords.remove(i);  //  Les instances de chaque CtRecord concerné restent cependant intactes et pourront appeler onRequestClockAppAlarmSwitch()
                        if (ctRecords.isEmpty()) {
                            break;   //  Evacuation générale
                        }
                        i = i - 1;   //  Compenser le remove
                    }
                    if (action.equals(ACTIONS_ON_SELECTION.COUNT)) {     //  cf count
                        //  NOP
                    }
                }
                i = i + 1;
            }
            while (i < ctRecords.size());
        }
        return count;
    }

    private int getSelectedRunningTimersWithClockAppAlarm() {
        int count = 0;
        if (!ctRecords.isEmpty()) {
            for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
                if (ctRecords.get(i).getMode().equals(MODES.TIMER)) {
                    if (ctRecords.get(i).isSelected()) {
                        if (ctRecords.get(i).isRunning()) {
                            if (ctRecords.get(i).isClockAppAlarmOn()) {
                                count = count + 1;
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    private void RequestAdditionalClockAppAlarmDismiss(CtRecord ctRecord) {   //  Pas d'appel à dismissClockAppAlarm() (mais processNextRequestedClockAppAlarmDismiss() le fera)
        requestedClockAppAlarmDismisses = requestedClockAppAlarmDismisses + ALARM_SEPARATOR + ctRecord.getLabel() + ALARM_FIELD_SEPARATOR + "Dismissing " + ctRecord.getClockAppAlarmDescription();
    }

    private void processNextRequestedClockAppAlarmDismiss() {   //  Une alarme à la fois, la prochaine est traitée au prochain init() de CtRecordsHandler
        if (!requestedClockAppAlarmDismisses.equals("")) {
            requestedClockAppAlarmDismisses = requestedClockAppAlarmDismisses.substring(ALARM_SEPARATOR.length());
            int nextAlarmIndex = requestedClockAppAlarmDismisses.indexOf(ALARM_SEPARATOR);
            boolean nextAlarmFound = (nextAlarmIndex != NOT_FOUND);
            String content = (nextAlarmFound ? requestedClockAppAlarmDismisses.substring(0, nextAlarmIndex) : requestedClockAppAlarmDismisses);
            requestedClockAppAlarmDismisses = (nextAlarmFound ? requestedClockAppAlarmDismisses.substring(nextAlarmIndex) : "");
            int nextFieldIndex = content.indexOf(ALARM_FIELD_SEPARATOR);   //  ALARM_FIELD_SEPARATOR toujours présent
            String alarmLabel = content.substring(0, nextFieldIndex);
            String alarmDescription = content.substring(nextFieldIndex + ALARM_FIELD_SEPARATOR.length());
            dismissClockAppAlarm(context, alarmLabel, alarmDescription);   //  Lancement obligatoire de Clock App qui désactive l'alarme et que l'utilisateur doit quitter pour revenir dans SwTimer App
        }
    }

    private int getMaxId() {
        int maxId = 0;
        if (!ctRecords.isEmpty()) {
            for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
                if (ctRecords.get(i).getIdct() > maxId) {
                    maxId = ctRecords.get(i).getIdct();
                }
            }
        }
        return maxId;
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

    private void setupCtRecords() {
        ctRecords = chronoTimerRowsToCtRecords(StringDBUtils.getDBChronoTimers(stringDB));
        if (!ctRecords.isEmpty()) {
            for (int i = 0; i <= (ctRecords.size() - 1); i = i + 1) {
                setupCtRecordListener(ctRecords.get(i));
            }
        }
    }

    private void setupCtRecordListener(CtRecord ctRecord) {
        ctRecord.setOnRequestClockAppAlarmSwitchListener(new CtRecord.onRequestClockAppAlarmSwitchListener() {
            @Override
            public void onRequestClockAppAlarmSwitch(CtRecord ctRecord, SWITCHES clockAppAlarmSwitch) {
                CtRecordsHandler.this.onRequestClockAppAlarmSwitch(ctRecord, clockAppAlarmSwitch);
            }
        });
        ctRecord.setOnExpiredTimerListener(new CtRecord.onExpiredTimerListener() {
            @Override
            public void onExpiredTimer(CtRecord ctRecord) {
                CtRecordsHandler.this.onExpiredTimer(ctRecord);
            }
        });
    }

}
