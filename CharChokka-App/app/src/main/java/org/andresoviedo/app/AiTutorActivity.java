package org.andresoviedo.app;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.andresoviedo.dddmodel2.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

public class AiTutorActivity extends AppCompatActivity {

    SurfaceView cameraView;
    TextView textView;
    CameraSource cameraSource;
    final int RequestCameraPermissionID = 1001;
    private static final String FORMULA_FILE_NAME = "formula.txt"; private static final String REACTION_FILE_NAME = "reaction.txt";

    private SparseArray<TextBlock> textBlock = null;

    //------------------------------------------------
    //JO's coding
    class TextAreaVO {
        public final String value;
        public final Rect rect;

        TextAreaVO(String value, Rect rect) {
            this.value = value;
            this.rect = rect;
        }
    }

    private ArrayList<TextBlock> detectedTexts = new ArrayList<TextBlock>();
    private boolean bDetected = false;

    private ArrayList<TextAreaVO> textAreas = new ArrayList<TextAreaVO>();

    private Paint paintLineBlue = new Paint();
    private Paint paintLineWhite = new Paint();
    private Paint paintLineRed = new Paint();
    private Paint paintTextPanel = new Paint();
    private Paint paintText = new Paint();

    private TextureView tvCanvas;

    private float resolutionRatioW = 0;
    private float resolutionRatioH = 0;

    private ArrayList<ImageView> ivMenus = new ArrayList<ImageView>();

    private WebView webView;
    //------------------------------------------------

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_tutor);

        cameraView = (SurfaceView) findViewById(R.id.surface_view2);
        textView = (TextView) findViewById(R.id.text_view2);

        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w("AiTutorActivity", "Detector dependencies are not yet available");
        }
        else {
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
//                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedPreviewSize(1920, 1080)
                    .setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();
            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {

                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(AiTutorActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    RequestCameraPermissionID);
                            return;
                        }
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

                    cameraSource.stop();
                }
            });
            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {

                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    final ArrayList<Rect> boundingBoxes = new ArrayList<Rect>();

                    if(items.size() != 0)
                    {
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for(int i =0;i<items.size();++i)
                                {
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
//                                    stringBuilder.append(item.getValue() + "/" + item.getBoundingBox());
                                    stringBuilder.append("\n");

                                    Rect temp = item.getBoundingBox();
                                    //Log.i(temp.toString(), "R");

                                    //Separate the coordinates....
                                    String coordinates = temp.toString();
                                    String[] twoCoordinates = coordinates.split(" - ");
                                    String leftTop = twoCoordinates[0];String rightBottom = twoCoordinates[1];
                                    String[] firstCoordinate = leftTop.split(", ");
                                    String[] secondCoordinate = rightBottom.split(", ");
                                    String left = firstCoordinate[0].split("\\(")[1]; String top = firstCoordinate[1];

                                    String right = secondCoordinate[0]; String bottom = secondCoordinate[1].split("\\)")[0];
                                    Log.i(left, top); Log.i(right, bottom);

                                    String tempString = stringBuilder.toString();
                                    Log.i(tempString, "00");

                                    String toDetect = item.getValue().toLowerCase();
                                    /*boolean condition1 = toDetect.contains("water") || toDetect.contains("sulfuric acid");
                                    boolean condition2 = toDetect.contains("h2o") || toDetect.contains("h20");
                                    boolean condition3 = toDetect.contains("h2so4") || toDetect.contains("h2504") || toDetect.contains("h25o4") || toDetect.contains("h2s04");
*/
                                    //List of key-words to detect
                                    String[] listOfWords = {"water", "sulfuric acid", "h2o", "h20", "h2so4",  "h2504", "h25o4","h2s04" };
                                    //Finding the key-word's coordinates
                                    int left_position = Integer.parseInt(left);
                                    int right_position = Integer.parseInt(right);
                                    int lengthOfDetectedString = toDetect.length();
                                    int span = right_position-left_position;
                                    int perCharacterSpace = (int)(span/lengthOfDetectedString);

                                    //composition of h2o reaction
                                    Hashtable<String, Integer> idealReactant = new Hashtable<String, Integer>() {{
                                        put("h2", 2);
                                        put("o2", 1);
                                        //etc
                                    }};

                                    Hashtable<String, Integer> idealProduct = new Hashtable<String, Integer>() {{
                                        put("h2o", 2);
                                        //etc
                                    }};

                                    //Detect if a key-word is present
                                    boolean found = false;
                                    String foundWord = "";
                                    for (String keyWord: listOfWords){
                                        if(toDetect.contains(keyWord)){
                                            found = true;
                                            foundWord = keyWord;
                                            break;
                                        }
                                    }

                                    //If a keyword is found detect its bounding box and save it to a file
                                    if(found){

                                        int startIndex = toDetect.indexOf(foundWord);
                                        int newLeft = perCharacterSpace*startIndex + left_position;
                                        int newRight = newLeft + lengthOfDetectedString*perCharacterSpace;

                                        String coord = top +","+ Integer.toString(newLeft)+"-"+bottom+","+Integer.toString(newRight);
                                        String stringToSend = foundWord+": "+coord+"\n";

                                        //------------------------------------------------
                                        //JO's coding
                                        if(bDetected == false){
                                            detectedTexts.add(item);
                                        }
                                        //------------------------------------------------
                                    }
                                }

                                textBlock = items;

                                //------------------------------------------------
                                //JO's coding
                                if(bDetected == false && detectedTexts.size() > 0){
                                    bDetected = true;
                                }
                                //------------------------------------------------
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        //------------------------------------------------
        //JO's coding

        paintLineBlue.setColor(0xff00bcd4);
        paintLineBlue.setStrokeWidth(10);

        paintLineWhite.setColor(0xffffffff);
        paintLineWhite.setStrokeWidth(4);

        paintLineRed.setColor(0xffff0000);
        paintLineRed.setStrokeWidth(10);

        paintTextPanel.setColor(0xfff84545);
        paintTextPanel.setStyle(Paint.Style.FILL);

        paintText.setTextSize(80);

        tvCanvas = findViewById(R.id.textureView2);
        tvCanvas.setOpaque(false);

        //해당 글자에 취소선,incorrect 표시
        tvCanvas.setSurfaceTextureListener(
                new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                        final Handler handler = new Handler();

                        //카메라와 화면의 해상도 비율
                        resolutionRatioW = (float)width / cameraSource.getPreviewSize().getHeight();
                        resolutionRatioH = (float)height / cameraSource.getPreviewSize().getWidth();

                        handler.postDelayed(new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                            @Override
                            public void run() {
                                if(bDetected  == true && detectedTexts.size() > 0){
                                    Canvas canvas = tvCanvas.lockCanvas();

                                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                                    textAreas.clear();

                                    for(TextBlock tb : detectedTexts){
                                        Rect rect = tb.getBoundingBox();

                                        //카메라 해상도와 화면 해상도가 달라서 비율을 맞춤
                                        rect.left *= resolutionRatioW;
                                        rect.top *= resolutionRatioH;
                                        rect.right *= resolutionRatioW;
                                        rect.bottom *= resolutionRatioH;

                                        //빨간 선
                                        canvas.drawLine(rect.left, rect.top + (rect.bottom - rect.top) * 0.5f, rect.right, rect.top + (rect.bottom - rect.top) * 0.5f, paintLineRed);

                                        //글자 사각형
                                        canvas.drawRoundRect(rect.left - 20, rect.bottom + 20, rect.left + 340, rect.bottom + 120, 20, 20, paintTextPanel);

                                        //글자
                                        canvas.drawText("Incorrect", rect.left, rect.bottom + 100, paintText);
                                    }

                                    tvCanvas.unlockCanvasAndPost(canvas);

                                    detectedTexts.clear();
                                    bDetected = false;
                                }

                                handler.postDelayed(this, 30);
                            }
                        }, 30);
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

                    }
                }
        );

        findViewById(R.id.iv_ai_tutor_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), HomeActivity.class));
            }
        });
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();

    }

}