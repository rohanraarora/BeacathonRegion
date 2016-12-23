package in.codingninjas.beacathonregion;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.squareup.picasso.Picasso;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import in.codingninjas.beacathonregion.network.ApiClient;
import in.codingninjas.beacathonregion.network.responses.ApiResponse;
import in.codingninjas.beacathonregion.network.NetworkDataManager;
import in.codingninjas.beacathonregion.utils.RoundedImageView;
import in.codingninjas.beacathonregion.utils.UserUtil;
import retrofit2.Call;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String TAG = MainActivity.class.getSimpleName();

    private BeaconManager beaconManager;

    private CopyOnWriteArrayList<String> regionNameList;
    private CopyOnWriteArrayList<Region> regionList;
    private HashMap<String,Region> ssnRegionMap;

    private ArrayAdapter<String> arrayAdapter;

    private LinearLayout mContainer;

    private ProgressDialog progressDialog;

    private static final Identifier nameSpaceId = Identifier.parse("0x5dc33487f02e477d4058");

    private static String[] mPermissions = { Manifest.permission.ACCESS_FINE_LOCATION};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!havePermissions()) {
            Log.i(TAG, "Requesting permissions needed for this app.");
            requestPermissions();
        }

        if(!isBlueEnable()){
            Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(bluetoothIntent);
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        mContainer = (LinearLayout) findViewById(R.id.activity_main);

        RoundedImageView imageView = (RoundedImageView)findViewById(R.id.profile_image_view);
        TextView nameTextView = (TextView)findViewById(R.id.name_text_view);

        String profilePicURL = UserUtil.getProfilePicURL();
        if(profilePicURL!=null && !profilePicURL.isEmpty()) {
            Picasso.with(this).load(UserUtil.getProfilePicURL())
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder).into(imageView);
        }
        nameTextView.setText(UserUtil.getName());

        ssnRegionMap = new HashMap<>();
        regionList = new CopyOnWriteArrayList<>();
        regionNameList = new CopyOnWriteArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, regionNameList);

        ListView listView = (ListView)findViewById(R.id.list_view);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String beaconSSN = regionList.get(i).getId2().toHexString();
                Intent regionIntent = new Intent(MainActivity.this,RegionDetailActivity.class);
                regionIntent.putExtra("beacon_ssn",beaconSSN);
                regionIntent.putExtra("name",regionNameList.get(i));
                startActivity(regionIntent);
            }
        });
        listView.setAdapter(arrayAdapter);

        ssnRegionMap.put("0x0117c59825E9",new Region("Test Room",nameSpaceId,Identifier.parse("0x0117c59825E9"),null));
        ssnRegionMap.put("0x0117c55be3a8",new Region("Git Room",nameSpaceId,Identifier.parse("0x0117c55be3a8"),null));
        ssnRegionMap.put("0x0117c552c493",new Region("Android Room",nameSpaceId,Identifier.parse("0x0117c552c493"),null));
        ssnRegionMap.put("0x0117c55fc452",new Region("iOS Room",nameSpaceId,Identifier.parse("0x0117c55fc452"),null));
        ssnRegionMap.put("0x0117c555c65f",new Region("Python Room",nameSpaceId,Identifier.parse("0x0117c555c65f"),null));
        ssnRegionMap.put("0x0117c55d6660",new Region("Office",nameSpaceId,Identifier.parse("0x0117c55d6660"),null));
        ssnRegionMap.put("0x0117c55ec086",new Region("Ruby Room",nameSpaceId,Identifier.parse("0x0117c55ec086"),null));

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        beaconManager.bind(this);



    }

    private boolean isBlueEnable() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        return bluetoothAdapter.isEnabled();

    }

    private boolean havePermissions() {
        for(String permission:mPermissions){
            if(ActivityCompat.checkSelfPermission(this,permission)!= PackageManager.PERMISSION_GRANTED){
                return  false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                mPermissions, PERMISSIONS_REQUEST_CODE);
    }

    /**
     * Displays {@link Snackbar} instructing user to visit Settings to grant permissions required by
     * this application.
     */
    private void showLinkToSettingsSnackbar() {
        if (mContainer == null) {
            return;
        }
        Snackbar.make(mContainer,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Build intent that displays the App settings screen.
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package",
                                BuildConfig.APPLICATION_ID, null);
                        intent.setData(uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }).show();
    }

    /**
     * Displays {@link Snackbar} with button for the user to re-initiate the permission workflow.
     */
    private void showRequestPermissionsSnackbar() {
        if (mContainer == null) {
            return;
        }
        Snackbar.make(mContainer, R.string.permission_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Request permission.
                        ActivityCompat.requestPermissions(MainActivity.this,
                                mPermissions,
                                PERMISSIONS_REQUEST_CODE);
                    }
                }).show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != PERMISSIONS_REQUEST_CODE) {
            return;
        }
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,permission)) {
                    Log.i(TAG, "Permission denied without 'NEVER ASK AGAIN': " + permission);
                    showRequestPermissionsSnackbar();
                } else {
                    Log.i(TAG, "Permission denied with 'NEVER ASK AGAIN': " + permission);
                    showLinkToSettingsSnackbar();
                }
            } else {
                Log.i(TAG, "Permission granted, building GoogleApiClient");
            }
        }
    }

    @Override
    public void onBeaconServiceConnect() {

        MonitorNotifier monitorNotifier = new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {

            }

            @Override
            public void didExitRegion(Region region) {

            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {
                String regionName = region.getUniqueId();
                String beaconSSN = region.getId2().toHexString();
                switch (i){
                    case INSIDE:
                        Log.i("TAG","Enter " + regionName);
                        regionNameList.add(regionName);
                        regionList.add(region);
                        notifyListChange();
                       // enterRegion(beaconSSN);
                        break;
                    case OUTSIDE:
                        Log.i("TAG","Outside " + regionName);
                        if(regionNameList.contains(regionName)){
                            regionNameList.remove(regionName);
                        }
                        if(regionList.contains(region)) {
                            regionList.remove(region);
                            notifyListChange();
                         //   exitRegion(beaconSSN);
                        }
                        break;
                }
                ArrayList<String> list_beaconSSN = new ArrayList<String>();
                for(Region r: regionList){
                    list_beaconSSN.add(r.getId2().toHexString());
                }
                updateUserInRegions(list_beaconSSN);
            }
        };
        beaconManager.addMonitorNotifier(monitorNotifier);


        try {
            for(String key:ssnRegionMap.keySet()) {
                Region region = ssnRegionMap.get(key);
                beaconManager.startMonitoringBeaconsInRegion(region);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private void notifyListChange(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(arrayAdapter!=null){
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(beaconManager!=null) {
            beaconManager.unbind(this);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void updateUserInRegions(final ArrayList<String> list_beaconSSN){
        NetworkDataManager<ApiResponse> manager = new NetworkDataManager<>();
        NetworkDataManager.NetworkResponseListener listener = manager.new NetworkResponseListener() {
            @Override
            public void onSuccessResponse(ApiResponse response) {
                Log.i("TAG","Enter Update Success for beacon: " + list_beaconSSN);
            }

            @Override
            public void onFailure(int code, String message) {
                Log.i("TAG","Enter Update Fail for beacon: " + list_beaconSSN);
            }
        };
        Call<ApiResponse> call= ApiClient.authorizedApiService().updateUserInRegions(list_beaconSSN);
        manager.execute(call,listener);
    }

    private void enterRegion(final String beaconSSN){
        NetworkDataManager<ApiResponse> manager = new NetworkDataManager<>();
        NetworkDataManager.NetworkResponseListener listener = manager.new NetworkResponseListener() {
            @Override
            public void onSuccessResponse(ApiResponse response) {
                Log.i("TAG","Enter Update Success for beacon: " + beaconSSN);
            }

            @Override
            public void onFailure(int code, String message) {
                Log.i("TAG","Enter Update Fail for beacon: " + beaconSSN);
            }
        };
        Call<ApiResponse> call= ApiClient.authorizedApiService().addUserInRegion(beaconSSN);
        manager.execute(call,listener);
    }

    private void exitRegion(final String beaconSSN){
        NetworkDataManager<ApiResponse> manager = new NetworkDataManager<>();
        NetworkDataManager.NetworkResponseListener listener = manager.new NetworkResponseListener() {
            @Override
            public void onSuccessResponse(ApiResponse response) {
                Log.i("TAG","Exit Update Success for beacon: " + beaconSSN);
            }

            @Override
            public void onFailure(int code, String message) {
                Log.i("TAG","Exit Update Fail for beacon: " + beaconSSN);
            }
        };
        Call<ApiResponse> call = ApiClient.authorizedApiService().removeUserFromRegion(beaconSSN);
        manager.execute(call,listener);
    }


    private void logout(){
        progressDialog.setMessage("Logging Out...");
        progressDialog.show();
        Call<ApiResponse> logout = ApiClient.authorizedApiService().logout();
        NetworkDataManager<ApiResponse> manager = new NetworkDataManager<>();
        NetworkDataManager.NetworkResponseListener listener = manager.new NetworkResponseListener() {
            @Override
            public void onSuccessResponse(ApiResponse response) {
                Log.i("TAG","logout Success");
                if(progressDialog!=null){
                    progressDialog.dismiss();
                }
                UserUtil.logout();
                Intent login = new Intent(MainActivity.this,Login.class);
                startActivity(login);
                finish();
            }

            @Override
            public void onFailure(int code, String message) {
                if(progressDialog!=null){
                    progressDialog.dismiss();
                }
                Log.i("TAG","logout Fail");
                Snackbar.make(mContainer,"Unable to logut",Snackbar.LENGTH_SHORT).show();
            }
        };
        manager.execute(logout,listener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_logout){
            logout();
        }
        return super.onOptionsItemSelected(item);
    }
}
