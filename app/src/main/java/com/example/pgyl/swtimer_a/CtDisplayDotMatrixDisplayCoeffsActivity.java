package com.example.pgyl.swtimer_a;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;

import com.example.pgyl.pekislib_a.DotMatrixDisplayView;
import com.example.pgyl.pekislib_a.HelpActivity;
import com.example.pgyl.pekislib_a.InputButtonsActivity;
import com.example.pgyl.pekislib_a.MainActivity;
import com.example.pgyl.pekislib_a.PresetsActivity;
import com.example.pgyl.pekislib_a.StringDB;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.example.pgyl.pekislib_a.Constants.ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.Constants.NOT_FOUND;
import static com.example.pgyl.pekislib_a.Constants.PEKISLIB_ACTIVITIES;
import static com.example.pgyl.pekislib_a.Constants.SHP_FILE_NAME_SUFFIX;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.HelpActivity.HELP_ACTIVITY_TITLE;
import static com.example.pgyl.pekislib_a.PresetsActivity.PRESETS_ACTIVITY_DISPLAY_TYPE;
import static com.example.pgyl.pekislib_a.PresetsActivity.PRESETS_ACTIVITY_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.StringDBTables.ACTIVITY_START_STATUS;
import static com.example.pgyl.pekislib_a.StringDBTables.TABLE_EXTRA_KEYS;
import static com.example.pgyl.pekislib_a.StringDBUtils.getCurrentFromActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.getCurrentsFromActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.getLabels;
import static com.example.pgyl.pekislib_a.StringDBUtils.getMaxs;
import static com.example.pgyl.pekislib_a.StringDBUtils.isColdStartStatusOfActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.setCurrentForActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.setCurrentsForActivity;
import static com.example.pgyl.pekislib_a.StringDBUtils.setStartStatusOfActivity;
import static com.example.pgyl.swtimer_a.Constants.SWTIMER_ACTIVITIES;
import static com.example.pgyl.swtimer_a.StringDBTables.chronoTimerRowToCtRecord;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayCoeffsDotCornerRadiusIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayCoeffsDotSpacingIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayCoeffsScrollSpeedIndex;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayCoeffsTableName;
import static com.example.pgyl.swtimer_a.StringDBTables.getDotMatrixDisplayColorsTableName;
import static com.example.pgyl.swtimer_a.StringDBUtils.getDBChronoTimerById;

public class CtDisplayDotMatrixDisplayCoeffsActivity extends Activity {
    //region Constantes
    private enum COMMANDS {
        NEXT_VALUE(""), CANCEL("Cancel"), VALUE(""), PRESETS("Presets"), OK("OK");

        private String valueText;

        COMMANDS(String valueText) {
            this.valueText = valueText;
        }

        public String TEXT() {
            return valueText;
        }

        public int INDEX() {
            return ordinal();
        }
    }

    private enum SHP_KEY_NAMES {COEFF_INDEX}

    private final int COEFF_INDEX_DEFAULT_VALUE = 1;  //  les records dans les tables de la DB stockent leur identifiant comme 1er élément (offset 0) => les data commencent à l'offset 1
    //endregion
    //region Variables
    private DotMatrixDisplayView dotMatrixDisplayView;
    private CtDisplayDotMatrixDisplayUpdater dotMatrixDisplayUpdater;
    private Button[] buttons;
    private SeekBar seekBarForValue;
    private CtRecord currentCtRecord;
    private int coeffIndex;
    private String[] colors;   //  Couleurs de DotMatrixDisplay, Boutons, Backscreen
    private String[] coeffs;   //  Espacement des points de DotMatrixDisplay, forme des points et vitesse de défilement
    private String[] labels;
    private String[] maxs;
    private String tableDescription;
    private boolean validReturnFromCalledActivity;
    private String calledActivityName;
    private StringDB stringDB;
    private String shpFileName;
    //endregion

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().setTitle("Set Dot matrix display coeffs");
        setupOrientationLayout();
        setupButtons();
        setupSeekBarForValue();
        setupDotMatrixDisplay();
        validReturnFromCalledActivity = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        dotMatrixDisplayUpdater.close();
        dotMatrixDisplayUpdater = null;
        currentCtRecord = null;
        setCurrentsForActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY_COEFFS.toString(), getDotMatrixDisplayCoeffsTableName(), coeffs);
        stringDB.close();
        stringDB = null;
        savePreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();

        shpFileName = getPackageName() + "." + getClass().getSimpleName() + SHP_FILE_NAME_SUFFIX;
        tableDescription = getIntent().getStringExtra(TABLE_EXTRA_KEYS.DESCRIPTION.toString());
        int idct = getIntent().getIntExtra(CtDisplayActivity.CTDISPLAY_EXTRA_KEYS.CURRENT_CHRONO_TIMER_ID.toString(), NOT_FOUND);
        setupStringDB();
        currentCtRecord = chronoTimerRowToCtRecord(getDBChronoTimerById(stringDB, idct));
        coeffs = getCurrentsFromActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY_COEFFS.toString(), getDotMatrixDisplayCoeffsTableName());
        labels = getLabels(stringDB, getDotMatrixDisplayCoeffsTableName());
        maxs = getMaxs(stringDB, getDotMatrixDisplayCoeffsTableName());
        colors = getCurrentsFromActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY.toString(), getDotMatrixDisplayColorsTableName());  //  Prendre les couleurs actuelles de CtDisplayActivity

        if (isColdStartStatusOfActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY_COEFFS.toString())) {
            setStartStatusOfActivity(stringDB, SWTIMER_ACTIVITIES.CT_DISPLAY_DOT_MATRIX_DISPLAY_COEFFS.toString(), ACTIVITY_START_STATUS.HOT);
            coeffIndex = COEFF_INDEX_DEFAULT_VALUE;
        } else {
            coeffIndex = getSHPCoeffIndex();
            if (validReturnFromCalledActivity) {
                validReturnFromCalledActivity = false;
                if (calledActivityName.equals(PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString())) {
                    coeffs[coeffIndex] = getCurrentFromActivity(stringDB, PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString(), getDotMatrixDisplayCoeffsTableName(), coeffIndex);
                }
                if (calledActivityName.equals(PEKISLIB_ACTIVITIES.PRESETS.toString())) {
                    coeffs = getCurrentsFromActivity(stringDB, PEKISLIB_ACTIVITIES.PRESETS.toString(), getDotMatrixDisplayCoeffsTableName());
                }
            }
        }
        setupDotMatrixDisplayUpdater(currentCtRecord);
        setupDotMatrixDisplayColors();
        setupDotMatrixDisplayCoeffs();
        setupMaxSeekBarForValue();
        rebuildDotMatrixDisplayStructure();
        updateDisplayDotMatrixDisplay();
        updateDisplayButtonTextNextValue();
        updateDisplaySeekBarForValueProgress();
        updateDisplayButtonTextValue();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        validReturnFromCalledActivity = false;
        if (requestCode == PEKISLIB_ACTIVITIES.INPUT_BUTTONS.INDEX()) {
            calledActivityName = PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
        if (requestCode == PEKISLIB_ACTIVITIES.PRESETS.INDEX()) {
            calledActivityName = PEKISLIB_ACTIVITIES.PRESETS.toString();
            if (resultCode == RESULT_OK) {
                validReturnFromCalledActivity = true;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.example.pgyl.pekislib_a.R.menu.menu_help_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == com.example.pgyl.pekislib_a.R.id.HELP) {
            launchHelpActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onButtonClick(COMMANDS command) {
        if (command.equals(COMMANDS.NEXT_VALUE)) {
            onButtonClickNextValue();
        }
        if (command.equals(COMMANDS.CANCEL)) {
            onButtonClickCancel();
        }
        if (command.equals(COMMANDS.VALUE)) {
            onButtonClickValue();
        }
        if (command.equals(COMMANDS.PRESETS)) {
            onButtonClickPresets();
        }
        if (command.equals(COMMANDS.OK)) {
            onButtonClickOK();
        }
    }

    private void onButtonClickNextValue() {
        coeffIndex = coeffIndex + 1;
        if (coeffIndex >= coeffs.length) {
            coeffIndex = 1;
        }
        updateDisplayButtonTextNextValue();
        setupMaxSeekBarForValue();
        updateDisplaySeekBarForValueProgress();
        updateDisplayButtonTextValue();
        setupDotMatrixDisplayCoeffs();
        rebuildDotMatrixDisplayStructure();
        updateDisplayDotMatrixDisplay();
    }

    private void onButtonClickCancel() {
        finish();
    }

    private void onButtonClickValue() {
        launchInputButtonsActivity();
    }

    private void onButtonClickPresets() {
        launchPresetsActivity();
    }

    private void onButtonClickOK() {
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private void onSeekBarForValueProgressChanged(boolean fromUser) {
        if (fromUser) {
            coeffs[coeffIndex] = getSeekBarsForValueProgressString();
            updateDisplayButtonTextValue();
            setupDotMatrixDisplayCoeffs();
            rebuildDotMatrixDisplayStructure();
            if (coeffIndex != getDotMatrixDisplayCoeffsScrollSpeedIndex()) {
                updateDisplayDotMatrixDisplay();
            }
        }
    }

    private void updateDisplayDotMatrixDisplay() {
        final boolean AUTOMATIC_SCROLL_ON = true;

        dotMatrixDisplayUpdater.displayInitTimeAndLabel();
        if (coeffIndex == getDotMatrixDisplayCoeffsScrollSpeedIndex()) {
            dotMatrixDisplayUpdater.startAutomatic(AUTOMATIC_SCROLL_ON);
        } else {
            dotMatrixDisplayUpdater.stopAutomatic();
        }
    }

    private void updateDisplayButtonTextNextValue() {
        final String SYMBOL_NEXT = " >";               //  Pour signifier qu'on peut passer au suivant en poussant sur le bouton

        buttons[COMMANDS.NEXT_VALUE.INDEX()].setText(labels[coeffIndex] + SYMBOL_NEXT);
    }

    private void updateDisplayButtonTextValue() {
        buttons[COMMANDS.VALUE.INDEX()].setText(getSeekBarsForValueProgressString());
    }

    private void updateDisplaySeekBarForValueProgress() {
        seekBarForValue.setProgress(Integer.parseInt(coeffs[coeffIndex]));
    }

    private String getSeekBarsForValueProgressString() {
        return String.valueOf(seekBarForValue.getProgress());
    }

    private void savePreferences() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        SharedPreferences.Editor shpEditor = shp.edit();
        shpEditor.putInt(SHP_KEY_NAMES.COEFF_INDEX.toString(), coeffIndex);
        shpEditor.commit();
    }

    private int getSHPCoeffIndex() {
        SharedPreferences shp = getSharedPreferences(shpFileName, MODE_PRIVATE);
        return shp.getInt(SHP_KEY_NAMES.COEFF_INDEX.toString(), COEFF_INDEX_DEFAULT_VALUE);
    }

    private void setupOrientationLayout() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.ctdisplaydotmatrixdisplaycoeffs_p);
        } else {
            setContentView(R.layout.ctdisplaydotmatrixdisplaycoeffs_l);
        }
    }

    private void setupDotMatrixDisplay() {  //  Pour Afficher HH:MM:SS.CC et éventuellement un label
        dotMatrixDisplayView = findViewById(R.id.DOT_MATRIX_DISPLAY);
    }

    private void setupDotMatrixDisplayUpdater(CtRecord currentCtRecord) {
        dotMatrixDisplayUpdater = new CtDisplayDotMatrixDisplayUpdater(dotMatrixDisplayView, currentCtRecord);
    }

    private void setupDotMatrixDisplayColors() {
        dotMatrixDisplayUpdater.setColors(colors);
    }


    private void setupDotMatrixDisplayCoeffs() {
        dotMatrixDisplayUpdater.setDotSpacingCoeff(coeffs[getDotMatrixDisplayCoeffsDotSpacingIndex()]);
        dotMatrixDisplayUpdater.setDotCornerRadiusCoeff(coeffs[getDotMatrixDisplayCoeffsDotCornerRadiusIndex()]);
        dotMatrixDisplayUpdater.setScrollSpeed(coeffs[getDotMatrixDisplayCoeffsScrollSpeedIndex()]);
    }

    private void rebuildDotMatrixDisplayStructure() {
        dotMatrixDisplayUpdater.rebuildStructure();
    }

    private void setupButtons() {
        final String BUTTON_XML_PREFIX = "BTN_";

        buttons = new Button[COMMANDS.values().length];
        Class rid = R.id.class;
        for (COMMANDS command : COMMANDS.values()) {
            try {
                buttons[command.INDEX()] = findViewById(rid.getField(BUTTON_XML_PREFIX + command.toString()).getInt(rid));   //  BTN_... dans le XML
                buttons[command.INDEX()].setText(command.TEXT());
                final COMMANDS fcommand = command;
                buttons[command.INDEX()].setOnClickListener(new Button.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onButtonClick(fcommand);
                    }
                });
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
                Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void setupSeekBarForValue() {
        try {
            seekBarForValue = findViewById(R.id.SEEKB_VALUE);
            seekBarForValue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    onSeekBarForValueProgressChanged(fromUser);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        } catch (IllegalArgumentException | SecurityException ex) {
            Logger.getLogger(InputButtonsActivity.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setupMaxSeekBarForValue() {
        seekBarForValue.setMax(Integer.parseInt(maxs[coeffIndex]));
    }

    private void setupStringDB() {
        stringDB = new StringDB(this);
        stringDB.open();
    }

    private void launchInputButtonsActivity() {
        setCurrentForActivity(stringDB, PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString(), getDotMatrixDisplayCoeffsTableName(), coeffIndex, getSeekBarsForValueProgressString());
        setStartStatusOfActivity(stringDB, PEKISLIB_ACTIVITIES.INPUT_BUTTONS.toString(), ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, InputButtonsActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), labels[coeffIndex]);
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), getDotMatrixDisplayCoeffsTableName());
        callingIntent.putExtra(TABLE_EXTRA_KEYS.INDEX.toString(), coeffIndex);
        startActivityForResult(callingIntent, PEKISLIB_ACTIVITIES.INPUT_BUTTONS.INDEX());
    }

    private void launchPresetsActivity() {
        final String SEPARATOR = " - ";

        setCurrentsForActivity(stringDB, PEKISLIB_ACTIVITIES.PRESETS.toString(), getDotMatrixDisplayCoeffsTableName(), coeffs);
        setStartStatusOfActivity(stringDB, PEKISLIB_ACTIVITIES.PRESETS.toString(), ACTIVITY_START_STATUS.COLD);
        Intent callingIntent = new Intent(this, PresetsActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), tableDescription);
        callingIntent.putExtra(PRESETS_ACTIVITY_EXTRA_KEYS.SEPARATOR.toString(), SEPARATOR);
        callingIntent.putExtra(PRESETS_ACTIVITY_EXTRA_KEYS.DISPLAY_TYPE.toString(), PRESETS_ACTIVITY_DISPLAY_TYPE.NO_COLORS.toString());
        callingIntent.putExtra(TABLE_EXTRA_KEYS.TABLE.toString(), getDotMatrixDisplayCoeffsTableName());
        startActivityForResult(callingIntent, PEKISLIB_ACTIVITIES.PRESETS.INDEX());
    }

    private void launchHelpActivity() {
        Intent callingIntent = new Intent(this, HelpActivity.class);
        callingIntent.putExtra(ACTIVITY_EXTRA_KEYS.TITLE.toString(), HELP_ACTIVITY_TITLE);
        callingIntent.putExtra(HELP_ACTIVITY_EXTRA_KEYS.HTML_ID.toString(), R.raw.helpctdisplaydotmatrixdisplaycoeffsactivity);
        startActivity(callingIntent);
    }

}
