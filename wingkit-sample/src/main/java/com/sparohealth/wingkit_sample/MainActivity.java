package com.sparohealth.wingkit_sample;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.sparohealth.wingkit.classes.Client;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    Button btnLogin;
    String clientId = "82ed694a5f516698a5445a542ce65c65";
    String clientSecret = "ba7fdf3f8a375230d5bb817290c6987538d67a6a0bc724368f181010d933acc9";
    App application = null;
    MainActivity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        application = (App)getApplication();

        btnLogin = (Button)findViewById(R.id.login);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (clientId == null || clientId.length() == 0 || clientSecret == null || clientSecret.length() == 0) {
                    AlertDialog alert = new AlertDialog.Builder(activity).create();
                    alert.setTitle("Invalid OAuth Credentials");
                    alert.setMessage("You need to configure the client id and client secret on the Client object in order to authenticate.");
                    alert.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                }
                else {
                    application.client = new Client(application, clientId, clientSecret);
                    try {
                        application.client.authenticate(new Client.WingApiCallback() {
                            @Override
                            public void onSuccessResponse(JSONObject result) {
                                Log.d("Main", result.toString());
                                Intent intent = new Intent(getApplicationContext(), DemographicsActivity.class);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onErrorResponse(Exception error) {
                                Log.d("Main", error.getMessage());
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // do nothing
    }
}
