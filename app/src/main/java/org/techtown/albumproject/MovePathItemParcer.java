package org.techtown.albumproject;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;


/**
 * Created by Developer on 2018-04-09.
 * ArrayList<String>은 Parcelable 되지 못해서 따로 클래스를 하나 만들어야 했습니다.
 */

public class MovePathItemParcer implements Parcelable {

    private ArrayList<String> selectedItemPath; //선택된 파일들의 경로

    //클래스를 만들었을 때의 생성자
    public MovePathItemParcer(ArrayList<String> selectedItemPath) {
        this.selectedItemPath = selectedItemPath;
    }

    //ArrayList반환
    public ArrayList<String> getArrayList(){
        return selectedItemPath;
    }

    protected MovePathItemParcer(Parcel in) {
        //selectedItemPath = in.createStringArrayList();
        selectedItemPath = in.readArrayList(String.class.getClassLoader());
    }

    //실제 오브젝트 serialization/flattening을 하는 메소드. 오브젝트의 각 엘리먼트를 각각 parcel해줘야 한다.
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(selectedItemPath);
    }

    //describeContents() - Parcel 하려는 오브젝트의 종류를 정의한다.
    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MovePathItemParcer> CREATOR = new Creator<MovePathItemParcer>() {
        @Override
        public MovePathItemParcer createFromParcel(Parcel in) {
            return new MovePathItemParcer(in);
        }

        @Override
        public MovePathItemParcer[] newArray(int size) {
            return new MovePathItemParcer[size];
        }
    };

}
