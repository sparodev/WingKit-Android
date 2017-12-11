package com.sparohealth.wingkit_sample;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Spinner;

import com.sparohealth.wingkit.classes.PatientData;
import com.sparohealth.wingkit.classes.PatientData.Ethnicity;
import com.sparohealth.wingkit.classes.PatientData.BiologicalSex;
import com.sparohealth.wingkit_sample.adapters.DemographicAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DemographicsActivity extends AppCompatActivity {
    private ListView listView;
    private List<DemographicsItem> demographicsItems = new ArrayList<>();
    private DemographicAdapter demographicAdapter;
    private List<String> biologicalSexList = new ArrayList<>();
    private List<String> ethnicityList = new ArrayList<>();
    private DemographicsActivity activity = this;
    private View btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_demographics);


        populateDemographics();
        populateDemographicLists();
        demographicAdapter = new DemographicAdapter(demographicsItems);

        listView = (ListView) findViewById(R.id.listView);
        btnNext = findViewById(R.id.next);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Ethnicity ethnicity = null;
                BiologicalSex biologicalSex = null;
                int height = 0;
                int age = 0;

                for (DemographicsItem item : demographicsItems) {
                    if (item.value.length() == 0) {
                        AlertDialog alert = new AlertDialog.Builder(activity).create();
                        alert.setTitle("Invalid " + item.title);
                        alert.setMessage("Please select a " + item.title.toLowerCase() + " in order to continue.");
                        alert.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        alert.show();
                        return;
                    }
                    else {
                        if (item.type.equals("ethnicity")) {
                            ethnicity = Ethnicity.get(item.value);
                        }
                        else if (item.type.equals("sex")) {
                            biologicalSex = BiologicalSex.valueOf(item.value);
                        }
                        else if (item.type.equals("height")) {
                            height = Integer.valueOf(item.value);
                        }
                        else if (item.type.equals("age")) {
                            age = Integer.valueOf(item.value);
                        }
                    }
                }

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.YEAR, (-1) * age);
//                calendar.set(Calendar.YEAR, 1990);
//                calendar.set(Calendar.MONTH, 10);
//                calendar.set(Calendar.DATE, 16);
                Date date = calendar.getTime();

                // "234"
                String patientId = UUID.randomUUID().toString().substring(0, 20);

                ((App)getApplication()).patientData = new PatientData(patientId, biologicalSex, ethnicity.toString(), height, age, date);

                Intent intent = new Intent(getApplicationContext(), PretestChecksActivity.class);
                startActivity(intent);
                finish();
            }
        });

        listView.setAdapter(demographicAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final PopupMenu menu = new PopupMenu(activity, view);
                DemographicsItem item = demographicsItems.get(i);

                if (item.type.equals("sex")) {
                    for (String sex : biologicalSexList) {
                        menu.getMenu().add(sex);
                    }
                    menu.show();
                }
                else if (item.type.equals("ethnicity")) {
                    for (String ethnicity : ethnicityList) {
                        menu.getMenu().add(ethnicity);
                    }
                    menu.show();
                }
                else if (item.type.equals("age")) {
                    showAgePicker(i);
                }
                else {
                    showHeightPicker(i);
                }

                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        String selected = menuItem.getTitle().toString();
                        if (biologicalSexList.contains(selected)) {
                            for (DemographicsItem demoItem : demographicsItems) {
                                if (demoItem.type.equals("sex")) {
                                    demoItem.value = selected;
                                    demoItem.display = selected;
                                    break;
                                }
                            }
                        }
                        else if (ethnicityList.contains(selected)) {
                            for (DemographicsItem demoItem : demographicsItems) {
                                if (demoItem.type.equals("ethnicity")) {
                                    demoItem.value = selected;
                                    demoItem.display = selected;
                                    break;
                                }
                            }
                        }
                        demographicAdapter.notifyDataSetChanged();
                        return true;
                    }
                });
            }
        });
    }

    private void showAgePicker(final int itemPosition) {
        final FrameLayout rootLayout = (FrameLayout) findViewById(android.R.id.content);
        View.inflate(this, R.layout.spinner_dialog_age, rootLayout);
        View spinnerDialog = findViewById(R.id.spinnerDialog);
        spinnerDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // do nothing
            }
        });
        View btnCancel = findViewById(R.id.cancel);
        View btnOK = findViewById(R.id.ok);

        final Spinner spnAge = (Spinner)findViewById(R.id.age);
        List<String> age = new ArrayList();
        for (int i = 1; i <= 100; i++) {
            age.add(String.valueOf(i));
        }

        ArrayAdapter<String> ageAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, age);
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnAge.setAdapter(ageAdapter);

        if (demographicsItems.get(itemPosition).value.length() != 0)
            spnAge.setSelection(Integer.valueOf(demographicsItems.get(itemPosition).value) - 1);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rootLayout.removeView(findViewById(R.id.spinnerDialog));
            }
        });

        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rootLayout.removeView(findViewById(R.id.spinnerDialog));
                demographicsItems.get(itemPosition).value = spnAge.getSelectedItem().toString();
                demographicsItems.get(itemPosition).display = spnAge.getSelectedItem().toString();
                demographicAdapter.notifyDataSetChanged();
            }
        });
    }

    private void showHeightPicker(final int itemPosition) {
        final FrameLayout rootLayout = (FrameLayout)findViewById(android.R.id.content);
        View.inflate(this, R.layout.spinner_dialog_height, rootLayout);
        View spinnerDialog = findViewById(R.id.spinnerDialog);
        spinnerDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // do nothing
            }
        });

        View btnCancel = findViewById(R.id.cancel);
        View btnOK = findViewById(R.id.ok);

        final Spinner spnFeet = (Spinner)findViewById(R.id.feet);
        List<String> feet = new ArrayList();
        for (int i = 1; i <= 10; i++) {
            feet.add(String.valueOf(i));
        }

        ArrayAdapter<String> feetAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, feet);
        feetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnFeet.setAdapter(feetAdapter);

        final Spinner spnInches = (Spinner)findViewById(R.id.inches);
        List<String> inches = new ArrayList();
        for (int i = 0; i <= 11; i++) {
            inches.add(String.valueOf(i));
        }

        ArrayAdapter<String> inchesAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, inches);
        inchesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnInches.setAdapter(inchesAdapter);

        if (demographicsItems.get(itemPosition).value.length() != 0) {
            int inchesValue = Integer.valueOf(demographicsItems.get(itemPosition).value);
            int feetValue = inchesValue / 12;
            inchesValue = inchesValue % 12;

            spnFeet.setSelection(feetValue - 1);
            spnInches.setSelection(inchesValue);
        }

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rootLayout.removeView(findViewById(R.id.spinnerDialog));
            }
        });

        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rootLayout.removeView(findViewById(R.id.spinnerDialog));
                int feet = spnFeet.getSelectedItemPosition() + 1;
                int inches = spnInches.getSelectedItemPosition();
                int height = (feet * 12) + inches;

                demographicsItems.get(itemPosition).value = String.valueOf(height);
                demographicsItems.get(itemPosition).display = String.format("%d' %d\"", feet, inches);
                demographicAdapter.notifyDataSetChanged();
            }
        });
    }

    private void populateDemographicLists() {
        biologicalSexList.add(BiologicalSex.male.toString());
        biologicalSexList.add(BiologicalSex.female.toString());

        ethnicityList.add(Ethnicity.Asian.toString());
        ethnicityList.add(Ethnicity.black.toString());
        ethnicityList.add(Ethnicity.nativeAmerican.toString());
        ethnicityList.add(Ethnicity.pacificIslander.toString());
        ethnicityList.add(Ethnicity.whiteHispanic.toString());
        ethnicityList.add(Ethnicity.whiteNonHispanic.toString());
        ethnicityList.add(Ethnicity.other.toString());
        ethnicityList.add(PatientData.Ethnicity.twoOrMore.toString());
    }

    private void populateDemographics() {
        demographicsItems.add(new DemographicsItem("sex","Biological Sex", ""));
        demographicsItems.add(new DemographicsItem("age","Age", ""));
        demographicsItems.add(new DemographicsItem("height","Height", ""));
        demographicsItems.add(new DemographicsItem("ethnicity","Ethnicity", ""));
    }

    public class DemographicsItem {
        public String type;
        public String title;
        public String value;
        public String display;

        public DemographicsItem(String type, String title, String value) {
            this.type = type;
            this.title = title;
            this.value = value;
            this.display = value;
        }
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
        finish();
    }}
