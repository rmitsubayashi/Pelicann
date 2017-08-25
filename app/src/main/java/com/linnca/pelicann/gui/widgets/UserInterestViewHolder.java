package com.linnca.pelicann.gui.widgets;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.linnca.pelicann.R;
import com.linnca.pelicann.db.datawrappers.WikiDataEntryData;

//holder for user interest list cells
public class UserInterestViewHolder  extends RecyclerView.ViewHolder {
    private WikiDataEntryData wikiDataEntryData;
    private final TextView label;
    private final TextView description;
    /*
    * Ideally we wouldn't have a button to delete but instead use a context action mode
    * and allow multiple selections.
    * But doesn't allowing multiple selections conflict with the nature of
    * real time databases??
    * */
    private final Button deleteButton;

    public UserInterestViewHolder(View itemView) {
        super(itemView);
        label = (TextView) itemView.findViewById(R.id.user_interests_list_item_label);
        description = (TextView) itemView.findViewById(R.id.user_interests_list_item_description);
        deleteButton = (Button) itemView.findViewById(R.id.user_interests_list_item_delete);
    }


    public void setLabel(String label) {
        this.label.setText(label);
    }

    public void setDescription(String description) {
        this.description.setText(description);
    }

    public void setButtonListener(View.OnClickListener listener){
        deleteButton.setOnClickListener(listener);
    }

    public void setWikiDataEntryData(WikiDataEntryData data){
        this.wikiDataEntryData = data;
    }

    public WikiDataEntryData getWikiDataEntryData(){
        return this.wikiDataEntryData;
    }

}