package org.andresoviedo.app;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.andresoviedo.app.model3D.view.ModelActivity;
import org.andresoviedo.app.util.CharChokkaJsonVO;
import org.andresoviedo.app.util.Global;
import org.andresoviedo.app.util.LoadingCircleEffect;
import org.andresoviedo.app.util.RequestHttpURLConnection;
import org.andresoviedo.dddmodel2.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

public class AugmentedCamActivity extends AppCompatActivity {

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

    private TextureView tvCanvas;

    private float resolutionRatioW = 0;
    private float resolutionRatioH = 0;

    private ArrayList<ImageView> ivMenus = new ArrayList<ImageView>();

    private WebView webView;

    private String[] webUrls = {"www.naver.com", "www.daum.net", "", "www.google.com"};

    private DownloadThread downloadThread;

    private LoadingCircleEffect downloadEffect;

    public class NetworkTask extends AsyncTask<Void, Void, String> {

        private String url;
        private ContentValues values;

        public NetworkTask(String url, ContentValues values) {
            this.url = url;
            this.values = values;
        }

        @Override
        protected String doInBackground(Void... params) {

            String result; // 요청 결과를 저장할 변수.
            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            result = requestHttpURLConnection.request(url, values); // 해당 URL로 부터 결과물을 얻어온다.

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if(s.length() > 0){
                String json = s.substring(s.indexOf("{"), s.length() - 1);
                json = json.replace("\\", "");

                ObjectMapper om = new ObjectMapper();

                try{
                    Global.charChokka = om.readValue(json, CharChokkaJsonVO.class);

                    Global.currentWord = Global.charChokka.chemicalString;

                    webUrls[0] = Global.charChokka.wikiUrl;
                    webUrls[1] = Global.charChokka.youTubeUrl;

                    //------------------------------------------------------------
                    //download obj file
                    File writePath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/CharChokka");

                    if(writePath.exists() == false){
                        writePath.mkdir();
                    }

                    String objFilename = Global.charChokka._3dModelUrl.substring(Global.charChokka._3dModelUrl.lastIndexOf("/") + 1, Global.charChokka._3dModelUrl.length());

                    Global.objUrl = writePath.getAbsolutePath() + "/" + objFilename;

                    downloadThread = new DownloadThread(Global.charChokka._3dModelUrl, Global.objUrl);
                    downloadThread.start();
                    //------------------------------------------------------------
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class DownloadThread extends Thread{
        private String serverUrl;
        private String localPath;

        DownloadThread(String _serverUrl, String _localPath){
            this.serverUrl = _serverUrl;
            this.localPath = _localPath;
        }

        @Override
        public void run() {
//            super.run();

            URL url;
            int read;

            try {
                url = new URL(serverUrl);

                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                int len = conn.getContentLength();
                byte[] b = new byte[len];
                InputStream is = conn.getInputStream();
                File file = new File(localPath);
                FileOutputStream fos = new FileOutputStream(file);

                for(;;){
                    read = is.read(b);

                    if(read <= 0){
                        break;
                    }

                    fos.write(b, 0, read);
                }

                is.close();
                fos.close();
                conn.disconnect();
            }catch (MalformedURLException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }

            afterDownloadHandler.sendEmptyMessage(0);
        }
    }

    //다운로드 끝난 후 처리
    private final Handler afterDownloadHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
//            super.handleMessage(msg);

            downloadEffect.stop();

            //메뉴 보이기
            for(ImageView iv : ivMenus){
                iv.setVisibility(View.VISIBLE);
            }
        }
    };
    //------------------------------------------------

    //Saves the detected bounding boxes of chosen words to an internal file /data/data/com.example.[projectname]/files
    //access the directory using Device File Explorer
    public void saveToFile(String text, String FILE_NAME){
        FileOutputStream fos = null;
        try {
            //text = text + "\n";
            fos = openFileOutput(FILE_NAME, MODE_APPEND);
            fos.write(text.getBytes());

//            Log.i(text, getFilesDir()+"/"+FILE_NAME);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

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

    public boolean isReaction(String string){
        if(string.contains("+") && string.contains("=")){
//            Log.i("Equation Detected", "1011");
            return true;
        }
        return false;
    }

    //Print a dictionary
    public void printDictionary(Hashtable<String, Integer> dictionary){
//        Log.i("Printing", "Dictionary");

        for(String key : dictionary.keySet()) {

            int numberOfMolecules = dictionary.get(key);
//            Log.i(key, Integer.toString(numberOfMolecules));
        }

    }

    //populates a dictionary with an element as the key and its respective molecule numbers as the value
    public Hashtable<String, Integer> populateDictionary(String[] elements){

        Hashtable<String, Integer> dictionary = new Hashtable<String, Integer>();

        String chemical = ""; int number_of_molecule = -1;
        for(String e:elements){
            if(e.length() != 0){
                Character ch = e.charAt(0);
                if( Character.isLetter(ch)){
                    number_of_molecule = 1;
                    chemical = e;
                }else if(Character.isDigit(ch)){
                    number_of_molecule = Integer.parseInt(String.valueOf(ch));
                    chemical = e.substring(1);

                }

                dictionary.put(chemical, number_of_molecule);
            }
        }

        return dictionary;
    }

    public String getStringRepresentation(ArrayList<Character> list)
    {
        StringBuilder builder = new StringBuilder(list.size());
        for(Character ch: list)
        {
            builder.append(ch);
        }
        return builder.toString();
    }

    // Function to remove non-alphanumeric
    // characters from string
    public static String removeNonAlphanumeric(String str)
    {
        // replace the given string
        // with empty string
        // except the pattern "[^a-zA-Z0-9]"
        str = str.replaceAll(
                "[^a-zA-Z0-9]", "");

        // return string
        return str;
    }

    public ArrayList<Hashtable<String, Integer>> elementAnalysisGenerator(Set<String> elements){
        ArrayList<Hashtable<String, Integer>>elementAnalysis = new ArrayList<Hashtable<String, Integer>>();
        for(String e: elements){
//            Log.i("analysis", e);
            Hashtable<String, Integer>tempElement = new Hashtable<String, Integer>(); //key-element, value-number of atoms
            ArrayList<Character> elementCharacters = new ArrayList<Character>(); //tracks characters of element
            e = removeNonAlphanumeric(e);
            int indexOfElementString =0;
            int lengthOfElementString = e.length();

            while (indexOfElementString < lengthOfElementString){
                Character tempCh = e.charAt(indexOfElementString);
                if( Character.isLetter(tempCh)){
                    elementCharacters.add(tempCh);
                    if(indexOfElementString==lengthOfElementString-1){
                        String elementString = getStringRepresentation(elementCharacters);
//                        Log.i("analysis of element", "102");
//                        Log.i(elementString, Integer.toString(1));
                        tempElement.put(elementString, 1);
                        tempElement = new Hashtable<String, Integer>();
                        break;
                    }
                    indexOfElementString++;
                }
                else if(Character.isDigit(tempCh)){
                    String elementString = getStringRepresentation(elementCharacters);
//                    Log.i("analysis of element", "101");
//                    Log.i(elementString, String.valueOf(tempCh));
                    tempElement.put(elementString, Integer.parseInt(String.valueOf(tempCh)));
                    tempElement = new Hashtable<String, Integer>();
                    indexOfElementString++;
                }
            }
            elementAnalysis.add(tempElement);

        }

        return elementAnalysis;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_augmented_cam);

        cameraView = (SurfaceView) findViewById(R.id.surface_view);
        textView = (TextView) findViewById(R.id.text_view);

        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
//            Log.w("MainActivity", "Detector dependencies are not yet available");
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

                            ActivityCompat.requestPermissions(AugmentedCamActivity.this,
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
//                                    Log.i(left, top); Log.i(right, bottom);

                                    String tempString = stringBuilder.toString();
//                                    Log.i(tempString, "00");

                                    String toDetect = item.getValue().toLowerCase();

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

                                    if(isReaction(toDetect)){

                                        toDetect = toDetect.replaceAll("\\s+","");
                                        toDetect = toDetect.replaceAll("0", "o");
                                        String[] reactantProduct = toDetect.split("=");
                                        String reactant = reactantProduct[0]; String product = reactantProduct[1];
                                        String[] reactants = reactant.split("\\+"); //saves all reactants
                                        String[] products = product.split("\\+"); //saves all products

                                        // creating a My HashTable Dictionary
                                        Hashtable<String, Integer> reactantNumber = new Hashtable<String, Integer>();
                                        Hashtable<String, Integer> productNumber = new Hashtable<String, Integer>();

                                        //printReactantProducts(reactants, products); //Print all the detected reactants and products

                                        reactantNumber = populateDictionary(reactants);
                                        productNumber = populateDictionary(products);

                                        printDictionary(reactantNumber);
                                        printDictionary(productNumber);

                                        //Match with ideal reaction formulation to find errors in reactions
                                        boolean matched = true;
                                        for(String r:reactantNumber.keySet()){
                                            if(idealReactant.containsKey(r) ==false){
                                                matched =false;
//                                                Log.i("Mismatch Found", r);

                                                int startIndex = toDetect.indexOf(r);
                                                int newLeft = perCharacterSpace*startIndex + left_position;
                                                int newRight = newLeft + r.length()*perCharacterSpace;
                                                String coord = top +","+ Integer.toString(newLeft)+"-"+bottom+","+Integer.toString(newRight);

//                                                Log.i("Wrong Reactant", coord);

                                                String tempStrToSave =  r+":"+coord+"\n";

                                                saveToFile(tempStrToSave, REACTION_FILE_NAME);


                                            }else{
                                                int detectedNumber = reactantNumber.get(r);
                                                int idealNumber = idealReactant.get(r);
                                                if(detectedNumber != idealNumber){
                                                    matched= false;

                                                    String tempChemical = Integer.toString(detectedNumber)+r;
//                                                    Log.i("Mismatch Found", tempChemical);

                                                    int startIndex = toDetect.indexOf(tempChemical);
                                                    int newLeft = perCharacterSpace*startIndex + left_position;
                                                    int newRight = newLeft + tempChemical.length()*perCharacterSpace;
                                                    String coord = top +","+ Integer.toString(newLeft)+"-"+bottom+","+Integer.toString(newRight);

//                                                    Log.i("Wrong Reactant Mole no.", coord);

                                                    String tempStrToSave =  tempChemical+":"+coord+"\n";

                                                    saveToFile(tempStrToSave, REACTION_FILE_NAME);
                                                }
                                            }
                                        }
                                        if(matched){
                                            for(String p:productNumber.keySet()){
                                                if(idealProduct.containsKey(p)==false){
                                                    matched=false;
//                                                    Log.i("Mismatch Found", p);

                                                    int startIndex = toDetect.indexOf(p);
                                                    int newLeft = perCharacterSpace*startIndex + left_position;
                                                    int newRight = newLeft + p.length()*perCharacterSpace;
                                                    String coord = top +","+ Integer.toString(newLeft)+"-"+bottom+","+Integer.toString(newRight);

//                                                    Log.i("Wrong Product", coord);

                                                    String tempStrToSave =  p+":"+coord+"\n";

                                                    saveToFile(tempStrToSave, REACTION_FILE_NAME);
                                                }else{
                                                    int detectedNumber = productNumber.get(p);
                                                    int idealNumber = idealProduct.get(p);
                                                    if(detectedNumber != idealNumber){
                                                        matched= false;
                                                        String tempChemical = Integer.toString(detectedNumber)+p;
//                                                        Log.i("Mismatch Found", tempChemical);

//                                                        Log.i("Mismatch Found", tempChemical);

                                                        int startIndex = toDetect.indexOf(tempChemical);
                                                        int newLeft = perCharacterSpace*startIndex + left_position;
                                                        int newRight = newLeft + tempChemical.length()*perCharacterSpace;
                                                        String coord = top +","+ Integer.toString(newLeft)+"-"+bottom+","+Integer.toString(newRight);

//                                                        Log.i("Wrong Product Mole no.", coord);

                                                        String tempStrToSave =  tempChemical+":"+coord+"\n";

                                                        saveToFile(tempStrToSave, REACTION_FILE_NAME);

                                                    }
                                                }
                                            }
                                        }
                                        if(matched){
//                                            Log.i("Results","Matched");
                                        }
                                        else{
//                                            Log.i("Results","DId NOT match");
                                        }

                                    }else{

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
                                            saveToFile(stringToSend, FORMULA_FILE_NAME);

                                            //------------------------------------------------
                                            //JO's coding
                                            if(bDetected == false){
                                                detectedTexts.add(item);
                                            }
                                            //------------------------------------------------
                                        }


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

        if(textBlock != null){

        }

        //------------------------------------------------
        //JO's coding
        if(Global.charChokka != null){
            webUrls[0] = Global.charChokka.wikiUrl;
            webUrls[1] = Global.charChokka.youTubeUrl;
        }

        webView = findViewById(R.id.wv_web);
        webView.setWebViewClient(new WebViewClient());
        webView.setVisibility(View.INVISIBLE);

        ivMenus.add((ImageView)findViewById(R.id.iv_menu0));
        ivMenus.add((ImageView)findViewById(R.id.iv_menu1));
        ivMenus.add((ImageView)findViewById(R.id.iv_menu2));
        ivMenus.add((ImageView)findViewById(R.id.iv_menu3));

        //메뉴 숨기고 클릭 이벤트 설정
        for(ImageView iv : ivMenus){
            if(Global.charChokka == null){
                iv.setVisibility(View.INVISIBLE);
            }

            iv.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    for(int i=0; i<ivMenus.size(); i++){
                        if(v == ivMenus.get(i)){
                            if(i == 0 || i == 1 || i == 3){
                                Global.webUrl = webUrls[i];

                                startActivity(new Intent(getApplicationContext(), WebActivity.class));
                            }else if(i == 2){
                                startActivity(new Intent(getApplicationContext(), ModelActivity.class));
                            }

                            break;
                        }
                    }
                }
            });
        }

        paintLineBlue.setColor(0xff00bcd4);
        paintLineBlue.setStrokeWidth(10);

        paintLineWhite.setColor(0xffffffff);
        paintLineWhite.setStrokeWidth(4);

        tvCanvas = findViewById(R.id.textureView);
        tvCanvas.setOpaque(false);

        //해당 글자에 사각형 그리기
        tvCanvas.setSurfaceTextureListener(
                new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                        final Handler handler = new Handler();

                        //카메라와 화면의 해상도 비율
                        resolutionRatioW = (float)width / cameraSource.getPreviewSize().getHeight();
                        resolutionRatioH = (float)height / cameraSource.getPreviewSize().getWidth();

                        handler.postDelayed(new Runnable() {
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

                                        //해당 글자 전체를 감싸는 흰 사각형
                                        canvas.drawLine(rect.left, rect.top, rect.right, rect.top, paintLineWhite);
                                        canvas.drawLine(rect.right, rect.top, rect.right, rect.bottom, paintLineWhite);
                                        canvas.drawLine(rect.right, rect.bottom, rect.left, rect.bottom, paintLineWhite);
                                        canvas.drawLine(rect.left, rect.bottom, rect.left, rect.top, paintLineWhite);

                                        //사각형의 모서리
                                        canvas.drawLine(rect.left, rect.top, rect.left + 40, rect.top, paintLineBlue);
                                        canvas.drawLine(rect.left, rect.top, rect.left, rect.top + 40, paintLineBlue);
                                        canvas.drawLine(rect.right, rect.top, rect.right - 40, rect.top, paintLineBlue);
                                        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + 40, paintLineBlue);
                                        canvas.drawLine(rect.right, rect.bottom, rect.right - 40, rect.bottom, paintLineBlue);
                                        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - 40, paintLineBlue);
                                        canvas.drawLine(rect.left, rect.bottom, rect.left + 40, rect.bottom, paintLineBlue);
                                        canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - 40, paintLineBlue);

                                        //터치를 위한 글자 영역 저장
                                        textAreas.add(new TextAreaVO(tb.getValue(), rect));
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

        //글자 터치
        tvCanvas.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP){
                    float x = event.getX();
                    float y = event.getY();

                    for(TextAreaVO ta : textAreas){
                        if(ta.rect.left < x && x < ta.rect.right
                                && ta.rect.top < y && y < ta.rect.bottom
                        ){
                            //다운로드 효과
                            downloadEffect = new LoadingCircleEffect(R.drawable.download, getBaseContext(), findViewById(R.id.fl_circle_effect));

                            //다운로드 효과를 1초 동안 보여주고 실제 다운로드 시작
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    NetworkTask networkTask = new NetworkTask("http://www.oasisvisa.com/gsenOcr/CharChokka.php?reqText=" + ta.value, null);
                                    networkTask.execute();
                                }
                            }, 1000);
                        }
                    }
                }

                return true;
            }
        });

        findViewById(R.id.iv_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), HomeActivity.class));
            }
        });
        //------------------------------------------------
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();

        if(webView.getVisibility() == View.VISIBLE){
            webView.setVisibility(View.INVISIBLE);
        }
    }

}