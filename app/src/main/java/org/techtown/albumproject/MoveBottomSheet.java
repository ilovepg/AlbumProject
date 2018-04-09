package org.techtown.albumproject;

import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Developer on 2018-04-05.
 */

public class MoveBottomSheet extends BottomSheetDialogFragment{

    /*UI관련*/
    private RecyclerView recyclerView;
    private MoveRecyclerViewAdpater adapter;
    private List<Linux_File_Item> file_list;
    private RecyclerView.LayoutManager mLayoutManager;

    //리눅스 경로
    private String basePath="/var/www/html/albumProject/album/"; //베이스 경로
    private ArrayList<String> pathDepth=new ArrayList<String>();
    private String currentPath=""; //basePath+pathDepth 따라서 현재 작업 디렉토리임.

    //기타
    StringTokenizer st;
    private static final String TAG = "MoveBottomSheet";

    //CallBack
    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallBack=new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            switch (newState){
                case BottomSheetBehavior.STATE_COLLAPSED:{
                    Log.e("state","COLLAPSED");
                    break;
                }
                case BottomSheetBehavior.STATE_DRAGGING: {
                    Log.e("state","STATE_DRAGGING");
                    break;
                }
                case BottomSheetBehavior.STATE_EXPANDED: {
                    Log.e("state","STATE_EXPANDED");
                    break;
                }
                case BottomSheetBehavior.STATE_HIDDEN: {
                    Log.e("state","STATE_HIDDEN");
                    dismiss();
                    break;
                }
            }



        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {

        }
    };

    //생성자
    public MoveBottomSheet() {

    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(),R.layout.bottom_move,null);
        dialog.setContentView(contentView);
        CoordinatorLayout.LayoutParams layoutParams =
                (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = layoutParams.getBehavior();
        if(behavior != null && behavior instanceof BottomSheetBehavior){
            ((BottomSheetBehavior)behavior).setBottomSheetCallback(mBottomSheetBehaviorCallBack);
            ((BottomSheetBehavior) behavior).setPeekHeight(1000);

        }

        /*RecyclerView 셋팅*/
        recyclerView = (RecyclerView) contentView.findViewById(R.id.bottomMove_recyclerView);
        mLayoutManager=new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setNestedScrollingEnabled(false);

        /*버튼 클릭 리스너*/
        Button bottomMove_cancleBtn = (Button)contentView.findViewById(R.id.bottomMove_cancleBtn);
        Button bottomMove_createFolder = (Button)contentView.findViewById(R.id.bottomMove_createFolder);
        Button bottomMove_submit = (Button)contentView.findViewById(R.id.bottomMove_submitBtn);

        //취소
        bottomMove_cancleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        //RecyclerView 버튼 클릭
        // 터치를 하고 손을 땔떼만 값을 얻을 수 있도록 추가
        final GestureDetector gestureDetector = new GestureDetector(getContext(),new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                View child = rv.findChildViewUnder(e.getX(),e.getY());
                if(child!=null && gestureDetector.onTouchEvent(e)){
                    if(file_list.get(rv.getChildAdapterPosition(child)).getFileType()==0){
                        //파일 타입이 폴더라면
                        String folderName=file_list.get(rv.getChildAdapterPosition(child)).getFileName();
                        currentPath="";
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

        getFileList("test",basePath); // 파일리스트를 불러온다.
    }

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
                    file_list=response.body();
                    adapter = new MoveRecyclerViewAdpater(file_list,R.layout.move_item,getContext());
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();

                    // '/'를 기준으로 나눈다.
                    st=new StringTokenizer(file_list.get(0).getPwd(),"/");

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
                Toast.makeText(getActivity(), "fail", Toast.LENGTH_SHORT).show();
                Log.e(TAG,"getFileList Fail"+t.toString());
            }
        });
    }

}
