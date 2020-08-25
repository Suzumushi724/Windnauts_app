package com.suzumushi.windnauts;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

//import com.google.api.services.sheets.v4.Sheets;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
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
    private static final String EMAIL = "@dc.tohoku.ac.jp";

    public String today;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Date date = new Date();
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd");
        today = dateformat.format(date);
        createSignInIntent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user.getEmail().substring(user.getEmail().length()-EMAIL.length()).equals(EMAIL)) {
                    CheckUsers(user);
                    Button scan_button = findViewById(R.id.scan);
                    scan_button.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    new IntentIntegrator(MainActivity.this).setRequestCode(SCAN_CODE).initiateScan();
                                }
                            }
                    );
                    ActiveDataListener();
                }
                else{
                    Toast.makeText(MainActivity.this,"メールアドレスが無効です",Toast.LENGTH_LONG).show();
                }
                // ...
            } else {
                //Toast.makeText(MainActivity.this,"サインインに失敗しました",Toast.LENGTH_LONG).show();
                if(response == null){
                    Toast.makeText(MainActivity.this,"response is null",Toast.LENGTH_LONG).show();
                }
                if(response.getError().getErrorCode() == ErrorCodes.NO_NETWORK){
                    Toast.makeText(MainActivity.this,"No network",Toast.LENGTH_LONG).show();
                }
                String err = response.getError().toString();
                Toast.makeText(MainActivity.this,err,Toast.LENGTH_LONG).show();
            }
        }

        else if(requestCode == SCAN_CODE){
            IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);
            if(result != null) {
                if(result.getContents() == null) {
                    Toast.makeText(this,"読み取り失敗",Toast.LENGTH_LONG);
                } else {
                    DatabaseReference status = FirebaseDatabase.getInstance().getReference(
                            "members").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    if(result.getContents().equals(IN_CODE)){
                        status.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Long status_num = (Long) snapshot.child("Active").getValue();
                                if(status_num != null) {
                                    if (status_num == 0) {
                                        inout(snapshot,IN_CODE,!snapshot.child("Data").child(today).child("in").exists());
                                        Toast.makeText(MainActivity.this, "入室を記録しました", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "前回の退出記録がありません", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(MainActivity.this,"エラーが発生しました",Toast.LENGTH_LONG).show();
                            }
                        });

                    }
                    else if(result.getContents().equals(OUT_CODE)){
                        status.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Long status_num = (Long)snapshot.child("Active").getValue();

                                if (status_num == 1) {
                                    inout(snapshot,OUT_CODE,!snapshot.child("Data").child(today).child("out").exists());
                                    Toast.makeText(MainActivity.this, "退室を記録しました", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "前回の入室記録がありません", Toast.LENGTH_LONG).show();
                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(MainActivity.this,"エラーが発生しました",Toast.LENGTH_LONG).show();
                            }
                        });

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
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Map<String, String> value = new HashMap<>();
                    value = (Map<String, String>) dataSnapshot.getValue();
                    if (value != null) {
                        if (!value.containsKey(user.getUid())) {
                            init_user(user,database);
                        } else {
                            TextView user_name = findViewById(R.id.user_name);
                            user_name.setText(user.getDisplayName() + getString(R.string.come_back));
                        }
                    } else {
                        init_user(user,database);
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

    public void inout(DataSnapshot snapshot,String code,boolean first_time){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(
                "members").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        if(code.equals(IN_CODE)) {
            ref.child("Active").setValue(1);
        }
        else{
            ref.child("Active").setValue(0);
        }
        Date time = new Date();
        String Data = time.toString();
        if(first_time){
            ref.child("Data").child(today).child(code).setValue(new ArrayList<Date>(){{add(time);}});
            return;
        }
        else{
            List<Date> data_list = (List<Date>) snapshot.child("Data").child(today).child(code).getValue();
            data_list.add(time);
            ref.child("Data").child(today).child(code).setValue(data_list);
            return;
        }
    }

    public void ActiveDataListener(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("members");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Iterable<DataSnapshot>members = snapshot.getChildren();
                TextView active_users_text = findViewById(R.id.active_users);
                active_users_text.setText("");
                active_users_text.setTextSize(18);
                active_users_text.setText("現在作業場にいる人\n");
                active_users_text.setTextSize(14);
                for(DataSnapshot member:members){
                    DataSnapshot active_status = (DataSnapshot)snapshot.child(member.getKey()).child("Active");
                    if(active_status.exists()){
                        if((Long)active_status.getValue() == 1){
                            active_users_text.append((String)snapshot.child(member.getKey()).child("name").getValue()+"\n");
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void init_user(FirebaseUser user, FirebaseDatabase database){
        DatabaseReference member = database.getReference("members").child(user.getUid());
        member.child("name").setValue(user.getDisplayName());
        member.child("Active").setValue(0);
        TextView user_name = findViewById(R.id.user_name);
        user_name.setText(user.getDisplayName() + getString(R.string.welcome));
    }
}