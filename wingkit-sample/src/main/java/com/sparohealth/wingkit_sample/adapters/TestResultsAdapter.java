package com.sparohealth.wingkit_sample.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sparohealth.wingkit_sample.R;
import com.sparohealth.wingkit_sample.TestResultItem;

import java.util.ArrayList;

/**
 * Created by darien.sandifer on 11/7/2017.
 */

public class TestResultsAdapter extends BaseAdapter {
    private ArrayList<TestResultItem> items;
    private Context context;
    private LayoutInflater inflater;


    public TestResultsAdapter(Context context, ArrayList<TestResultItem> newItems) {
        this.items = newItems;
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public TestResultItem getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        if (view == null){
            view = inflater.inflate(R.layout.test_results_row,viewGroup,false);
        }

        TestResultItem currentItem = getItem(position);
        TextView resultLabel = view.findViewById(R.id.resultLabel);
        TextView resultValue = view.findViewById(R.id.resultValue);
        resultLabel.setText(currentItem.label);
        resultValue.setText(currentItem.value);

        return  view;
    }
}
