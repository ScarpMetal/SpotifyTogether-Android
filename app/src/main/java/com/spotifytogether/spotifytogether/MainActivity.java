package com.spotifytogether.spotifytogether;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.view.View;

import java.util.HashMap;
import java.util.Random;
import java.util.Map;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    Button mButtonCreateParty;

    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference mPartiesRef = mRootRef.child("parties");
    DatabaseReference mPartyKeysRef = mRootRef.child("party_keys");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonCreateParty = (Button)findViewById(R.id.buttonCreateParty);
    }

    @Override
    protected void onStart(){
        super.onStart();

        mButtonCreateParty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                Map<String, Object> partyInfoMap = new HashMap<String, Object>();
                String key = generateKey();
                partyInfoMap.put("key", key);
                partyInfoMap.put("skip_threshold", 0);

                DatabaseReference mPartyRef = mPartiesRef.push();
                mPartyRef.setValue(partyInfoMap);
                String mPartyKey = mPartyRef.getKey();
                mPartyKeysRef.child(key).setValue(mPartyKey);
                Intent queueIntent = new Intent(view.getContext(), QueueActivity.class);
                queueIntent.putExtra("mPartyString", mPartyRef.getKey());
                queueIntent.putExtra("mPartyCode", key);
                startActivity(queueIntent);
            }
        });
    }

    protected String generateKey(){
        char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 4; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        String output = sb.toString();
        return output;
    }
}
