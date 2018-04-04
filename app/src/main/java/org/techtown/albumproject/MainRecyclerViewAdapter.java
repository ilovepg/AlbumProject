package org.techtown.albumproject;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Developer on 2018-04-01.
 */

public class MainRecyclerViewAdapter extends RecyclerView.Adapter<MainRecyclerViewAdapter.ViewHolder> {

    private List<Linux_File_Item> file_list;
    private int itemLayout;
    private Context context;

    public MainRecyclerViewAdapter(List<Linux_File_Item> file_list, int itemLayout, Context context) {
        this.file_list = file_list;
        this.itemLayout = itemLayout;
        this.context=context;
    }

    @Override
    public MainRecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(itemLayout,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MainRecyclerViewAdapter.ViewHolder holder, int position) {
        final Linux_File_Item item = file_list.get(position);
        holder.fileName.setText(item.getFileName());
        holder.underFolderNumber.setText(String.valueOf(item.getUnderFolderNumber()));

        //하위 폴더가 있으면 Image를 교체해준다.
        if(item.getUnderFolderNumber()>0){
          holder.fileImg.setImageResource(R.drawable.folder);
        }
    }

    @Override
    public int getItemCount() {
        return file_list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        public ImageView fileImg;
        public TextView fileName;
        public TextView underFolderNumber;
        public CheckBox fileChecked;

        public ViewHolder(View itemView) {
            super(itemView);
            fileImg = (ImageView)itemView.findViewById(R.id.folder_img);
            fileName = (TextView)itemView.findViewById(R.id.folder_name);
            underFolderNumber = (TextView)itemView.findViewById(R.id.underFolder_number);
            fileChecked = (CheckBox) itemView.findViewById(R.id.folder_checkBox);
        }
    } // end of ViewHolder

}
