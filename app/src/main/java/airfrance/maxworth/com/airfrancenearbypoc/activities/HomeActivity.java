package airfrance.maxworth.com.airfrancenearbypoc.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import airfrance.maxworth.com.airfrancenearbypoc.R;
import airfrance.maxworth.com.airfrancenearbypoc.pojo.BoardingPassData;
import airfrance.maxworth.com.airfrancenearbypoc.pojo.PrintElements;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by mars on 13/03/18.
 */

public class HomeActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks{

    private static final String[] LOCATION =
            {Manifest.permission.ACCESS_FINE_LOCATION};

    private static final int RC_LOCATION_PERM = 124;


    @BindView(R.id.connectKiosk)
    TextView connectKiosk;

    @BindView(R.id.status)
    TextView status;

    @BindView(R.id.printPass)
    TextView printPass;

    @BindView(R.id.oneImg)
    ImageView one;

    @BindView(R.id.twoImg)
    ImageView two;

    @BindView(R.id.threeImg)
    ImageView three;

    @BindView(R.id.searchView)
    RelativeLayout searchView;

    @BindView(R.id.printView)
    LinearLayout printView;

    List<String> selectedViews = new ArrayList<>();


    private BluetoothAdapter mBluetoothAdapter;

    List<BluetoothDevice> bleDevices = new ArrayList<>();
    public BluetoothDevice selectedDevice;

    Handler mHandler = new Handler();
    boolean mScanning = false;
    long SCAN_PERIOD = 1500;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);


        searchView.setVisibility(View.VISIBLE);
        printView.setVisibility(View.GONE);
    }


    private boolean hasLocationPermissions() {
        return EasyPermissions.hasPermissions(this, LOCATION);
    }


    /*
    @author Prakash
    @Step1 Check all required permission
    @Step2 Enable bluetooth default adapter if disables and create instance of
    bluetooth adapter
     */
    @AfterPermissionGranted(RC_LOCATION_PERM)
    public void locationTask() {
        if (hasLocationPermissions()) {
            Log.e("Permissions","start process");
            if(getBLEAdapter()){
                startDiscovery();
            }
        } else {
            // Ask for both permissions
            Log.e("Permissions","requestPermissions");
            EasyPermissions.requestPermissions(
                    this,
                    "Allow Location permission",
                    RC_LOCATION_PERM,
                    LOCATION);
        }
    }

    /*
    Called with reference of func locationTask
    @Step2 Enable bluetooth default adapter if disables and create instance of
    bluetooth adapter
     */

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean getBLEAdapter(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.enable();
        }
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            status.setText("No BLE feature found on this device");
            printPass.setEnabled(false);
            connectKiosk.setEnabled(false);
            return false;
        }else if(!mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.enable();
        }
        status.setVisibility(View.GONE);
        printPass.setEnabled(true);
        connectKiosk.setEnabled(true);
        return true;
    }

    public boolean hasBleFeature(){
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
           status.setText("No BLE feature found on this device");
            printPass.setEnabled(false);
            connectKiosk.setEnabled(false);
           return false;
        }
        status.setVisibility(View.GONE);
        printPass.setEnabled(true);
        connectKiosk.setEnabled(true);
        return true;
    }



    @OnClick({R.id.connectKiosk,R.id.printPass})
    public void onClick(View view){
        Log.e("Click",view.getId()+"");
        if(view.getId()==R.id.connectKiosk){
            locationTask();
        }else if(view.getId()==R.id.printPass){
                if(selectedDevice==null){
                    showAlert("No Selected devices found");
                }else{
                    print(selectedDevice);
                }
        }
    }


    public void print(BluetoothDevice device){

        if(mBluetoothAdapter!=null) {
            mBluetoothAdapter.cancelDiscovery();
            if(!mBluetoothAdapter.isEnabled()){
                Toast.makeText(this,"Bluetooth Disabled",Toast.LENGTH_LONG).show();
                return;
            }
        }
        show();
        setMessage("Looking for kiosk...");
        try{
            Connection connection = new Connection(device);
            connection.start();
        }catch (Exception e){
            hide();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        locationTask();
    }


    public void startBleSearch(){
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();

            }
          bleDevices.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        bleDevices.clear();
        try{
            unregisterReceiver(mReceiver);
        }catch (Exception e){

        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mBluetoothAdapter!=null)
        mBluetoothAdapter.disable();
    }

    public void startDiscovery(){
        startBleSearch();
        mBluetoothAdapter.startDiscovery();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        status.setVisibility(View.VISIBLE);
        status.setText("Looking for kiosk...");
        registerReceiver(mReceiver, filter);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction()==null){
                status.setText("UNKNOW EVENT ");
            }else if(intent.getAction().equalsIgnoreCase(BluetoothDevice.ACTION_FOUND)){
                Log.e("mReceiver","ACTION_FOUND");
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                status.setText("LAST FOUND DEVICE :- "+device.getName());

                if(device==null) {

                }else if(device.getName()==null) {

                }else if(device.getName().startsWith("ICTSKIOSK")){
                    mBluetoothAdapter.cancelDiscovery();
                    selectedDevice = device;
                    searchView.setVisibility(View.GONE);
                    printView.setVisibility(View.VISIBLE);
                    print(device);

                }


            }else if(intent.getAction().equalsIgnoreCase(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){
                Log.e("mReceiver","ACTION_DISCOVERY_STARTED");
                status.setText("BLE DISCOVERY STARTED");

            }else if(intent.getAction().equalsIgnoreCase(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
                Log.e("mReceiver","ACTION_DISCOVERY_FINISHED");
                status.setText("BLE DISCOVERY FINISHED");

            }


        }
    };


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mBluetoothAdapter.disable();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.d("Permissions", "onPermissionsGranted:" + requestCode );
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Log.d("Permissions", "onPermissionsDenied:" + requestCode );
    }

    @Override
    public void onRationaleAccepted(int requestCode) {
        Log.d("Permissions", "onRationaleAccepted:" + requestCode );
    }

    @Override
    public void onRationaleDenied(int requestCode) {
        Log.d("Permissions", "onPermissionsDenied:" + requestCode );
    }


    public void showAlert(String message){
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage(message);
        builder1.setCancelable(true);
        builder1.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alertBox = builder1.create();
        alertBox.show();
    }



    public class Connection extends Thread{
        private BluetoothDevice bluetoothDevice;
        private final String UUIDSTR = "01234567-0123-4567-8901-fcd82f1e0677";
        private BluetoothSocket bluetoothSocket = null;
        public Connection(BluetoothDevice device){
            try {
                bluetoothDevice = device;
                bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(UUIDSTR));

            }catch (final Exception e){
                hide();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HomeActivity.this.showAlert("Unable to create scoket connection "+e.getMessage());
                    }
                });

            }
        }


        @Override
        public void run() {
            super.run();

            try{
                if(bluetoothSocket==null){
                    hide();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            HomeActivity.this.showAlert("Socket Not connected.");
                        }
                    });
                }else{

                    // BluetoothManager manager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
                    //manager.getAdapter().listenUsingInsecureRfcommWithServiceRecord(bluetoothDevice.getName(),UUID.fromString(UUIDSTR));
                    bluetoothSocket.connect();
                    connectedSocket = bluetoothSocket;
                    Thread.sleep(750);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                           // new PrintThread(bluetoothSocket).start();
                            hide();
                        }
                    });
                }

            }catch (final Exception e){
                hide();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HomeActivity.this.showAlert("Unable to create scoket connection "+e.getMessage());
                    }
                });
            }

        }
    }
    private BluetoothSocket connectedSocket = null;
    public class PrintThread extends Thread{


        private BluetoothSocket bluetoothSocket = null;
        private  InputStream connectedInputStream;
        private  OutputStream connectedOutputStream;

        public PrintThread(BluetoothSocket socket){
            bluetoothSocket = socket;
        }


        @Override
        public void run() {
            super.run();
            //get Stream data

            try {
                if(bluetoothSocket != null) {
                    connectedInputStream = bluetoothSocket.getInputStream();
                    connectedOutputStream = bluetoothSocket.getOutputStream();
                }else{
                    hide();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            HomeActivity.this.showAlert("Invalid connection");
                        }
                    });
                }
            }
            catch (final Exception e) {
                hide();
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HomeActivity.this.showAlert("Unable to create scoket connection "+e.getMessage());
                    }
                });
            }


            if(bluetoothSocket.isConnected() || !bluetoothSocket.isConnected()){
                //send data on stream
                try{
                    InputStream is = getAssets().open("bg_image.png");
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);
                    byte[] byteArray = stream.toByteArray();

                    PrintElements printElements = new PrintElements();
                    printElements.flight_no="AF 101";
                    printElements.Kiosk_id="KIOSK01";
                    BoardingPassData boardingPassData1=new BoardingPassData();
                    boardingPassData1.id="BP001";
                    boardingPassData1.pngImage= Base64.encodeToString(byteArray, Base64.DEFAULT);

                    if(selectedViews.size()==1) {
                        printElements.boardingPassData = new BoardingPassData[]{boardingPassData1};
                    }else if(selectedViews.size()==2){
                        printElements.boardingPassData=new BoardingPassData[]{boardingPassData1,boardingPassData1};
                    }else if(selectedViews.size()==3){
                        printElements.boardingPassData=new BoardingPassData[]{boardingPassData1,boardingPassData1,boardingPassData1};
                    }

                    String val=new Gson().toJson(printElements);
                    connectedOutputStream.write(val.getBytes());
                    Thread.sleep(2500);
                    bluetoothSocket.close();
                    connectedSocket = null;
                    // Thread.currentThread().interrupt();
                    endPrint();
                    hide();
                }catch (final Exception e){
                    hide();

                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                              HomeActivity.this.showAlert("Unable to PRINT DATA "+e.getMessage());
                        }
                    });
                }
            }else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        HomeActivity.this.showAlert("Unable to PRINT DATA socket not found");
                    }
                });
            }





        }
    }


    private ProgressDialog dialog;
    public void show(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(dialog==null)
                    dialog = new ProgressDialog(HomeActivity.this);
                dialog.setMessage("Printing...");
                dialog.show();
            }
        });


    }


    public void setMessage(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(dialog!=null){
                    dialog.setMessage(message);
                }
            }
        });

    }


    public void hide(){
       runOnUiThread(new Runnable() {
           @Override
           public void run() {
               try {
                   if (dialog != null)
                       dialog.hide();
                   dialog = null;
               }catch (Exception e){

               }
           }
       });
    }




    public void imageClick(View view){

        if(view.getId()==R.id.one){


            if(view.getTag()==null){
                view.setTag("ONE");
                one.setImageResource(R.drawable.ic_check_fill);
                if(!selectedViews.contains("ONE")){
                    selectedViews.add("ONE");
                }
            }else if(view.getTag().equals("ONE")){
                view.setTag(null);
                one.setImageResource(R.drawable.ic_check_round);
                if(selectedViews.contains("ONE")){
                    selectedViews.remove("ONE");
                }
            }



        }else if(view.getId()==R.id.two){

            if(view.getTag()==null){
                view.setTag("TWO");
                two.setImageResource(R.drawable.ic_check_fill);
                if(!selectedViews.contains("TWO")){
                    selectedViews.add("TWO");
                }
            }else if(view.getTag().equals("TWO")){
                view.setTag(null);
                two.setImageResource(R.drawable.ic_check_round);
                if(selectedViews.contains("TWO")){
                    selectedViews.remove("TWO");
                }
            }



        }else if(view.getId()==R.id.three){


            if(view.getTag()==null){
                view.setTag("THREE");
                three.setImageResource(R.drawable.ic_check_fill);
                if(!selectedViews.contains("ONE")){
                    selectedViews.add("ONE");
                }
            }else if(view.getTag().equals("THREE")){
                view.setTag(null);
                three.setImageResource(R.drawable.ic_check_round);
                if(selectedViews.contains("THREE")){
                    selectedViews.remove("THREE");
                }
            }


        }else if(view.getId()==R.id.printSlip){

            if(connectedSocket!=null) {
                if (selectedViews.size() > 0) {
                    show();
                    setMessage("Printing...");
                    new PrintThread(connectedSocket).start();
                } else {
                    Toast.makeText(getApplicationContext(), "Please select any image", Toast.LENGTH_LONG).show();
                }
            }


        }
    }



    public void endPrint(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hide();
                connectedSocket = null;
                selectedViews.clear();
                one.setTag(null);
                two.setTag(null);
                three.setTag(null);
                one.setImageResource(R.drawable.ic_check_round);
                two.setImageResource(R.drawable.ic_check_round);
                three.setImageResource(R.drawable.ic_check_round);
                searchView.setVisibility(View.VISIBLE);
                printView.setVisibility(View.GONE);
            }
        });

    }

}
