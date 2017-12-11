package com.sparohealth.wingkit_sample;

/**
 * Created by darien.sandifer on 10/27/2017.
 */

public class ChecklistItem {

        private String label;
        private boolean isChecked;

        public ChecklistItem(String name, boolean isChecked) {
            this.label = name;
            this.isChecked = isChecked;
        }

        public String getLabelName() {
            return this.label;
        }

        public boolean getIsChecked() {
            return isChecked;
        }

        public void setChecked(boolean newValue){
            isChecked = newValue;
        }
        public  void setLabel(String newValue){
            label = newValue;
        }
}
