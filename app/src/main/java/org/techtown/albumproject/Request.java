package org.techtown.albumproject;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

/**
 * Created by Developer on 2018-04-02.
 */

public interface Request {

    //폴더 및 파일 리스트 요청하기
    @GET("fileList.php")
    Call<List<Linux_File_Item>> getFolder(
        @Query("userID")String userID,
        @Query("folderName")String folderName
    );

    //폴더 만들기
    @GET("createDir.php")
    Call<ResponseBody> createDir(
            @Query("userID")String userID,
            @Query("path")String path,
            @Query("dirName")String dirName
    );

    //파일 이동
    @POST("moveFile.php")
    Call<ResponseBody> mvFile(
            @Part("userID")String userID,
            @Part("sourcePath")String sourcePath,
            @Part("targetPath")String targetPath
    );
}
