package com.spotifytogether.spotifytogether;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.SpotifyPlayer;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class QueueActivity extends AppCompatActivity implements
        SpotifyPlayer.NotificationCallback, ConnectionStateCallback {

    /* Spotify Variables */
    private static final String CLIENT_ID = "22cd5d3a03044fbc9d9d0ec9b23fa66b";
    private static final String REDIRECT_URI = "com.spotifytogether.spotifytogether://callback";
    private static final int REQUEST_CODE = 1337;
    private int songsPlayed = 0;
    private Player mPlayer;
    public static String toPlay;

    /* Firebase Variables */
    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference mSongsRef;
    String mCurrentSongId;
    String mPartyId;
    String mPartyCode;

    ArrayList<Song> songs;

    Button mButtonAddTestSong;
    Button mButtonSkip;

    TextView mLabelPartyCode;
    TextView mLabelSongName;
    TextView mLabelArtist;

    String[] dURI = new String[] {
            "spotify:track:2DopRU3QWz4CWymUUrKvO4",
            "spotify:track:2w0nIRl9BhixD3DcS1Mz3g",
            "spotify:track:7uoQRNx100em2FLLxnqym0",
            "spotify:track:2gPWssZglF1r1xFU5Ow46X",
            "spotify:track:4DXlMukYPm3OPFIiOgWKvh",
            "spotify:track:1pKeFVVUOPjFsOABub0OaV",
            "spotify:track:1ZHYJ2Wwgxes4m8Ba88PeK"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue_view);

        /* Spotify Authentication */
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

        /* Create songs ArrayList */
        mPartyId = getIntent().getExtras().getString("mPartyString");
        mPartyCode = getIntent().getExtras().getString("mPartyCode");
        mSongsRef = mRootRef.child("songs").child(mPartyId);

        mSongsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                songs = new ArrayList<Song>();
                for (DataSnapshot song: dataSnapshot.getChildren()) {
                    Song s = new Song();
                    s.mId = song.getKey().toString();
                    System.out.println(s.mId);
                    s.album = (String)song.child("album").getValue();
                    s.artist = (String)song.child("artist").getValue();
                    s.songName = (String)song.child("name").getValue();
                    s.skip_total = ((Long)song.child("skip_total").getValue()).intValue();
                    s.spotify_uri = (String)song.child("spotify_uri").getValue();
                    s.submitter = (String)song.child("submitter").getValue();
                    for (DataSnapshot skip_user: song.child("skip_users").getChildren()) {
                        s.add_skip_user(skip_user.toString());
                    }
                    songs.add(s);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }

        });

        mButtonAddTestSong = (Button)findViewById(R.id.buttonAddTestSong);
        mButtonSkip = (Button)findViewById(R.id.buttonSkip);

        mButtonAddTestSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                Random rand = new Random();
                String randURI = dURI[rand.nextInt(dURI.length)];
                addSongToQueue("Test Album", "Test Artist", "Test Song Name", 3,
                        randURI, "2212301420");
            }
        });

        mButtonSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                skipCurrentSong();
            }
        });

        mLabelPartyCode = (TextView)findViewById(R.id.labelPartyCode);
        mLabelSongName = (TextView)findViewById(R.id.labelSongName);
        mLabelArtist = (TextView)findViewById(R.id.labelArtist);
        mLabelPartyCode.setText(mPartyCode);
    }

    private String addSongToQueue(String album, String artist, String name, int skip_total,
                                String spotify_uri, String submitter){
        Map<String, Object> songInfoMap = new HashMap<String, Object>();
        songInfoMap.put("album", album);
        songInfoMap.put("artist", artist);
        songInfoMap.put("name", name);
        songInfoMap.put("skip_total", skip_total);
        songInfoMap.put("spotify_uri", spotify_uri);
        songInfoMap.put("submitter", submitter);
        Map<String, Object> skipUsersMap = new HashMap<String, Object>();
        skipUsersMap.put("41209410", true);
        skipUsersMap.put("46439530", true);
        skipUsersMap.put("45435360", true);
        songInfoMap.put("skip_users", skipUsersMap);

        DatabaseReference mSongRef = mSongsRef.push();
        mSongRef.setValue(songInfoMap);

        return mSongRef.getKey();
    }

    private class Song{
        public String mId = "";
        public String album = "";
        public String artist = "";
        public String songName = "";
        public int skip_total = 0;
        public String spotify_uri = "";
        public String submitter = "";
        private ArrayList<String> skip_users = new ArrayList<String>();

        public void add_skip_user(String user){
            skip_users.add(user);
        }
    }

    protected void refresh_queue_view(){

    }

    /* Spotify Functions */
    public void skipCurrentSong(){
        onPlaybackEvent(PlayerEvent.kSpPlaybackNotifyTrackDelivered);
    }

    public String getNextSong(){
        if(songs.size() == 1) {
            mSongsRef.child(songs.get(0).mId).removeValue();
            return null;
        } else if(songs.size() >= 2) {
            mSongsRef.child(songs.get(0).mId).removeValue();
            return songs.get(1).spotify_uri;
        } else {
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer) {
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(QueueActivity.this);
                        mPlayer.addNotificationCallback(QueueActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("QueueActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        // VERY IMPORTANT! This must always be called or else you will leak resources
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("QueueActivity", "Playback event received: " + playerEvent.name());

        if(PlayerEvent.kSpPlaybackNotifyTrackDelivered == playerEvent) {
            toPlay = getNextSong();
            if(toPlay != null){
                mPlayer.playUri(null, QueueActivity.toPlay, 0, 0);
            }

            Log.d("QueueActivity", "Changing song");
        }
        if(songs.size() >= 1){
            mLabelSongName.setText(songs.get(0).songName);
            mLabelArtist.setText(songs.get(0).artist);
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("QueueActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("QueueActivity", "User logged in");
        mCurrentSongId = addSongToQueue("Tapioca", "Slime Girls", "Summer 3 Tokyo Drift", 0,
                "spotify:track:4kmDJv1cUfyqeDiXlRYns3", "124352323");
        mPlayer.playUri(null, "spotify:track:4kmDJv1cUfyqeDiXlRYns3", 0, 0);
    }

    @Override
    public void onLoggedOut() {
        Log.d("QueueActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Error i) {
        Log.d("QueueActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("QueueActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("QueueActivity", "Received connection message: " + message);
    }
}
