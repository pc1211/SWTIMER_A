package com.example.pgyl.swtimer_a;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;

import com.example.pgyl.pekislib_a.CustomImageButton;
import com.example.pgyl.pekislib_a.HelpActivity;
import com.example.pgyl.pekislib_a.StateView;
import com.example.pgyl.pekislib_a.StringShelfDatabase;

import java.util.EnumMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.pgyl.pekislib_a.Constants.ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.Constants.BUTTON_STATES;
import static com.example.pgyl.pekislib_a.Constants.SHP_FILE_NAME_SUFFIX;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_TITLE;
import static com.example.pgyl.pekislib_a.MiscUtils.beep;
import static com.example.pgyl.pekislib_a.MiscUtils.msgBox;
import static com.example.pgyl.pekislib_a.MiscUtils.toastLong;
import static com.example.pgyl.pekislib_a.StateView.STATES;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.ACTIVITY_START_STATUS;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.createTableActivityInfosIfNotExists;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.getDefaults;
import static com.example.pgyl.pekislib_a.StringShelfDatabaseUtils.tableActivityInfosExists;
import static com.example.pgyl.swtimer_a.CtDisplayActivity.CTDISPLAY_EXTRA_KEYS;
import static com.example.pgyl.swtimer_a.CtRecord.MODE;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.createTableChronoTimersIfNotExists;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.createTableColorsBackScreenIfNotExists;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.createTableColorsButtonsIfNotExists;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.createTableColorsTimeIfNotExists;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.createTableMessagesIfNotExists;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.createTablePresetsCTIfNotExists;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getColorsBackScreenTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getColorsButtonsTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getColorsTimeTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.getMessagesTableName;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.initializeTableColorsBackScreen;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.initializeTableColorsButtons;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.initializeTableColorsTime;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.initializeTableMessages;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.initializeTablePresetsCT;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setCurrentValuesInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.setStartStatusInCtDisplayActivity;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.tableChronoTimersExists;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.tableColorsBackScreenExists;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.tableColorsButtonsExists;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.tableColorsTimeExists;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.tableMessagesExists;
import static com.example.pgyl.swtimer_a.StringShelfDatabaseUtils.tablePresetsCTExists;

public class MainActivity extends Activity {
    //region Constantes
    private enum COMMANDS {
        SHOW_EXPIRATION_TIME(R.drawable.main_clock), ADD_NEW_CHRONOTIMER_TO_LIST(R.drawable.main_tolist), NEW_CHRONO(R.drawable.main_chrono), NEW_TIMER(R.drawable.main_timer), INVERT_SELECTION_ALL_CT(R.drawable.main_inv), SELECT_ALL_CT(R.drawable.main_all), START_SELECTED_CT(R.drawable.main_start), STOP_SELECTED_CT(R.drawable.main_stop), SPLIT_SELECTED_CT(R.drawable.main_split), RESET_SELECTED_CT(R.drawable.main_reset), REMOVE_SELECTED_CT(R.drawable.main_remove);

        private int valueId;

        COMMANDS(int valueId) {
            this.valueId = valueId;
        }

        public int ID() {
            return valueId;
        }

        public int INDEX() {
            return ordinal();
        }
    }

    public enum SWTIMER_SHP_KEY_NAMES {SHOW_EXPIRATION_TIME, ADD_NEW_CHRONOTIMER_TO_LIST, SET_CLOCK_APP_ALARM_ON_START_TIMER, KEEP_SCREEN, REQUESTED_CLOCK_APP_ALARM_DISMISSES}

    //endregion
    //region Variables
    private CustomImageButton[] buttons;
    private EnumMap<COMMANDS, StateView> commandStateViewsMap;
    private Menu menu;
    private MenuItem barMenuItemSetClockAppAlarmOnStartTimer;
    private MenuItem barMenuItemKeepScreen;
    private CtRecordsHandler ctRecordsHandler;
    private MainCtListUpdater mainCtListUpdater;
    private boolean showExpirationTime;
    private boolean addNewChronoTimerToList;
    private boolean setClockAppAlarmOnStartTimer;
    private boolean keepScreen;
    private ListView mainCtListView;
    private MainCtListItemAdapter mainCtListItemAdapter;
    private StringShelfDatabase stringShelfDatabase;
    private String shpFileName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String ACTIVITY_TITLE = "Swtimer";

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().setTitle(ACTIVITY_TITLE);
        setContentView(R.layout.main);
        setupButtons();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mainCtListUpdater.stopAutomatic();
        mainCtListUpdater.close();
        mainCtListUpdater = null;
        mainCtListItemAdapter.close();
        mainCtListItemAdapter = null;
        ctRecordsHandler.saveAndclose();
        ctRecordsHandler = null;
        stringShelfDatabase.close();
        stringShelfDatabase = null;
        commandStateViewsMap.clear();
        commandStateViewsMap = null;
        menu = null;
        savePreferences();
    }
    //endregion

    @Override
    protected void onResume() {
        super.onResume();

        shpFileName = getPackageName() + SHP_FILE_NAME_SUFFIX;   //  Sans nom d'activité car sera partagé avec CtDisplayActivity
        keepScreen = getSHPKeepScreen();
        setupCommandStateViewsMap();
        setupStringShelfDatabase();
        setupCtRecordsHandler();
        setupMainCtList();
        setupMainCtListUpdater();
        setupShowExpirationTime();
        setupSetClockAppAlarmOnStartTimer();
        setupAddNewChronoTimerToList();
        setupButtonColors();

        updateDisplayButtonColors();
        updateDisplayStateColors();
        updateDisplayKeepScreen();
        sortAndReloadMainCtList();
        mainCtListUpdater.startAutomatic();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {  //  Non appelé après changement d'orientation
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        setupBarMenuItems();
        updateDisplaySetClockAppAlarmOnStartTimerBarMenuItemIcon(setClockAppAlarmOnStartTimer);
        updateDisplayKeepScreenBarMenuItemIcon(keepScreen);
        return true;
    }
    //endregion

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {  // appelé par invalideOptionsMenu après changement d'orientation
        updateDisplaySetClockAppAlarmOnStartTimerBarMenuItemIcon(setClockAppAlarmOnStartTimer);
        updateDisplayKeepScreenBarMenuItemIcon(keepScreen);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.HELP) {
            launchHelpActivity();
            return true;
        }
        if (item.getItemId() == R.id.ABOUT) {
            msgBox("Version: " + BuildConfig.VERSION_NAME, this);
            return true;
        }
        if (item.getItemId() == R.id.BAR_MENU_ITEM_SET_CLOCK_APP_ALARM_ON_START_TIMER) {
            setClockAppAlarmOnStartTimer = !setClockAppAlarmOnStartTimer;
            updateDisplaySetClockAppAlarmOnStartTimerBarMenuItemIcon(setClockAppAlarmOnStartTimer);
            mainCtListItemAdapter.setClockAppAlarmOnStartTimer(setClockAppAlarmOnStartTimer);
        }
        if (item.getItemId() == R.id.BAR_MENU_ITEM_KEEP_SCREEN) {
            keepScreen = !keepScreen;
            updateDisplayKeepScreen();
            updateDisplayKeepScreenBarMenuItemIcon(keepScreen);
        }
        return super.onOptionsItemSelected(item);
    }

    private void onButtonClick(COMMANDS command) {
        long nowm = System.currentTimeMillis();
        if (command.equals(COMMANDS.NEW_CHRONO)) {
            onButtonClickAddNewChrono();
        }
        if (command.equals(COMMANDS.NEW_TIMER)) {
            onButtonClickAddNewTimer();
        }
        if (command.equals(COMMANDS.SHOW_EXPIRATION_TIME)) {
            onButtonClickShowExpirationTime();
        }
        if (command.equals(COMMANDS.ADD_NEW_CHRONOTIMER_TO_LIST)) {
            onButtonClickAddNewChronoTimerToList();
        }
        if ((command.equals(COMMANDS.INVERT_SELECTION_ALL_CT)) || (command.equals(COMMANDS.SELECT_ALL_CT))) {
            onButtonClickActionOnAll(command);
        }
        if ((command.equals(COMMANDS.START_SELECTED_CT)) || (command.equals(COMMANDS.STOP_SELECTED_CT)) || (command.equals(COMMANDS.SPLIT_SELECTED_CT)) || (command.equals(COMMANDS.RESET_SELECTED_CT)) || (command.equals(COMMANDS.REMOVE_SELECTED_CT))) {
            onButtonClickActionOnSelection(command, nowm);
        }
    }

    private void onButtonClickActionOnAll(COMMANDS command) {
        if (ctRecordsHandler.getCountAll() >= 1) {
            mainCtListUpdater.stopAutomatic();
            if (command.equals(COMMANDS.INVERT_SELECTION_ALL_CT)) {
                ctRecordsHandler.invertSelectionAll();
            }
            if (command.equals(COMMANDS.SELECT_ALL_CT)) {
                ctRecordsHandler.selectAll();
            }
            mainCtListUpdater.update();
            mainCtListUpdater.startAutomatic();
        } else {
            toastLong("The list must contain at least one Chrono or Timer", this);
        }
    }

    private void onButtonClickActionOnSelection(COMMANDS command, long nowm) {
        if (ctRecordsHandler.getCountSelection() >= 1) {
            mainCtListUpdater.stopAutomatic();
            if (command.equals(COMMANDS.START_SELECTED_CT)) {
                ctRecordsHandler.startSelection(nowm, setClockAppAlarmOnStartTimer);
            }
            if (command.equals(COMMANDS.STOP_SELECTED_CT)) {
                ctRecordsHandler.stopSelection(nowm);
            }
            if (command.equals(COMMANDS.SPLIT_SELECTED_CT)) {
                ctRecordsHandler.splitSelection(nowm);
            }
            if (command.equals(COMMANDS.RESET_SELECTED_CT)) {
                ctRecordsHandler.resetSelection();
            }
            if (command.equals(COMMANDS.REMOVE_SELECTED_CT)) {
                removeSelection();
            }
            sortAndReloadMainCtList();
            mainCtListUpdater.startAutomatic();
        } else {
            toastLong("The list must contain at least one selected Chrono or Timer", this);
        }
    }

    private void onButtonClickAddNewChrono() {
        createChronoTimer(MODE.CHRONO);
    }

    private void onButtonClickAddNewTimer() {
        createChronoTimer(MODE.TIMER);
    }

    private void onButtonClickShowExpirationTime() {
        showExpirationTime = !showExpirationTime;
        setState(COMMANDS.SHOW_EXPIRATION_TIME, showExpirationTime);
        mainCtListItemAdapter.setShowExpirationTime(showExpirationTime);
        updateDisplayStateColor(COMMANDS.SHOW_EXPIRATION_TIME);
        mainCtListUpdater.update();
    }

    private void onButtonClickAddNewChronoTimerToList() {
        addNewChronoTimerToList = !addNewChronoTimerToList;
        setState(COMMANDS.ADD_NEW_CHRONOTIMER_TO_LIST, addNewChronoTimerToList);
        updateDisplayStateColor(COMMANDS.ADD_NEW_CHRONOTIMER_TO_LIST);
    }

    private void onCtListExpiredTimers() {
        mainCtListUpdater.stopAutomatic();
        sortAndReloadMainCtList();
        mainCtListUpdater.startAutomatic();
        beep(this);
    }

    private void onCtListItemButtonClick() {
        mainCtListUpdater.stopAutomatic();
        sortAndReloadMainCtList();
        mainCtListUpdater.startAutomatic();
    }

    private void sortAndReloadMainCtList() {
        long nowm = System.currentTimeMillis();
        ctRecordsHandler.updateTimeAll(nowm);
        ctRecordsHandler.sortCtRecords();
        mainCtListUpdater.reload();
    }

    private void updateDisplayStateColor(COMMANDS command) {
        commandStateViewsMap.get(command).invalidate();
    }

    private void updateDisplayStateColors() {
        for (COMMANDS command : COMMANDS.values()) {
            if ((command.equals(COMMANDS.ADD_NEW_CHRONOTIMER_TO_LIST)) || (command.equals(COMMANDS.SHOW_EXPIRATION_TIME))) {
                updateDisplayStateColor(command);
            }
        }
    }

    private void updateDisplayButtonColors() {
        for (final COMMANDS command : COMMANDS.values()) {
            buttons[command.INDEX()].updateColor();
        }
    }

    private void updateDisplaySetClockAppAlarmOnStartTimerBarMenuItemIcon(boolean setClockAppAlarmOnStartTimer) {
        barMenuItemSetClockAppAlarmOnStartTimer.setIcon((setClockAppAlarmOnStartTimer ? R.drawable.main_bell_start_on : R.drawable.main_bell_start_off));
    }

    private void updateDisplayKeepScreenBarMenuItemIcon(boolean keepScreen) {
        barMenuItemKeepScreen.setIcon((keepScreen ? R.drawable.main_light_on : R.drawable.main_light_off));
    }

    private void updateDisplayKeepScreen() {
        if (keepScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void removeSelection() {
        String s = String.valueOf(ctRecordsHandler.getCountSelection());
        if (ctRecordsHandler.getCountSelection() == ctRecordsHandler.getCountAll()) {
            s = "All";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove " + s + " Chronos/Timers");
        builder.setMessage("Are you sure ?");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int id) {
                ctRecordsHandler.removeSelection();
                sortAndReloadMainCtList();
            }
        });
        builder.setNegativeButton("No", null);
        Dialog dialog = builder.create();
        dialog.show();
    }

    private void createChronoTimer(MODE mode) {
        mainCtListUpdater.stopAutomatic();
        int idct = ctRecordsHandler.createChronoTimer(mode);
        if (addNewChronoTimerToList) {
            sortAndReloadMainCtList();
            mainCtListUpdater.startAutomatic();
        } else {
            launchCtDisplayActivity(idct);
        }
    }

    private void setState(COMMANDS command, boolean value) {
        commandStateViewsMap.get(command).setState(value ? STATES.ON : STATES.OFF);
    }

    private void savePreferences() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        SharedPreferences.Editor shpEditor = shp.edit();
        shpEditor.putBoolean(SWTIMER_SHP_KEY_NAMES.SHOW_EXPIRATION_TIME.toString(), showExpirationTime);
        shpEditor.putBoolean(SWTIMER_SHP_KEY_NAMES.ADD_NEW_CHRONOTIMER_TO_LIST.toString(), addNewChronoTimerToList);
        shpEditor.putBoolean(SWTIMER_SHP_KEY_NAMES.SET_CLOCK_APP_ALARM_ON_START_TIMER.toString(), setClockAppAlarmOnStartTimer);
        shpEditor.putBoolean(SWTIMER_SHP_KEY_NAMES.KEEP_SCREEN.toString(), keepScreen);
        shpEditor.commit();
    }

    private boolean getSHPShowExpirationTime() {
        final boolean SHOW_EXPIRATION_TIME_DEFAULT_VALUE = false;

        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getBoolean(SWTIMER_SHP_KEY_NAMES.SHOW_EXPIRATION_TIME.toString(), SHOW_EXPIRATION_TIME_DEFAULT_VALUE);
    }

    private boolean getSHPaddNewChronoTimerToList() {
        final boolean ADD_NEW_CHRONOTIMER_TO_LIST_DEFAULT_VALUE = false;

        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getBoolean(SWTIMER_SHP_KEY_NAMES.ADD_NEW_CHRONOTIMER_TO_LIST.toString(), ADD_NEW_CHRONOTIMER_TO_LIST_DEFAULT_VALUE);
    }

    private boolean getSHPSetClockAppAlarmOnStartTimer() {
        final boolean SET_CLOCK_APP_ALARM_ON_START_TIMER_DEFAULT_VALUE = true;

        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getBoolean(SWTIMER_SHP_KEY_NAMES.SET_CLOCK_APP_ALARM_ON_START_TIMER.toString(), SET_CLOCK_APP_ALARM_ON_START_TIMER_DEFAULT_VALUE);
    }

    private boolean getSHPKeepScreen() {
        final boolean KEEP_SCREEN_DEFAULT_VALUE = false;

        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getBoolean(SWTIMER_SHP_KEY_NAMES.KEEP_SCREEN.toString(), KEEP_SCREEN_DEFAULT_VALUE);
    }

    private void setupButtons() {
        final String BUTTON_COMMAND_XML_PREFIX = "BTN_";
        final long BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;

        buttons = new CustomImageButton[COMMANDS.values().length];
        Class rid = R.id.class;
        for (COMMANDS command : COMMANDS.values())
            try {
                buttons[command.INDEX()] = findViewById(rid.getField(BUTTON_COMMAND_XML_PREFIX + command.toString()).getInt(rid));
                buttons[command.INDEX()].setImageResource(command.ID());
                if ((!command.equals(COMMANDS.START_SELECTED_CT)) && (!command.equals(COMMANDS.STOP_SELECTED_CT))) {   //  Start et stop doivent pouvoir cliquer sans délai
                    buttons[command.INDEX()].setMinClickTimeInterval(BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
                }
                final COMMANDS fcommand = command;
                buttons[command.INDEX()].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onButtonClick(fcommand);
                    }
                });
            } catch (IllegalAccessException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    private void setupCommandStateViewsMap() {
        final String BUTTON_STATE_XML_PREFIX = "STATE_";

        Class rid = R.id.class;
        commandStateViewsMap = new EnumMap<COMMANDS, StateView>(COMMANDS.class);
        for (COMMANDS command : COMMANDS.values())
            try {
                StateView stateView = findViewById(rid.getField(BUTTON_STATE_XML_PREFIX + command.toString()).getInt(rid));
                if (stateView != null) {   //  Les boutons n'ont pas tous une StateView
                    commandStateViewsMap.put(command, stateView);
                }
            } catch (IllegalAccessException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchFieldException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SecurityException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    private void setupCtRecordsHandler() {
        ctRecordsHandler = new CtRecordsHandler(this, stringShelfDatabase);
    }

    private void setupStringShelfDatabase() {
        stringShelfDatabase = new StringShelfDatabase(this);
        stringShelfDatabase.open();
        if (!tableActivityInfosExists(stringShelfDatabase)) {
            createTableActivityInfosIfNotExists(stringShelfDatabase);
        }
        if (!tableColorsTimeExists(stringShelfDatabase)) {
            createTableColorsTimeIfNotExists(stringShelfDatabase);
            initializeTableColorsTime(stringShelfDatabase);
            setCurrentValuesInCtDisplayActivity(stringShelfDatabase, getColorsTimeTableName(), getDefaults(stringShelfDatabase, getColorsTimeTableName()));
        }
        if (!tableColorsButtonsExists(stringShelfDatabase)) {
            createTableColorsButtonsIfNotExists(stringShelfDatabase);
            initializeTableColorsButtons(stringShelfDatabase);
            setCurrentValuesInCtDisplayActivity(stringShelfDatabase, getColorsButtonsTableName(), getDefaults(stringShelfDatabase, getColorsButtonsTableName()));
        }
        if (!tableColorsBackScreenExists(stringShelfDatabase)) {
            createTableColorsBackScreenIfNotExists(stringShelfDatabase);
            initializeTableColorsBackScreen(stringShelfDatabase);
            setCurrentValuesInCtDisplayActivity(stringShelfDatabase, getColorsBackScreenTableName(), getDefaults(stringShelfDatabase, getColorsBackScreenTableName()));
        }
        if (!tablePresetsCTExists(stringShelfDatabase)) {
            createTablePresetsCTIfNotExists(stringShelfDatabase);
            initializeTablePresetsCT(stringShelfDatabase);
        }
        if (!tableChronoTimersExists(stringShelfDatabase)) {
            createTableChronoTimersIfNotExists(stringShelfDatabase);
        }
        if (!tableMessagesExists(stringShelfDatabase)) {
            createTableMessagesIfNotExists(stringShelfDatabase);
            initializeTableMessages(stringShelfDatabase);
            setCurrentValuesInCtDisplayActivity(stringShelfDatabase, getMessagesTableName(), getDefaults(stringShelfDatabase, getMessagesTableName()));
        }
    }

    private void setupMainCtList() {
        mainCtListItemAdapter = new MainCtListItemAdapter(this, stringShelfDatabase);
        mainCtListItemAdapter.setOnItemButtonClick(new MainCtListItemAdapter.onButtonClickListener() {
            @Override
            public void onButtonClick() {
                onCtListItemButtonClick();
            }
        });
        mainCtListView = findViewById(R.id.CT_LIST);
        mainCtListView.setAdapter(mainCtListItemAdapter);
    }

    private void setupMainCtListUpdater() {
        mainCtListUpdater = new MainCtListUpdater(mainCtListView, ctRecordsHandler);
        mainCtListUpdater.setOnExpiredTimersListener(new MainCtListUpdater.onExpiredTimersListener() {
            @Override
            public void onExpiredTimers() {
                onCtListExpiredTimers();
            }
        });
    }

    private void setupShowExpirationTime() {
        showExpirationTime = getSHPShowExpirationTime();
        setState(COMMANDS.SHOW_EXPIRATION_TIME, showExpirationTime);
        mainCtListItemAdapter.setShowExpirationTime(showExpirationTime);
    }

    private void setupSetClockAppAlarmOnStartTimer() {
        setClockAppAlarmOnStartTimer = getSHPSetClockAppAlarmOnStartTimer();
        mainCtListItemAdapter.setClockAppAlarmOnStartTimer(setClockAppAlarmOnStartTimer);
    }

    private void setupAddNewChronoTimerToList() {
        addNewChronoTimerToList = getSHPaddNewChronoTimerToList();
        setState(COMMANDS.ADD_NEW_CHRONOTIMER_TO_LIST, addNewChronoTimerToList);
    }

    private void setupButtonColors() {
        final String NEW_CHRONO_TIMER_UNPRESSED_COLOR_DEFAULT = "668CFF";
        final String NEW_CHRONO_TIMER_PRESSED_COLOR_DEFAULT = "0040FF";

        for (final COMMANDS command : COMMANDS.values()) {
            boolean needSpecialColor = (command.equals(COMMANDS.NEW_CHRONO) || (command.equals(COMMANDS.NEW_TIMER)));
            buttons[command.INDEX()].setUnpressedColor(((needSpecialColor) ? NEW_CHRONO_TIMER_UNPRESSED_COLOR_DEFAULT : BUTTON_STATES.UNPRESSED.DEFAULT_COLOR()));
            buttons[command.INDEX()].setPressedColor(((needSpecialColor) ? NEW_CHRONO_TIMER_PRESSED_COLOR_DEFAULT : BUTTON_STATES.PRESSED.DEFAULT_COLOR()));
        }
    }

    private void setupBarMenuItems() {
        final String BAR_MENU_ITEM_SET_CLOCK_APP_ALARM_ON_START_TIMER_NAME = "BAR_MENU_ITEM_SET_CLOCK_APP_ALARM_ON_START_TIMER";
        final String BAR_MENU_ITEM_KEEP_SCREEN_NAME = "BAR_MENU_ITEM_KEEP_SCREEN";

        Class rid = R.id.class;
        try {
            barMenuItemSetClockAppAlarmOnStartTimer = menu.findItem(rid.getField(BAR_MENU_ITEM_SET_CLOCK_APP_ALARM_ON_START_TIMER_NAME).getInt(rid));
            barMenuItemKeepScreen = menu.findItem(rid.getField(BAR_MENU_ITEM_KEEP_SCREEN_NAME).getInt(rid));
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void launchHelpActivity() {
        Intent callingIntent = new Intent(this, HelpActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), HELP_ACTIVITY_TITLE);
        callingIntent.putExtra(HELP_ACTIVITY_EXTRA_KEYS.HTML_ID.toString(), R.raw.helpmainactivity);
        startActivity(callingIntent);
    }

    private void launchCtDisplayActivity(int idct) {
        setStartStatusInCtDisplayActivity(stringShelfDatabase, ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, CtDisplayActivity.class);
        callingIntent.putExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), idct);
        startActivity(callingIntent);
    }

}