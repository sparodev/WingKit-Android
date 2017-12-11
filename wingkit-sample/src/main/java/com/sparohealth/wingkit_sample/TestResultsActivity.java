package com.sparohealth.wingkit_sample;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.sparohealth.wingkit.classes.Test;
import com.sparohealth.wingkit.classes.TestSession;
import com.sparohealth.wingkit_sample.adapters.TestResultsAdapter;

import java.util.ArrayList;

public class TestResultsActivity extends ListActivity {

    private ArrayList<TestResultItem> itemsArray = new ArrayList<TestResultItem>();
    private TestResultsAdapter adapter;
    private TestSession newSession;
    private Button doneButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_test_results);
            doneButton = findViewById(R.id.done);
            doneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(i);
                    finish();
                }
            });
            createItemsArray();
            adapter = new TestResultsAdapter(getListView().getContext(),itemsArray);
            ListView itemsListView  = getListView();
            itemsListView.setAdapter(adapter);
        }catch (Exception ex){
            Log.d("test", ex.getMessage());
        }



    }

    private void createItemsArray() {
        //link with live data


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            try {
                String json = extras.getString("json");

                Gson gson = new Gson();
                newSession = gson.fromJson(json.toString(), TestSession.class);

                if (newSession.id != null)
                    itemsArray.add(new TestResultItem("ID",newSession.id.toString()));
                if (newSession.startedAt != null)
                    itemsArray.add(new TestResultItem("Started At",newSession.startedAt.toString()));
                if (newSession.endedAt != null)
                    itemsArray.add(new TestResultItem("Ended At",newSession.endedAt.toString()));
                if (newSession.bestTest !=null)
                    itemsArray.add(new TestResultItem("Best Test Choice",newSession.bestTestChoice));
//                if (newSession.referenceMetric !=null)
//                    itemsArray.add(new TestResultItem("Reference Metric",newSession.referenceMetric));
                if (newSession.pefPredicted !=null)
                    itemsArray.add(new TestResultItem("PEF Predicted",newSession.pefPredicted.toString()));
                if (newSession.fev1Predicted !=null)
                    itemsArray.add(new TestResultItem("FEV1 Predicted",newSession.fev1Predicted.toString()));
                if (newSession.lungFunctionZone !=null)
                    itemsArray.add(new TestResultItem("Lung Function Zone",newSession.lungFunctionZone));
                if (newSession.respiratoryState !=null)
                    itemsArray.add(new TestResultItem("Respiratory Zone",newSession.respiratoryState));

                if (newSession.bestTest != null) {
                    itemsArray.add(new TestResultItem("Best Test", ""));
                    itemsArray.add(new TestResultItem("ID", newSession.bestTest.id.toString()));
                    itemsArray.add(new TestResultItem("Breath Duration", newSession.bestTest.breathDuration.toString()));
                    itemsArray.add(new TestResultItem("Total Volume", newSession.bestTest.totalVolume.toString()));
                    itemsArray.add(new TestResultItem("PEF", newSession.bestTest.pef.toString()));
                    itemsArray.add(new TestResultItem("FEV1", newSession.bestTest.fev1.toString()));
                    itemsArray.add(new TestResultItem("Status", newSession.bestTest.status.toString()));
                    itemsArray.add(new TestResultItem("Taken At", newSession.bestTest.takenAt.toString()));
                }

                for (int x = 0; x < newSession.tests.size(); x++) {
                    Test current = newSession.tests.get(x);

                    itemsArray.add(new TestResultItem(String.format("Test #%d", x+1), ""));
                    itemsArray.add(new TestResultItem("ID", current.id));
                    itemsArray.add(new TestResultItem("PEF", current.pef == null ? "0" : current.pef.toString()));
                    itemsArray.add(new TestResultItem("FEV1", current.fev1 == null ? "0" : current.fev1.toString()));
                    itemsArray.add(new TestResultItem("Status", current.status.toString()));
                    itemsArray.add(new TestResultItem("Taken At", current.takenAt.toString()));

                }
//
//                for (int count = 0; )
//                itemsArray.add(new TestResultItem("Test 1",newSession.tests...toString()));
//


            } catch (Exception e) {
                Toast.makeText(this, "The JSON data could not be parsed!", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }

    }
    @Override
    public void onBackPressed() {
        Intent i = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i);
        finish();
    }
}
