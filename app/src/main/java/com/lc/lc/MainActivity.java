package com.lc.lc;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.lc.lc.DataLogic.Model.CustomerInfo;
import com.lc.lc.DataLogic.SharedManager;
import com.lc.lc.DataLogic.WebManager.MyWaiter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private Dialog mClaimDlg;
    private int mClaimID;
    private String mLastName;

    private GoogleApiClient mGoogleApiClient;
    public static final String TAG = MainActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;
    private LocationRequest mLocationRequest;
    private Location mCurLocation;

    private Camera mCamera;
    private MediaRecorder mRecorder;
    private boolean bRecording = false;

    private CameraPreview mPreview;
    private Camera.PictureCallback mPicture;
    private ImageView btn_capture, btn_logo, btn_record, btn_flash, btn_claim, pic_signal, bullet;
    private Context myContext;
    private RelativeLayout layout_preview;
    private boolean bCameraFront = false;
    private int m_nSignalStrength = 0;
    private TextView timer;

    private boolean bFlash = false;

    private long startTime = 0L;

    private Handler customHandler = new Handler();

    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;


        initialize();

    }


    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                bCameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                bCameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    public void onResume() {
        super.onResume();

        //if (checkPermissionForCameraAndMicrophone()) {
            if (!hasCamera(myContext)) {
                Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
                toast.show();
                finish();
            }
            if (mCamera == null) {
                //if the front facing camera does not exist
                if (findFrontFacingCamera() < 0) {
                    Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
                    btn_logo.setVisibility(View.GONE);
                }
                releaseCamera();
                chooseCamera();
            /*mCamera = Camera.open(findBackFacingCamera());
            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);*/
            }
        //}
        mGoogleApiClient.connect();


    }

    private boolean checkPermissionForCameraAndMicrophone() {
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if ((resultCamera == PackageManager.PERMISSION_GRANTED) &&
                (resultMic == PackageManager.PERMISSION_GRANTED)) {
            return true;
        } else {
            return false;
        }
    }

    private void requestPermissionForCameraAndMicrophone() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this,
                    R.string.permissions_needed,
                    Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    CAMERA_MIC_PERMISSION_REQUEST_CODE);
        }
    }

    public void initialize() {

        RelativeLayout bl = (RelativeLayout) findViewById(R.id.front);
        bl.bringToFront();

        layout_preview = (RelativeLayout) findViewById(R.id.camera_preview);

//        if (!checkPermissionForCameraAndMicrophone()) {
//            requestPermissionForCameraAndMicrophone();
//        } else {
            mPreview = new CameraPreview(myContext, mCamera);
            layout_preview.addView(mPreview);
//        }

        btn_capture = (ImageView) findViewById(R.id.btn_camera);
        btn_capture.setOnClickListener(captrureListener);

        btn_logo = (ImageView) findViewById(R.id.btn_mark);
        btn_logo.setOnClickListener(logoClickListener);

        btn_record = (ImageView) findViewById(R.id.btn_rec);
        btn_record.setOnClickListener(videoCaptureListener);

        btn_flash = (ImageView) findViewById(R.id.btn_light);
        btn_flash.setOnClickListener(flashClickListener);

        pic_signal = (ImageView) findViewById(R.id.btn_signal);

        btn_claim = (ImageView) findViewById(R.id.btn_claim);
        btn_claim.setOnClickListener(claimClickListener);

        timer = (TextView) findViewById(R.id.tv_timer);
        bullet = (ImageView) findViewById(R.id.bullet);
        //Signal Strength
        TelephonyManager tele = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        tele.listen(new myPhoneStateListener(), PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        //GeoLocation
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

    }

    public void chooseCamera() {
       // if (checkPermissionForCameraAndMicrophone()) {
            //if the camera preview is the front
            if (bCameraFront) {
                int cameraId = findBackFacingCamera();
                if (cameraId >= 0) {
                    //open the backFacingCamera
                    //set a picture callback
                    //refresh the preview

                    mCamera = Camera.open(cameraId);
                    mPicture = getPictureCallback();
                    mPreview.refreshCamera(mCamera);
                }
            } else {
                int cameraId = findFrontFacingCamera();
                if (cameraId >= 0) {
                    //open the backFacingCamera
                    //set a picture callback
                    //refresh the preview

                    mCamera = Camera.open(cameraId);
                    mPicture = getPictureCallback();
                    mPreview.refreshCamera(mCamera);
                }
            }

       // }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //when on Pause, release camera in order to be used from other applications
        releaseCamera();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    private boolean hasCamera(Context context) {
        //check if the device has camera
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    private Camera.PictureCallback getPictureCallback() {
        Camera.PictureCallback picture = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                //make a new picture file
                File pictureFile = getOutputMediaFile();

                if (pictureFile == null) {
                    return;
                }
                try {
                    //write the file
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                    Toast toast = Toast.makeText(myContext, "Picture saved: " + pictureFile.getName(), Toast.LENGTH_LONG);
                    Log.d("masuk", "masuk getPictureCallback()");
                    toast.show();
                    fileUpload(1);

                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                }

                //refresh camera to continue preview
                mPreview.refreshCamera(mCamera);
            }
        };
        return picture;
    }



    View.OnClickListener captrureListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCamera.takePicture(null, null, mPicture);
        }
    };

    View.OnClickListener logoClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //get the number of cameras
            /*int camerasNumber = Camera.getNumberOfCameras();
            if (camerasNumber > 1) {
                //release the old camera instance
                //switch camera, from the front and the back and vice versa

                releaseCamera();
                chooseCamera();
            } else {
                Toast toast = Toast.makeText(myContext, "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
                toast.show();
            }*/
            Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
            startActivity(intent);
            /*MyWaiter waiter = new MyWaiter(MainActivity.this, mGetTokenWaiter, R.string.loading_claim);
            waiter.execute(0);*/
        }
    };

    View.OnClickListener videoCaptureListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (bRecording) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            timer.setTextColor(getResources().getColor(R.color.colorTimer));
                            bullet.setImageDrawable(getResources().getDrawable(R.drawable.grey_bullet));
                            btn_record.setImageResource(R.drawable.record_off_round);
                            mRecorder.stop(); // stop the recording

                            //Stop timer video
                            //timeSwapBuff += timeInMilliseconds;
                            timeInMilliseconds = 0;
                            customHandler.removeCallbacks(updateTimerThread);
                            timer.setText("0:00");

                            releaseMediaRecorder(); // release the MediaRecorder object
                            Toast.makeText(MainActivity.this, "Video captured!", Toast.LENGTH_LONG).show();
                            fileUpload(2);
                        } catch (final Exception ex) {
                            Toast.makeText(MainActivity.this, "Video Save Failed!", Toast.LENGTH_LONG).show();
                        }

                    }
                });
                bRecording = false;
            } else {
                if (!prepareMediaRecorder()) {
                    Toast.makeText(MainActivity.this, "Fail in prepareMediaRecorder()!\n - Ended -", Toast.LENGTH_LONG).show();
                    finish();
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            timer.setTextColor(getResources().getColor(R.color.white));
                            bullet.setImageDrawable(getResources().getDrawable(R.drawable.bluebullet));
                            btn_record.setImageResource(R.drawable.record_on_round);
                            mRecorder.start();

                            //Start timer video
                            startTime = SystemClock.uptimeMillis();
                            customHandler.postDelayed(updateTimerThread, 0);


                        } catch (final Exception ex) {
                        }
                    }
                });
                bRecording = true;
            }
        }
    };

    //Update timer video
    private Runnable updateTimerThread = new Runnable() {

        public void run() {

            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;

            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);
            timer.setText("" + mins + ":"
                    + String.format("%02d", secs));
            customHandler.postDelayed(this, 0);
        }

    };

    View.OnClickListener flashClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Camera.Parameters p = mCamera.getParameters();

            bFlash = !bFlash;

            if (bFlash) {
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }

            mCamera.setParameters(p);
        }
    };

    View.OnClickListener claimClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            mClaimDlg = new Dialog(MainActivity.this);
            mClaimDlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mClaimDlg.setContentView(R.layout.dlg_claim);
            mClaimDlg.setTitle("Load Claim");
            final TextView txtClaim = (TextView) mClaimDlg.findViewById(R.id.txt_claimid);
            final TextView txtLastName = (TextView) mClaimDlg.findViewById(R.id.txt_lastname);
            TextView txtReturn = (TextView) mClaimDlg.findViewById(R.id.txt_return);
            if (SharedManager.getInstance().m_CustomerInfo != null) {
                txtReturn.setText("Successed");
                txtLastName.setText(SharedManager.getInstance().m_CustomerInfo.mCustomerLastName);
                txtClaim.setText(String.valueOf(SharedManager.getInstance().m_CustomerInfo.mClaimID));
            } else
                txtReturn.setText("");

            Button btn_save = (Button) mClaimDlg.findViewById(R.id.btn_save);
            btn_save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("save", "save");
                    if (txtClaim.getText().length() == 0 || txtLastName.getText().length() == 0) {
                        Log.d("save", "please input");
                        new AlertDialog.Builder(MainActivity.this).setTitle("Alert").setMessage("Please input the ClaimId or LastName!!!").setNegativeButton("OK", null).create().show();
                        return;
                    }
                    if (SharedManager.getInstance().m_CustomerInfo == null) {
                        Log.d("save", "shared manager null");
                        mClaimID = Integer.parseInt(txtClaim.getText().toString());
                        mLastName = txtLastName.getText().toString();
                        MyWaiter waiter = new MyWaiter(MainActivity.this, mLoadClaimWaiter, R.string.loading_claim);
                        waiter.execute(0);
                    } else {
                        Log.d("save", "loaded");
                        new AlertDialog.Builder(MainActivity.this).setTitle("Alert").setMessage("You have already loaded!").setNegativeButton("OK", null).create().show();
                    }
                }
            });

            ImageView btn_ok = (ImageView) mClaimDlg.findViewById(R.id.btn_ok);
            btn_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClaimDlg.dismiss();
                    mClaimDlg = null;
                }
            });
            mClaimDlg.show();
        }
    };

    private void releaseMediaRecorder() {
        if (mRecorder != null) {
            mRecorder.reset(); // clear recorder configuration
            mRecorder.release(); // release the recorder object
            mRecorder = null;
            mCamera.lock(); // lock camera for later use
        }
    }

    private boolean prepareMediaRecorder() {
        mRecorder = new MediaRecorder();
        mCamera.unlock();
        mRecorder.setCamera(mCamera);

        mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        CamcorderProfile cpHigh = CamcorderProfile
                .get(CamcorderProfile.QUALITY_HIGH);
        mRecorder.setProfile(cpHigh);
        //mRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        mRecorder.setOutputFile("/sdcard/Lc Camera/Video_Upload.mp4");

        mRecorder.setMaxDuration(600000); //set maximum duration 60 sec.
        mRecorder.setMaxFileSize(50000000); //set maximum file size 50M

        try {
            mRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    //make picture and save to a folder
    private static File getOutputMediaFile() {
        //make a new file directory inside the "sdcard" folder
        File mediaStorageDir = new File("/sdcard/", "Lc Camera");

        //if this "JCGCamera folder does not exist
        if (!mediaStorageDir.exists()) {
            //if you cannot make this folder return
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        //take the current timeStamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        //and make a media file:
        //mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "Image_Upload.jpg");

        return mediaFile;
    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    //Geo Location
    @Override
    public void onConnected(Bundle bundle) {

        try {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location == null) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } else {
                handleNewLocation(location);
            }
        } catch (SecurityException e) {
            new AlertDialog.Builder(MainActivity.this).setTitle("Error").setMessage("Please check your network state.").setNegativeButton("OK", null).create().show();
        }
    }

    private void handleNewLocation(Location location) {
        mCurLocation = location;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }
    ////////////////////////////////////

    ////////////////Phone Signal Strength
    class myPhoneStateListener extends PhoneStateListener {

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);

            TelephonyManager tele = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            String NetTypeStr;
            switch (tele.getNetworkType()) {
                case 1:
                    NetTypeStr = "GPRS";
                    break;
                case 2:
                    NetTypeStr = "EDGE";
                    break;
                case 3:
                    NetTypeStr = "UMTS";
                    break;
                case 4:
                    NetTypeStr = "CDMA";
                    break;
                case 5:
                    NetTypeStr = "EVDO_0";
                    break;
                case 6:
                    NetTypeStr = "EVDO_A";
                    break;
                case 7:
                    NetTypeStr = "1xRTT";
                    break;
                case 8:
                    NetTypeStr = "HSDPA";
                    break;
                case 9:
                    NetTypeStr = "HSUPA";
                    break;
                case 10:
                    NetTypeStr = "HSPA";
                    break;
                case 11:
                    NetTypeStr = "iDen";
                    break;
                case 12:
                    NetTypeStr = "EVDO_B";
                    break;
                case 13:
                    NetTypeStr = "LTE";
                    break;

                case 14:
                    NetTypeStr = "eHRPD";
                    break;
                case 15:
                    NetTypeStr = "HSPA+";
                    break;
            }
            int level = signalStrength.getLevel();
            switch (level) {
                case 0:
                    pic_signal.setImageBitmap(null);
                    break;
                case 1:
                    pic_signal.setImageResource(R.drawable.signal_1);
                    break;
                case 2:
                    pic_signal.setImageResource(R.drawable.signal_2);
                    break;
                case 3:
                    pic_signal.setImageResource(R.drawable.signal_3);
                    break;
                case 4:
                    pic_signal.setImageResource(R.drawable.signal_4);
                    break;
            }
        }
    }

    /////////////////////////////////////
    ////////////////////////////////////Web Api
    private void fileUpload(int fileType)//1: photo  2:video
    {//LoadClaimInfo
        Log.d("masuk", "masuk");
        if (SharedManager.getInstance().m_CustomerInfo != null) {
            Log.d("masuk", "masuk fileUpload");
            String filePath = null;
            if (fileType == 1) {//photoupload
                Log.d("masuk", "masuk1");
                filePath = "/sdcard/Lc Camera/Image_Upload.jpg";
            } else if (fileType == 2) {//video upload
                Log.d("masuk", "masuk2");
                filePath = "/sdcard/Lc Camera/Video_Upload.mp4";
            }
            try {
                ExifInterface exif = new ExifInterface(filePath);

                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, DMSconv(mCurLocation.getLatitude()));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, DMSconv(mCurLocation.getLongitude()));
                if (mCurLocation.getLatitude() > 0)
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
                else
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
                if (mCurLocation.getLongitude() > 0)
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
                else
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
                exif.saveAttributes();

            } catch (IOException e) {
                e.printStackTrace();
            }

            MyWaiter uploadWaiter = new MyWaiter(MainActivity.this, mUploadWaiter, R.string.uploading_file);
            uploadWaiter.m_tag = fileType;
            uploadWaiter.execute(0);
        }
    }

    String DMSconv(double coord) {
        coord = (coord > 0) ? coord : (-1) * coord;  // -105.9876543 -> 105.9876543
        String sOut = Integer.toString((int) coord) + "/1,";   // 105/1,
        coord = (coord % 1) * 60;         // .987654321 * 60 = 59.259258
        sOut = sOut + Integer.toString((int) coord) + "/1,";   // 105/1,59/1,
        coord = (coord % 1) * 6000;             // .259258 * 6000 = 1555
        sOut = sOut + Integer.toString((int) coord) + "/1000";   // 105/1,59/1,15555/1000
        return sOut;
    }

    public MyWaiter.WaiterDelegate mLoadClaimWaiter = new MyWaiter.WaiterDelegate() {

        @Override
        public boolean OnWaiterStart(MyWaiter waiter) {
            Log.d("save", "OnWaiterStart");
            return super.OnWaiterStart(waiter);
        }

        @Override
        public void OnWaiterStop(MyWaiter waiter, Object resultObj) {

            if (resultObj != null) {
                Log.d("save", "resultObj != null");
                if (mClaimDlg != null) {
                    Log.d("save", "failed");
                    TextView txtReturn = (TextView) mClaimDlg.findViewById(R.id.txt_return);
                    txtReturn.setText("Failed!");
                }
            } else {//Upload
                if (mClaimDlg != null) {
                    Log.d("save", "success");
                    TextView txtReturn = (TextView) mClaimDlg.findViewById(R.id.txt_return);
                    txtReturn.setText("Success!");

                }
            }
        }

        @Override
        public Object OnWaiterWork(MyWaiter waiter, Object... parms) {
            Log.d("save", "OnWaiterWork");
            String strError = null;
            strError = CustomerInfo.loadClaimInfo(mClaimID, mLastName);
            return strError;
        }

        @Override
        public void OnWaiterUpdate(MyWaiter waiter, Object... params) {
            Log.d("save", "OnWaiterUpdate");
            super.OnWaiterUpdate(waiter, params);
        }
    };

    public MyWaiter.WaiterDelegate mUploadWaiter = new MyWaiter.WaiterDelegate() {

        @Override
        public boolean OnWaiterStart(MyWaiter waiter) {
            return super.OnWaiterStart(waiter);
        }

        @Override
        public void OnWaiterStop(MyWaiter waiter, Object resultObj) {

            if (resultObj != null) {
                new AlertDialog.Builder(MainActivity.this).setTitle("Error").setMessage((String) resultObj).setNegativeButton("OK", null).create().show();
            } else {
                new AlertDialog.Builder(MainActivity.this).setTitle("Success").setMessage("Upload Successed!").setNegativeButton("OK", null).create().show();
            }
        }

        @Override
        public Object OnWaiterWork(MyWaiter waiter, Object... parms) {
            String strError = null;

            File uploadFile = null;
            if (waiter.m_tag == 1) {//photoupload
                uploadFile = new File("/sdcard/Lc Camera/Image_Upload.jpg");
            } else if (waiter.m_tag == 2) {//video upload
                uploadFile = new File("/sdcard/Lc Camera/Video_Upload.mp4");
            }
            CustomerInfo cust = SharedManager.getInstance().m_CustomerInfo;
            strError = SharedManager.getInstance().fileUpload(cust.mClaimID, 1, uploadFile);

            return strError;
        }

        @Override
        public void OnWaiterUpdate(MyWaiter waiter, Object... params) {
            super.OnWaiterUpdate(waiter, params);
        }
    };

    public MyWaiter.WaiterDelegate mGetTokenWaiter = new MyWaiter.WaiterDelegate() {

        @Override
        public boolean OnWaiterStart(MyWaiter waiter) {
            return super.OnWaiterStart(waiter);
        }

        @Override
        public void OnWaiterStop(MyWaiter waiter, Object resultObj) {

            if (resultObj != null) {
                new AlertDialog.Builder(MainActivity.this).setTitle("Error").setMessage((String) resultObj).setNegativeButton("OK", null).create().show();
            } else {
                Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
                startActivity(intent);
            }
        }

        @Override
        public Object OnWaiterWork(MyWaiter waiter, Object... parms) {
            String strError = null;
            strError = SharedManager.getInstance().getToken();
            return strError;
        }

        @Override
        public void OnWaiterUpdate(MyWaiter waiter, Object... params) {
            super.OnWaiterUpdate(waiter, params);
        }
    };


}
