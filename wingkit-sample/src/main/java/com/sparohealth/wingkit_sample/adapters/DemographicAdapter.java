package com.sparohealth.wingkit_sample.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.sparohealth.wingkit_sample.DemographicsActivity;
import com.sparohealth.wingkit_sample.R;
import java.util.List;

/**
 * Created by darien.sandifer on 10/25/2017.
 */

public class DemographicAdapter extends BaseAdapter{
    List<DemographicsActivity.DemographicsItem> dataItems = null;

    public DemographicAdapter(List<DemographicsActivity.DemographicsItem> items) {
        dataItems = items;
    }

//    // Returns the number of types of Views that will be created by getView(int, View, ViewGroup)
//    @Override
//    public int getViewTypeCount() {
//        // Returns the number of types of Views that will be created by this adapter
//        // Each type represents a set of views that can be converted
//
//        return  4;
//    }
//
//    // Get the type of View that will be created by getView(int, View, ViewGroup)
//    // for the specified item.
//    @Override
//    public int getItemViewType(int position) {
//        // Return an integer here representing the type of View.
//        // Note: Integers must be in the range 0 to getViewTypeCount() - 1
//
//        return 2;
//    }

    @Override
    public int getCount() {
        return dataItems.size();
    }

    @Override
    public Object getItem(int i) {
        return dataItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    // Get a View that displays the data at the specified position in the data set.
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.demographics_row, parent, false);
        }

        TextView lblTitle = convertView.findViewById(R.id.lblTitle);
        TextView lblValue = convertView.findViewById(R.id.lblValue);

        DemographicsActivity.DemographicsItem dataItem = dataItems.get(position);

        lblTitle.setText(dataItem.title);
        lblValue.setText(dataItem.display);

        return convertView;
    }
}



