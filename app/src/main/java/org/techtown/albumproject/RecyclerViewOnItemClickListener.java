package org.techtown.albumproject;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Developer on 2018-04-04.
 * RecyclerView 터치 클래스입니다.
 * short Click, long Click
 */

public class RecyclerViewOnItemClickListener extends RecyclerView.SimpleOnItemTouchListener {
    private OnItemClickListener mListener;
    private GestureDetector mGestureDector;
    public RecyclerViewOnItemClickListener(Context context, final RecyclerView recyclerView, OnItemClickListener listener){
        this.mListener=listener;
        mGestureDector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){

            @Override
            public boolean onSingleTapUp(MotionEvent e){
               return true;
           }

           @Override
            public void onLongPress(MotionEvent e){
               View childView = recyclerView.findChildViewUnder(e.getX(),e.getY());
               if(childView!=null && mListener != null){
                   mListener.onItemLongClick(childView,recyclerView.getChildAdapterPosition(childView));
               }
           }

        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e){
        View child = rv.findChildViewUnder(e.getX(),e.getY());
        if(child != null && mListener != null && mGestureDector.onTouchEvent(e)){
            mListener.onItemClick(child,rv.getChildAdapterPosition(child));
            return true;
        }
        return false;
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
        void onItemLongClick(View v, int position);
    }
}
