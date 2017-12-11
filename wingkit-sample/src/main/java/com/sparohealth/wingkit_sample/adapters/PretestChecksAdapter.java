package com.sparohealth.wingkit_sample.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import com.sparohealth.wingkit_sample.ChecklistItem;
import com.sparohealth.wingkit_sample.R;

import java.util.ArrayList;
import java.util.zip.Inflater;

/**
 * Created by darien.sandifer on 10/27/2017.
 */

public class PretestChecksAdapter extends BaseAdapter {
    private ArrayList<ChecklistItem> items;
    private Context context;
    private LayoutInflater inflater;

    public PretestChecksAdapter(Context context, ArrayList<ChecklistItem> newItems) {
        this.items = newItems;
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public ChecklistItem getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {

        if (view == null){
            view = inflater.inflate(R.layout.pretest_row, viewGroup,false);
        }

        CheckedTextView row = (CheckedTextView) view.findViewById(R.id.pretestCheckedTextView);
        ChecklistItem currentItem = getItem(position);
        row.setText(currentItem.getLabelName());
        row.setChecked(currentItem.getIsChecked());

        //disable selection
        view.setEnabled(false);

        return view;
    }
}
