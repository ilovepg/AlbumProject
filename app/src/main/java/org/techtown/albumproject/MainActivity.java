package org.techtown.albumproject;

import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private NetworkInfo networkInfo ;       //네트워크 상태 확인

    /*UI관련*/
    private RecyclerView recyclerView;      //폴더들이 나열될 RecyclerView
    private MainRecyclerViewAdapter adapter_main;//RecyclerView어댑터
    private List<Linux_File_Item> file_list;
    private RecyclerView.LayoutManager mLayoutManager;
    private Button backBtn;                 //뒤로가기 버튼
    private int editMode=0;                 //편집모드 0:일반, 1:편집

    //리눅스 경로
    private String basePath="/var/www/html/albumProject/album/"; //베이스 경로
    private ArrayList<String> pathDepth=new ArrayList<String>();
    private String currentPath=""; //basePath+pathDepth 따라서 현재 작업 디렉토리임.

    //기타
    StringTokenizer st;
    private static final String TAG = "MainActivity";

    //메뉴타입 정의
    int menu_type = 0;

    /* 연진 추가 */
    private LinearLayout bottomSheet;
    private BottomSheetBehavior mBottomSheetBehavior;

    private RecyclerView recyclerView_move;
    private MoveRecyclerViewAdpater adapter_move;
    private List<Linux_File_Item> file_list_move;
    private RecyclerView.LayoutManager mLayoutManager_move;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //네트워크에 연결되어있지 않으면 프로그램을 종료한다.
        networkInfo=getNetworkInfo();
        if(!networkInfo.isAvailable()){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            AlertDialog dialog = builder.setMessage("네트워크에 연결해주세요.")
                    .setNegativeButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.finishAffinity(MainActivity.this); //어느 Activity에서든 모든 부모 Activity를 닫을 수 있다.
                        }
                    })
                    .create();
            dialog.show();
        }

        /*RecyclerView, 뒤로가기 Button*/
        recyclerView = (RecyclerView) findViewById(R.id.main_recyclerView);
        mLayoutManager=new LinearLayoutManager(MainActivity.this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        backBtn = (Button)findViewById(R.id.main_backBtn); //뒤로가기 버튼
        backBtn.setVisibility(View.GONE); //처음에는 안보이게한다. (Root 디렉토리이기 때문에 뒤로갈 수 없게)

        getData("test",basePath); //서버에서 데이터 가져오기

        recyclerView.addOnItemTouchListener(new RecyclerViewOnItemClickListener(MainActivity.this, recyclerView, new RecyclerViewOnItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                editMode=adapter_main.getEditMode();
                if(editMode==0){ //편집모드가 아닐 때
                    if(file_list.get(position).getFileType()==0){
                        //파일타입이 폴더(디렉토리)일때
                        String folderName=file_list.get(position).getFileName();
                        currentPath="";
                        currentPath+=basePath;

                        //pathDepth 만큼 Path를 더해준다.
                        for(int i = 0; i< pathDepth.size(); i++){
                            currentPath+=(pathDepth.get(i)+"/");
                        }
                        currentPath+=folderName; //내가 클릭한 디렉토리 이름까지 붙여주면 끝.
                        getData("test", currentPath);
                    }
                }else{            //편집모드 일때
                    boolean isChecked = file_list.get(position).isChecked(); //클릭한 아이템의 check 상태를 가져온다.
                    file_list.get(position).setChecked(!isChecked);          //가져온 check 상태와 반대의 값을 넣는다. (check됐으면 check안되게, check안됐으면 check되게)
                    adapter_main.notifyItemChanged(position,file_list.get(position)); //adapter 새로고침(해당 포지션만)
                }
            }

            @Override
            public void onItemLongClick(View v, int position) {
                editMode=adapter_main.getEditMode();
                if(editMode==0){ //편집모드가 일반일 때만 편집모드에 진입할 수 있다.
                    switchEditMode();
                }
            }
        }));

        /* 연진 추가 시작 (BottomSheet) */
        bottomSheet = (LinearLayout) findViewById(R.id.bottomMove_bottomSheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        /*RecyclerView 셋팅*/
        recyclerView_move = (RecyclerView) findViewById(R.id.bottomMove_recyclerView);
        mLayoutManager_move=new LinearLayoutManager(MainActivity.this);
        recyclerView_move.setLayoutManager(mLayoutManager_move);
        recyclerView_move.setItemAnimator(new DefaultItemAnimator());

        /*버튼 클릭 리스너*/
        Button bottomMove_cancleBtn = (Button) findViewById(R.id.bottomMove_cancleBtn);
        Button bottomMove_createFolder = (Button) findViewById(R.id.bottomMove_createFolder);
        Button bottomMove_submit = (Button) findViewById(R.id.bottomMove_submitBtn);

        //취소 버튼 클릭하면 BottomSheet 사라짐
        bottomMove_cancleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        //RecyclerView 버튼 클릭
        // 터치를 하고 손을 땔떼만 값을 얻을 수 있도록 추가
        final GestureDetector gestureDetector = new GestureDetector(MainActivity.this ,new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });

        recyclerView_move.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                View child = rv.findChildViewUnder(e.getX(),e.getY());
                if(child!=null && gestureDetector.onTouchEvent(e)){
                    if(file_list_move.get(rv.getChildAdapterPosition(child)).getFileType()==0){
                        //파일 타입이 폴더라면
                        String folderName = file_list_move.get(rv.getChildAdapterPosition(child)).getFileName();
                        currentPath = "";
                        currentPath+=basePath;

                        //pathDepth 만큼 Path를 더해준다.
                        for(int i = 0; i< pathDepth.size(); i++){
                            currentPath+=(pathDepth.get(i)+"/");
                        }
                        currentPath+=folderName; //내가 클릭한 디렉토리 이름까지 붙여주면 끝.
                        getFileList("test",currentPath);
                    }
                }
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        //파일 리스트를 가져온다.
        getFileList("test", basePath);

        /* 연진 추가 끝 */
    }

    //networkinfo를 얻어오는 메소드
    private NetworkInfo getNetworkInfo(){
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }

    //안드로이드 ↔ 서버 통신 (파일과 폴더 리스트를 가져온다.)
    private void getData(String userID, String folderName){
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("http://52.78.47.225/albumProject/")
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();

        Log.e(TAG,"getData CurrentPath->"+folderName);

        final Request request = retrofit.create(Request.class);
        Call<List<Linux_File_Item>>call = request.getFolder(userID,folderName);
        call.enqueue(new Callback<List<Linux_File_Item>>() {
            @Override
            public void onResponse(Call<List<Linux_File_Item>> call, Response<List<Linux_File_Item>> response) {
                if(response.isSuccessful()){ //호출성공
                    file_list=response.body();
                    adapter_main = new MainRecyclerViewAdapter(file_list,R.layout.linux_folder_item,MainActivity.this);
                    recyclerView.setAdapter(adapter_main);
                    adapter_main.notifyDataSetChanged();

                    // '/'를 기준으로 나눈다.
                    st=new StringTokenizer(file_list.get(0).getPwd(),"/");

                    //pathDepth에 기존의 정보가 중복되지 않게 clear
                    pathDepth.clear();

                    //pathDepth에 넣는다.
                    while(st.hasMoreTokens()){
                        pathDepth.add(st.nextToken()); //현재 작업 디렉토리를 저장.
                    }

                    //만약 Root 디렉토리라면 뒤로가기 버튼을 없앤다.
                    if(pathDepth.size()==0){
                        backBtn.setVisibility(View.GONE);
                    }else{
                        backBtn.setVisibility(View.VISIBLE);
                    }

                    /* //Debug
                    for(int i=0;i<file_list.size();i++){
                        Log.e(TAG,"getData->"+file_list.get(i).getFileName());
                        Log.e(TAG,"getData under->"+file_list.get(i).getUnderFolderNumber());
                        Log.e(TAG,"getData path->"+file_list.get(i).getPwd());
                    }*/
                }else{
                    int statusCode = response.code();
                    Log.e(TAG,"getData statusCode"+statusCode);
                }
            }

            @Override
            public void onFailure(Call<List<Linux_File_Item>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "fail", Toast.LENGTH_SHORT).show();
                Log.e(TAG,"getData Fail"+t.toString());
            }
        });
    }

    //안드로이드 ↔ 서버 통신 (디렉토리를 만든다.)
    private void createDir(String dirName, final AlertDialog dialog){

        if(currentPath==null || currentPath.equals("")){ //currentPath에 아무것도 없을 때(즉, root디렉토리일 때)
            currentPath=basePath;
        }

        final Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("http://52.78.47.225/albumProject/")
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        Request request = retrofit.create(Request.class);
        final Call<ResponseBody>call=request.createDir("test",currentPath,dirName);

        //동기호출
        new AsyncTask<Void,Void,Void>(){
            boolean result=false; //result에 따라서 onPostExecute에서 분기됩니다.

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Response<ResponseBody>response=call.execute(); //Retrofit동기 실행 및 응답받기
                    if(response.isSuccessful()){
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        String jsonResult = jsonObject.getString("result");
                        if(jsonResult.equals("success")){
                            result=true;
                        }
                    }else{
                        int statusCode=response.code();
                        Log.e(TAG,"createDir statusCode->"+statusCode);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if(result){
                    dialog.dismiss();
                    getData("test",currentPath);
                    Toast.makeText(MainActivity.this, "폴더가 생성되었습니다.", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MainActivity.this, "폴더가 생성되지 않았습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    // 메뉴 동적생성
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {  // 유저가 메뉴 버튼을 누를때마다 호출되는 메소드
        menu.clear(); // 현재메뉴 Clear
        MenuInflater inflater = getMenuInflater();

        switch (menu_type) {
            case 0: // 메인메뉴 호출
                inflater.inflate(R.menu.main_menu, menu);
                break;

            case 1: // 편집메뉴 호출
                inflater.inflate(R.menu.edited_menu, menu);
                break;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    //메뉴 클릭 메소드
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_createFolder){ //폴더를 만드는 메뉴
            final EditText folderName = new EditText(MainActivity.this);
            final AlertDialog.Builder createFolderPopup = new AlertDialog.Builder(MainActivity.this);
            createFolderPopup.setTitle("폴더의 이름을 입력해주세요.")
                    .setView(folderName)
                    .setPositiveButton("생성", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    })
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            return ;
                        }
                    });
            final AlertDialog dialog=createFolderPopup.create();
            dialog.show();

            //POSITIVE버튼을 재정의해서 패턴에 걸리면 dialog가 dismiss되지 않게 한다.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    String input = folderName.getText().toString();
                    String pattern = "^[a-zA-Z0-9가-힣ㄱ-ㅎ]*$"; //영문,숫자,한글만 가능
                    if (!Pattern.matches(pattern, input)) {
                        Toast.makeText(MainActivity.this, "영문,숫자,한글만 입력해주세요.", Toast.LENGTH_SHORT).show();
                    } else {
                        createDir(input,dialog);
                    }
                }
            });
        }else if(id==R.id.action_editFolder){ //편집모드를 여는 메뉴
            switchEditMode();
        }else if(id==R.id.action_transferFolder){ //파일을 이동하는 메뉴
            //MoveBottomSheet moveBottomSheet = MoveBottomSheet.getInstance();
            //moveBottomSheet.show(getSupportFragmentManager(),"moveBottomSheet");

            /* 연진 추가 (BottomSheet) */

            // 파일이동 클릭하면 BottomSheet 확장
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }else if(id==R.id.action_copyFolder){  //파일을 복사하는 메뉴

        }

        return super.onOptionsItemSelected(item);
    }

    //클릭 메소드
    public void mainLayoutClicks(View v){
        switch(v.getId()){
            case R.id.main_backBtn:{ //뒤로가기 버튼
                goToBack();          //뒤로가기 메소드 호출
                break;
            }
        }
    }

    //스마트폰의 backButton을 눌렀을 때
    @Override
    public void onBackPressed() {
        editMode=adapter_main.getEditMode(); //현재의 편짐모드 상태를 가져온다.
        if(editMode==0){         //편집모드가 비활성화라면
            if(backBtn.getVisibility()==View.GONE){
                super.onBackPressed(); //만약 뒤로가기 버튼이 안보인다면 종료
            }else{
                goToBack();            //뒤로가기 메소드를 호출한다.
            }
        }else{                   //편집모드가 활성화 라면
            //편집모드를 비활성화 시킨다.
            editMode=0;
            menu_type=editMode;
            adapter_main.setEditMode(editMode);
            //adapter에게 알린다.
            adapter_main.notifyDataSetChanged();
        }
    }

    //디렉토리 뒤로가기 메소드
    private void goToBack(){
        pathDepth.remove(pathDepth.size()-1); //마지막 path를 삭제

        currentPath="";
        currentPath+=basePath;

        //pathDepth 만큼 Path를 더해준다.
        for(int i = 0; i< pathDepth.size(); i++){
            currentPath+=(pathDepth.get(i)+"/");
        }
        getData("test",currentPath);
    }

    //편집모드로 바꾸는 메소드
    private void switchEditMode(){
        editMode=1;  //편집모드 0:일반, 1:편집
        menu_type=editMode; //menu_type을 바꾼다.
        adapter_main.setEditMode(editMode);
        adapter_main.notifyDataSetChanged();
    }

    /* 연진 추가 (BottomSheet) */

    //파일 리스트를 가져온다. (안드로이드 ↔ 서버)
    private void getFileList(String userID, String folderName){
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl("http://52.78.47.225/albumProject/")
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();

        Log.e(TAG,"getFileList CurrentPath->"+folderName);

        final Request request = retrofit.create(Request.class);
        Call<List<Linux_File_Item>> call = request.getFolder(userID,folderName);
        call.enqueue(new Callback<List<Linux_File_Item>>() {
            @Override
            public void onResponse(Call<List<Linux_File_Item>> call, Response<List<Linux_File_Item>> response) {
                if(response.isSuccessful()){ //호출성공
                    file_list_move=response.body();
                    adapter_move = new MoveRecyclerViewAdpater(file_list_move,R.layout.move_item, getApplicationContext());
                    recyclerView_move.setAdapter(adapter_move);
                    adapter_move.notifyDataSetChanged();

                    // '/'를 기준으로 나눈다.
                    st=new StringTokenizer(file_list_move.get(0).getPwd(),"/");

                    //pathDepth에 기존의 정보가 중복되지 않게 clear
                    pathDepth.clear();

                    //pathDepth에 넣는다.
                    while(st.hasMoreTokens()){
                        pathDepth.add(st.nextToken()); //현재 작업 디렉토리를 저장.
                    }

                    /*//Debug
                    for(int i=0;i<file_list.size();i++){
                        Log.e(TAG,"getFileList->"+file_list.get(i).getFileName());
                        Log.e(TAG,"getFileList under->"+file_list.get(i).getUnderFolderNumber());
                        Log.e(TAG,"getFileList path->"+file_list.get(i).getPwd());
                    }*/
                }else{
                    int statusCode = response.code();
                    Log.e(TAG,"getFileList statusCode"+statusCode);
                }
            }

            @Override
            public void onFailure(Call<List<Linux_File_Item>> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "fail", Toast.LENGTH_SHORT).show();
                Log.e(TAG,"getFileList Fail"+t.toString());
            }
        });
    }
}
