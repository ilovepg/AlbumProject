package org.techtown.albumproject;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Developer on 2018-04-01.
 * 서버에게 응답받은 폴더 이름, 하위 폴더 개수 들이 들어갈 아이템 클래스입니다.
 */


public class Linux_File_Item {

    @SerializedName("folder_name")
    @Expose
    private String fileName;      //파일 이름

    private int underFolderNumber;//하위 파일의 개수
    private int fileType;         //fileType을 알려주는 변수 0이면 폴더 1이면 파일
    private String pwd;           //현재 파일이 있는 디렉토리(작업 디렉토리)
    private boolean isChecked;    //현재 아이템의 CheckBox가 CheckBox

    public Linux_File_Item(String fileName, int underFolderNumber, int fileType, String pwd) {
        this.fileName = fileName;
        this.underFolderNumber = underFolderNumber;
        this.fileType = fileType;
        this.pwd = pwd;
    }

    public String getPwd() {
        return pwd;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public int getUnderFolderNumber() {
        return underFolderNumber;
    }

    public void setUnderFolderNumber(int underFolderNumber) {
        this.underFolderNumber = underFolderNumber;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
