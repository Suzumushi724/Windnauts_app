package com.example.windnauts;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.AuthCredential;

//import com.google.api.services.sheets.v4.Sheets;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;
    private static final int SCAN_CODE = 100;
    private static final String IN_CODE = "in";
    private static final String OUT_CODE = "out";
    private static final String TAG = "MainActivity";

    public static String key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createSignInIntent();
        Button scan_button = findViewById(R.id.scan);
        scan_button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new IntentIntegrator(MainActivity.this).setRequestCode(SCAN_CODE).initiateScan();
                    }
                }
        );
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                CheckUsers(user);
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }

        else if(requestCode == SCAN_CODE){
            IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);
            if(result != null) {
                Log.d("test","success");
                if(result.getContents() == null) {
                    Toast.makeText(this,"読み取り失敗",Toast.LENGTH_LONG);
                } else {
                    if(result.getContents().equals(IN_CODE)){
                        Date in_time = new Date();
                        key = in_time.toString();
                        String Data = in_time.toString();
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        DatabaseReference tmp = database.getReference(
                                "members").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Data").child(key).child("in");
                        tmp.setValue(Data);
                        DatabaseReference tmp2 = database.getReference(
                                "members").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Active");
                        tmp2.setValue(1);
                    }
                    else if(result.getContents().equals(OUT_CODE)){
                        Log.d("key",key);
                        Date out_time = new Date();
                        String Data = out_time.toString();
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        DatabaseReference tmp = database.getReference(
                                "members").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Data").child(key).child("out");
                        tmp.setValue(Data);
                        DatabaseReference tmp2 = database.getReference(
                                "members").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Active");
                        tmp2.setValue(0);
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
                Log.d("test","failed");
            }
        }
    }

    public void createSignInIntent() {
        // [START auth_fui_create_intent]
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
        // [END auth_fui_create_intent]
    }

    public FirebaseUser CheckUsers(final FirebaseUser user) {
        if(user != null) {
            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            final DatabaseReference members = database.getReference("members");
            members.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Map<String, String> value = new HashMap<>();
                    value = (Map<String, String>) dataSnapshot.getValue();
                    Log.d(TAG, "Value is: " + value);
                    if (value != null) {
                        if (!value.containsKey(user.getUid())) {
                            DatabaseReference member = database.getReference("members").child(user.getUid()).child("name");
                            member.setValue(user.getDisplayName());
                            TextView user_name = findViewById(R.id.user_name);
                            user_name.setText(user.getDisplayName() + getString(R.string.welcome));

                        } else {
                            TextView user_name = findViewById(R.id.user_name);
                            user_name.setText(user.getDisplayName() + getString(R.string.come_back));
                        }
                    } else {
                        DatabaseReference member = database.getReference("members").child(user.getUid()).child("name");
                        member.setValue(user.getDisplayName());
                    }

                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.w(TAG, "Failed to read value.", error.toException());
                }
            });
        }
        return user;
    }


}
