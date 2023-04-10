package com.example.pgyl.mind_a;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.example.pgyl.pekislib_a.CustomImageButton;
import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.HelpActivity;
import com.example.pgyl.pekislib_a.PresetsHandler;
import com.example.pgyl.pekislib_a.StringDB;
import com.example.pgyl.pekislib_a.SymbolButtonView;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.pgyl.pekislib_a.Constants.ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.Constants.SHP_FILE_NAME_SUFFIX;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_TITLE;
import static com.example.pgyl.pekislib_a.MiscUtils.msgBox;
import static com.example.pgyl.pekislib_a.MiscUtils.toastLong;
import static com.example.pgyl.pekislib_a.StringDBTables.ACTIVITY_START_STATUS;
import static com.example.pgyl.pekislib_a.StringDBTables.getActivityInfosTableName;
import static com.example.pgyl.pekislib_a.StringDBUtils.createPekislibTableIfNotExists;
import static com.example.pgyl.pekislib_a.StringDBUtils.getDefaults;
import static com.example.pgyl.pekislib_a.StringDBUtils.setCurrentsForActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.setStartStatusOfActivity;
import static com.example.pgyl.mind_a.Constants.SWTIMER_ACTIVITIES;
import static com.example.pgyl.mind_a.CtDisplayActivity.CTDISPLAY_EXTRA_KEYS;
import static com.example.pgyl.mind_a.CtRecord.MODES;
import static com.example.pgyl.mind_a.MainCtListItemAdapter.onCheckBoxClickListener;
import static com.example.pgyl.mind_a.StringDBTables.getBackScreenColorsTableName;
import static com.example.pgyl.mind_a.StringDBTables.getChronoTimersTableName;
import static com.example.pgyl.mind_a.StringDBTables.getDotMatrixDisplayCoeffsTableName;
import static com.example.pgyl.mind_a.StringDBTables.getDotMatrixDisplayColorsTableName;
import static com.example.pgyl.mind_a.StringDBTables.getPresetsCTTableName;
import static com.example.pgyl.mind_a.StringDBTables.getStateButtonsColorsTableName;
import static com.example.pgyl.mind_a.StringDBUtils.createSwtimerTableIfNotExists;
import static com.example.pgyl.mind_a.StringDBUtils.initializeTableBackScreenColors;
import static com.example.pgyl.mind_a.StringDBUtils.initializeTableDotMatrixDisplayCoeffs;
import static com.example.pgyl.mind_a.StringDBUtils.initializeTableDotMatrixDisplayColors;
import static com.example.pgyl.mind_a.StringDBUtils.initializeTablePresetsCT;
import static com.example.pgyl.mind_a.StringDBUtils.initializeTableStateButtonsColors;

//  MainActivity fait appel à CtRecordShandler pour la gestion des CtRecord (création, suppression, tri, écoute des événements, ...) grâce aux boutons de contrôle agissant sur la sélection des items de la liste, ...
//  MainCtListUpdater maintient la liste de MainActivity (rafraîchissement, scrollbar, ...), fait appel à MainCtListAdapter (pour gérer chaque item) et également à CtRecordShandler (pour leur mise à jour)
//  MainCtListItemAdapter reçoit ses items (CtRecord) de la part de MainCtListUpdater et gère chaque item de la liste (avec ses boutons de contrôle)
//  CtRecordsHandler reçoit les événements onExpiredTimer() des CtRecord (et les relaie à MainCtListUpdater), et aussi leurs onRequestClockAppAlarmSwitch() pour la création/suppression d'alarmes dans Clock App
//  Si un item de liste génère un onExpiredTimer(), MainCtListUpdater le signalera à l'utilisateur
public class MainActivity extends Activity {
    //region Constantes
    private enum COMMANDS {
        NEW_CHRONO(R.drawable.main_chrono), NEW_TIMER(R.drawable.main_timer), INVERT_SELECTION_ALL_CT(R.drawable.main_inv), SELECT_ALL_CT(R.drawable.main_all), START_SELECTED_CT(R.drawable.main_start), STOP_SELECTED_CT(R.drawable.main_stop), SPLIT_SELECTED_CT(R.drawable.main_split), RESET_SELECTED_CT(R.drawable.main_reset), REMOVE_SELECTED_CT(R.drawable.main_remove);

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

    private enum STATE_COMMANDS {
        SHOW_EXPIRATION_TIME(R.raw.main_clock), ADD_NEW_CHRONOTIMER_TO_LIST(R.raw.main_tolist);

        private int valueId;

        STATE_COMMANDS(int valueId) {
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
    private LinearLayout layoutButtonsOnSelection;
    private LinearLayout layoutDotMatrixDisplay;
    private CustomImageButton[] buttons;
    private SymbolButtonView[] stateButtons;
    private DotMatrixDisplayView dotMatrixDisplayView;
    private MainDotMatrixDisplayUpdater dotMatrixDisplayUpdater;
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
    private StringDB stringDB;
    private String shpFileName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String ACTIVITY_TITLE = "Swtimer";

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().setTitle(ACTIVITY_TITLE);
        setContentView(R.layout.main);
        setupButtons();
        setupStateButtons();
        setupDotMatrixDisplay();
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
        dotMatrixDisplayUpdater.close();
        dotMatrixDisplayUpdater = null;
        stringDB.close();
        stringDB = null;
        menu = null;
        savePreferences();
    }
    //endregion

    @Override
    protected void onResume() {
        super.onResume();

        shpFileName = getPackageName() + SHP_FILE_NAME_SUFFIX;   //  Sans nom d'activité car sera partagé avec CtDisplayActivity
        keepScreen = getSHPKeepScreen();
        setupStringDB();
        setupCtRecordsHandler();
        setupMainCtList();
        setupMainCtListUpdater();
        setupDotMatrixDisplayUpdater();
        setupShowExpirationTime();
        setupSetClockAppAlarmOnStartTimer();
        setupAddNewChronoTimerToList();
        setupButtonSpecialColors();

        updateDisplayStateButtonColors();
        updateDisplayKeepScreen();
        mainCtListUpdater.reload();
        mainCtListUpdater.startAutomatic(System.currentTimeMillis(), 0);
        updateDisplayButtonsAndDotMatrixDisplayVisibility();
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
            PackageInfo pInfo = null;
            try {
                pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            String version = pInfo.versionName;//Version Name
            int verCode = pInfo.versionCode;//Version Code

            msgBox("Version: " + version, this);
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
        if ((command.equals(COMMANDS.INVERT_SELECTION_ALL_CT)) || (command.equals(COMMANDS.SELECT_ALL_CT))) {
            onButtonClickActionOnAll(command, nowm);
        }
        if ((command.equals(COMMANDS.START_SELECTED_CT)) || (command.equals(COMMANDS.STOP_SELECTED_CT)) || (command.equals(COMMANDS.SPLIT_SELECTED_CT)) || (command.equals(COMMANDS.RESET_SELECTED_CT)) || (command.equals(COMMANDS.REMOVE_SELECTED_CT))) {
            onButtonClickActionOnSelection(command, nowm);
        }
    }

    private void onStateButtonClick(STATE_COMMANDS stateCommand) {
        long nowm = System.currentTimeMillis();
        if (stateCommand.equals(STATE_COMMANDS.SHOW_EXPIRATION_TIME)) {
            onStateButtonClickShowExpirationTime(nowm);
        }
        if (stateCommand.equals(STATE_COMMANDS.ADD_NEW_CHRONOTIMER_TO_LIST)) {
            onStateButtonClickAddNewChronoTimerToList();
        }
    }

    private void onButtonClickActionOnAll(COMMANDS command, long nowm) {
        if (ctRecordsHandler.getCountAll() >= 1) {
            if (command.equals(COMMANDS.INVERT_SELECTION_ALL_CT)) {
                ctRecordsHandler.invertSelectionAll();
            }
            if (command.equals(COMMANDS.SELECT_ALL_CT)) {
                ctRecordsHandler.selectAll();
            }
            updateDisplayButtonsAndDotMatrixDisplayVisibility();
            mainCtListUpdater.repaint(nowm);
        } else {
            toastLong("The list must contain at least one Chrono or Timer", this);
        }
    }

    private void onButtonClickActionOnSelection(COMMANDS command, long nowm) {
        if (ctRecordsHandler.getCountSelection() >= 1) {
            if (command.equals(COMMANDS.REMOVE_SELECTED_CT)) {
                removeSelection();
            } else {   //  Pas Remove Selection
                if (command.equals(COMMANDS.SPLIT_SELECTED_CT)) {
                    ctRecordsHandler.splitSelection(nowm);
                } else {   //  Pas Split
                    mainCtListUpdater.stopAutomatic();
                    if (command.equals(COMMANDS.START_SELECTED_CT)) {
                        ctRecordsHandler.startSelection(nowm, setClockAppAlarmOnStartTimer);
                    }
                    if (command.equals(COMMANDS.STOP_SELECTED_CT)) {
                        ctRecordsHandler.stopSelection(nowm);
                    }
                    if (command.equals(COMMANDS.RESET_SELECTED_CT)) {
                        ctRecordsHandler.resetSelection();
                    }
                    if (ctRecordsHandler.getCountAllRunning() > 0) {
                        mainCtListUpdater.startAutomatic(nowm, 0);
                    }
                }
                mainCtListUpdater.repaint(nowm);
            }
        } else {
            toastLong("The list must contain at least one selected Chrono or Timer", this);
        }
    }

    private void onButtonClickAddNewChrono() {
        createChronoTimer(MODES.CHRONO);
    }

    private void onButtonClickAddNewTimer() {
        createChronoTimer(MODES.TIMER);
    }

    private void onStateButtonClickShowExpirationTime(long nowm) {
        showExpirationTime = !showExpirationTime;
        mainCtListItemAdapter.setShowExpirationTime(showExpirationTime);
        mainCtListUpdater.repaint(nowm);
        updateDisplayStateButtonColor(STATE_COMMANDS.SHOW_EXPIRATION_TIME);
    }

    private void onStateButtonClickAddNewChronoTimerToList() {
        addNewChronoTimerToList = !addNewChronoTimerToList;
        updateDisplayStateButtonColor(STATE_COMMANDS.ADD_NEW_CHRONOTIMER_TO_LIST);
    }

    private void onCtListItemCheckBoxClick() {
        updateDisplayButtonsAndDotMatrixDisplayVisibility();
    }

    private void onCtListItemStartStopResetClick(long nowm, long timeAcc) {
        mainCtListUpdater.stopAutomatic();
        if (ctRecordsHandler.getCountAllRunning() > 0) {
            mainCtListUpdater.startAutomatic(nowm, timeAcc);
        }
    }

    private void updateDisplayButtonsAndDotMatrixDisplayVisibility() {
        if (ctRecordsHandler.getCountAll() >= 1) {  //  Il y a au moins un chrono/timer dans la liste
            if (ctRecordsHandler.getCountSelection() >= 1) {  //  Au moins un chrono/timer est sélectionné
                setSecondRowLayoutVisible(layoutButtonsOnSelection);  //  Pour voir les boutons pouvant agir sur les chrono/timers sélectionnés et cacher le panneau d'affichage
            } else {   //  Aucun chrono/timer n'est sélectionné
                dotMatrixDisplayUpdater.displayEmptySelection();
                setSecondRowLayoutVisible(layoutDotMatrixDisplay);  //  Pour voir le panneau d'affichage et cacher les boutons pouvant agir sur les chrono/timers sélectionnés
            }
            buttons[COMMANDS.INVERT_SELECTION_ALL_CT.INDEX()].setVisibility(View.VISIBLE);   //  Pour voir les boutons pouvant agir sur la sélection des chrono/timers de la liste
            buttons[COMMANDS.SELECT_ALL_CT.INDEX()].setVisibility(View.VISIBLE);
        } else {  //  La liste est vide
            dotMatrixDisplayUpdater.displayEmptyList();
            setSecondRowLayoutVisible(layoutDotMatrixDisplay);   //  Pour voir le panneau d'affichage et cacher les boutons pouvant agir sur les chrono/timers sélectionnés
            buttons[COMMANDS.INVERT_SELECTION_ALL_CT.INDEX()].setVisibility(View.INVISIBLE);   //  Cacher les boutons non pertinents en cas de liste vide
            buttons[COMMANDS.SELECT_ALL_CT.INDEX()].setVisibility(View.INVISIBLE);
        }
        stateButtons[STATE_COMMANDS.SHOW_EXPIRATION_TIME.INDEX()].setVisibility((ctRecordsHandler.getCountAllTimers() >= 1) ? View.VISIBLE : View.INVISIBLE);  //  Pour voir le bouton montrant l'heure d'expiration du timer ou sinon le temps restant
    }

    private void updateDisplayStateButtonColor(STATE_COMMANDS stateCommand) {
        final String COLOR_1 = "FF0000";
        final String COLOR_2 = "668CFF";
        final String COLOR_3 = "404040";
        final String COLOR_4 = "000000";

        String stateButtonOnColor = ((stateCommand.equals(STATE_COMMANDS.ADD_NEW_CHRONOTIMER_TO_LIST)) ? COLOR_2 : COLOR_1);
        boolean b = getStateButtonState(stateCommand);
        String unpressedFrontColor = (b ? stateButtonOnColor : COLOR_3);
        stateButtons[stateCommand.INDEX()].setColors(unpressedFrontColor, COLOR_4, COLOR_4, unpressedFrontColor);
    }

    private void updateDisplayStateButtonColors() {
        for (STATE_COMMANDS stateCommand : STATE_COMMANDS.values()) {
            updateDisplayStateButtonColor(stateCommand);
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
                mainCtListUpdater.stopAutomatic();
                ctRecordsHandler.removeSelection();
                mainCtListUpdater.reload();
                if (ctRecordsHandler.getCountAllRunning() > 0) {
                    mainCtListUpdater.startAutomatic(System.currentTimeMillis(), 0);
                }
                updateDisplayButtonsAndDotMatrixDisplayVisibility();
            }
        });
        builder.setNegativeButton("No", null);
        Dialog dialog = builder.create();
        dialog.show();
    }

    private void createChronoTimer(MODES mode) {
        mainCtListUpdater.stopAutomatic();   //  C'est plus prudent
        int idct = ctRecordsHandler.createChronoTimer(mode);
        if (addNewChronoTimerToList) {
            mainCtListUpdater.reload();
            if (ctRecordsHandler.getCountAllRunning() > 0) {
                mainCtListUpdater.startAutomatic(System.currentTimeMillis(), 0);
            }
            updateDisplayButtonsAndDotMatrixDisplayVisibility();
        } else {
            launchCtDisplayActivity(idct);
        }
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
        final boolean SET_CLOCK_APP_ALARM_ON_START_TIMER_DEFAULT_VALUE = false;

        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getBoolean(SWTIMER_SHP_KEY_NAMES.SET_CLOCK_APP_ALARM_ON_START_TIMER.toString(), SET_CLOCK_APP_ALARM_ON_START_TIMER_DEFAULT_VALUE);
    }

    private boolean getSHPKeepScreen() {
        final boolean KEEP_SCREEN_DEFAULT_VALUE = false;

        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getBoolean(SWTIMER_SHP_KEY_NAMES.KEEP_SCREEN.toString(), KEEP_SCREEN_DEFAULT_VALUE);
    }

    private void setSecondRowLayoutVisible(LinearLayout linearLayout) {
        if (linearLayout.getId() == R.id.LAY_DOT_MATRIX_DISPLAY) {
            layoutDotMatrixDisplay.setVisibility(View.VISIBLE);
            layoutButtonsOnSelection.setVisibility(View.INVISIBLE);
        }
        if (linearLayout.getId() == R.id.LAY_BUTTONS_ON_SELECTION) {
            layoutButtonsOnSelection.setVisibility(View.VISIBLE);
            layoutDotMatrixDisplay.setVisibility(View.INVISIBLE);
        }
    }

    private boolean getStateButtonState(STATE_COMMANDS command) {
        if (command.equals(STATE_COMMANDS.SHOW_EXPIRATION_TIME)) {
            return showExpirationTime;
        }
        if (command.equals(STATE_COMMANDS.ADD_NEW_CHRONOTIMER_TO_LIST)) {
            return addNewChronoTimerToList;
        }
        return false;
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
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            }
        layoutButtonsOnSelection = findViewById(R.id.LAY_BUTTONS_ON_SELECTION);
    }

    private void setupStateButtons() {
        final String STATE_BUTTON_COMMAND_XML_PREFIX = "STATE_BTN_";
        final float STATE_BUTTON_SYMBOL_SIZE_COEFF = 0.75f;   //  Pour que le symbole ne frôle pas les bords de sa View
        final long STATE_BUTTON_MIN_CLICK_TIME_INTERVAL_MS = 500;

        stateButtons = new SymbolButtonView[STATE_COMMANDS.values().length];
        Class rid = R.id.class;
        for (STATE_COMMANDS stateCommand : STATE_COMMANDS.values()) {
            try {
                stateButtons[stateCommand.INDEX()] = findViewById(rid.getField(STATE_BUTTON_COMMAND_XML_PREFIX + stateCommand.toString()).getInt(rid));
                stateButtons[stateCommand.INDEX()].setSymbolSizeCoeff(STATE_BUTTON_SYMBOL_SIZE_COEFF);
                stateButtons[stateCommand.INDEX()].setSVGImageResource(stateCommand.ID());
                stateButtons[stateCommand.INDEX()].setMinClickTimeInterval(STATE_BUTTON_MIN_CLICK_TIME_INTERVAL_MS);
                final STATE_COMMANDS fstatecommand = stateCommand;
                stateButtons[stateCommand.INDEX()].setCustomOnClickListener(new SymbolButtonView.onCustomClickListener() {
                    @Override
                    public void onCustomClick() {
                        onStateButtonClick(fstatecommand);
                    }
                });
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void setupDotMatrixDisplay() {
        dotMatrixDisplayView = findViewById(R.id.DOT_MATRIX_DISPLAY);
        layoutDotMatrixDisplay = findViewById(R.id.LAY_DOT_MATRIX_DISPLAY);
    }

    private void setupDotMatrixDisplayUpdater() {
        dotMatrixDisplayUpdater = new MainDotMatrixDisplayUpdater(dotMatrixDisplayView);
    }

    private void setupCtRecordsHandler() {
        ctRecordsHandler = new CtRecordsHandler(this, stringDB);
    }

    private void setupStringDB() {
        stringDB = new StringDB(this);
        stringDB.open();

        if (!stringDB.tableExists(getActivityInfosTableName())) {
            createPekislibTableIfNotExists(stringDB, getActivityInfosTableName());
        }
        if (!stringDB.tableExists(getDotMatrixDisplayColorsTableName())) {
            createSwtimerTableIfNotExists(stringDB, getDotMatrixDisplayColorsTableName());
            initializeTableDotMatrixDisplayColors(stringDB);
            String[] defaults = getDefaults(stringDB, getDotMatrixDisplayColorsTableName());
            setCurrentsForActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), getDotMatrixDisplayColorsTableName(), defaults);
            createPresetWithDefaultValues(getDotMatrixDisplayColorsTableName(), defaults);   //  => PRESET1 = DEFAULT  dans la table de couleurs de DotMatrixDisplay
        }
        if (!stringDB.tableExists(getStateButtonsColorsTableName())) {
            createSwtimerTableIfNotExists(stringDB, getStateButtonsColorsTableName());
            initializeTableStateButtonsColors(stringDB);
            String[] defaults = getDefaults(stringDB, getStateButtonsColorsTableName());
            setCurrentsForActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), getStateButtonsColorsTableName(), defaults);
            createPresetWithDefaultValues(getStateButtonsColorsTableName(), defaults);
        }
        if (!stringDB.tableExists(getBackScreenColorsTableName())) {
            createSwtimerTableIfNotExists(stringDB, getBackScreenColorsTableName());
            initializeTableBackScreenColors(stringDB);
            String[] defaults = getDefaults(stringDB, getBackScreenColorsTableName());
            setCurrentsForActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), getBackScreenColorsTableName(), defaults);
            createPresetWithDefaultValues(getBackScreenColorsTableName(), defaults);
        }
        if (!stringDB.tableExists(getDotMatrixDisplayCoeffsTableName())) {
            createSwtimerTableIfNotExists(stringDB, getDotMatrixDisplayCoeffsTableName());
            initializeTableDotMatrixDisplayCoeffs(stringDB);
            String[] defaults = getDefaults(stringDB, getDotMatrixDisplayCoeffsTableName());
            setCurrentsForActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), getDotMatrixDisplayCoeffsTableName(), defaults);
            createPresetWithDefaultValues(getDotMatrixDisplayCoeffsTableName(), defaults);
        }
        if (!stringDB.tableExists(getPresetsCTTableName())) {
            createSwtimerTableIfNotExists(stringDB, getPresetsCTTableName());
            initializeTablePresetsCT(stringDB);
        }
        if (!stringDB.tableExists(getChronoTimersTableName())) {
            createSwtimerTableIfNotExists(stringDB, getChronoTimersTableName());
        }
    }

    private void createPresetWithDefaultValues(String tableName, String[] defaults) {
        PresetsHandler presetsHandler = new PresetsHandler(stringDB);
        presetsHandler.setTableName(tableName);
        presetsHandler.createNewPreset(defaults);
        presetsHandler.saveAndClose();
        presetsHandler = null;
    }

    private void setupMainCtList() {
        mainCtListItemAdapter = new MainCtListItemAdapter(this, stringDB);
        mainCtListItemAdapter.setOnItemCheckBoxClick(new onCheckBoxClickListener() {
            @Override
            public void onCheckBoxClick() {
                onCtListItemCheckBoxClick();
            }
        });
        mainCtListItemAdapter.setOnItemStartStopResetClick(new MainCtListItemAdapter.onStartStopResetClickListener() {
            @Override
            public void onStartStopResetClick(long nowm, long timeAcc) {
                onCtListItemStartStopResetClick(nowm, timeAcc);
            }
        });
        mainCtListView = findViewById(R.id.CT_LIST);
        mainCtListView.setAdapter(mainCtListItemAdapter);
    }

    private void setupMainCtListUpdater() {
        mainCtListUpdater = new MainCtListUpdater(mainCtListView, ctRecordsHandler, this);
    }

    private void setupShowExpirationTime() {
        showExpirationTime = getSHPShowExpirationTime();
        mainCtListItemAdapter.setShowExpirationTime(showExpirationTime);
        updateDisplayStateButtonColor(STATE_COMMANDS.SHOW_EXPIRATION_TIME);
    }

    private void setupAddNewChronoTimerToList() {
        addNewChronoTimerToList = getSHPaddNewChronoTimerToList();
        updateDisplayStateButtonColor(STATE_COMMANDS.ADD_NEW_CHRONOTIMER_TO_LIST);
    }

    private void setupSetClockAppAlarmOnStartTimer() {
        setClockAppAlarmOnStartTimer = getSHPSetClockAppAlarmOnStartTimer();
        mainCtListItemAdapter.setClockAppAlarmOnStartTimer(setClockAppAlarmOnStartTimer);
    }

    private void setupButtonSpecialColors() {
        final String NEW_CHRONO_TIMER_UNPRESSED_COLOR_DEFAULT = "668CFF";
        final String NEW_CHRONO_TIMER_PRESSED_COLOR_DEFAULT = "0040FF";

        for (final COMMANDS command : COMMANDS.values()) {
            if (command.equals(COMMANDS.NEW_CHRONO) || (command.equals(COMMANDS.NEW_TIMER))) {
                buttons[command.INDEX()].setColors(NEW_CHRONO_TIMER_PRESSED_COLOR_DEFAULT, NEW_CHRONO_TIMER_UNPRESSED_COLOR_DEFAULT);
            }
        }
    }

    private void setupBarMenuItems() {
        final String BAR_MENU_ITEM_SET_CLOCK_APP_ALARM_ON_START_TIMER_NAME = "BAR_MENU_ITEM_SET_CLOCK_APP_ALARM_ON_START_TIMER";
        final String BAR_MENU_ITEM_KEEP_SCREEN_NAME = "BAR_MENU_ITEM_KEEP_SCREEN";

        Class rid = R.id.class;
        try {
            barMenuItemSetClockAppAlarmOnStartTimer = menu.findItem(rid.getField(BAR_MENU_ITEM_SET_CLOCK_APP_ALARM_ON_START_TIMER_NAME).getInt(rid));
            barMenuItemKeepScreen = menu.findItem(rid.getField(BAR_MENU_ITEM_KEEP_SCREEN_NAME).getInt(rid));
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
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
        setStartStatusOfActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, CtDisplayActivity.class);
        callingIntent.putExtra(CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), idct);
        startActivity(callingIntent);
    }

}