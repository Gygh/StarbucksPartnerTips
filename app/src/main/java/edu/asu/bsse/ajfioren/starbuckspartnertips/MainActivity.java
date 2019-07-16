package edu.asu.bsse.ajfioren.starbuckspartnertips;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Copyright (c) 2019 /u/Gygh,
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author /u/Gygh ajfioren@asu.edu
 * @version July 16, 2019
 */
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener,
        DialogInterface.OnClickListener, TextView.OnEditorActionListener  {

    private Button addButt, removeButt;
    private EditText nameET, tippableHoursET, tipsPerHourET, tipsET;
    private Spinner selectSpinner1;
    private String selectedPartner;
    private String[] partners;
    private ArrayAdapter<String> partnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addButt = (Button)findViewById(R.id.addButt);
        removeButt = (Button)findViewById(R.id.removeButt);
        nameET = (EditText)findViewById(R.id.nameET);
        tippableHoursET = (EditText)findViewById(R.id.tippableHoursET);
        tipsPerHourET = (EditText)findViewById(R.id.tipsPerHourET);
        tipsET = (EditText)findViewById(R.id.tipsET);
        selectSpinner1 = (Spinner)findViewById(R.id.selectSpinner1);

        this.selectedPartner = this.setupselectSpinner1();
        loadFields();
    }

    /**
     * setupselectSpinner1
     * Sets up the spinner object and populates fields accordingly
     *
     * @return ret returns an array of partner objects or unknown if empty
     */
    private String setupselectSpinner1(){
        String ret = "unknown";
        try{
            PartnerDB db = new PartnerDB((Context)this);
            SQLiteDatabase plcDB = db.openDB();
            Cursor cur = plcDB.rawQuery("select name from partner;", new String[]{});
            ArrayList<String> al = new ArrayList<String>();
            while(cur.moveToNext()){
                try{
                    al.add(cur.getString(0));
                }catch(Exception ex){
                    android.util.Log.w(this.getClass().getSimpleName(),"exception stepping thru cursor"+ex.getMessage());
                }
            }
            partners = (String[]) al.toArray(new String[al.size()]);
            ret = (partners.length>0 ? partners[0] : "unknown");
            partnerAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item, partners);
            selectSpinner1.setAdapter(partnerAdapter);
            selectSpinner1.setOnItemSelectedListener(this);
        }catch(Exception ex){
            android.util.Log.w(this.getClass().getSimpleName(),"unable to setup partner spinner");
        }
        return ret;
    }

    /**
     * loadFields
     * Populates the ET fields with the partner's name, their tippable hours, the dollar per hour
     * amount of tips, and the partners total tips for this session.
     */
    private void loadFields(){
        try{
            PartnerDB db = new PartnerDB((Context)this);
            SQLiteDatabase crsDB = db.openDB();
            Cursor cur = crsDB.rawQuery("select tippableHours, tipsPerHour  from partner where name=?",
                    new String[]{selectedPartner});
            double tippableHours = 0.00;
            double tipsPerHour = 0.00;

            while (cur.moveToNext()){
                tippableHours = cur.getDouble(0);
                tipsPerHour = cur.getDouble(1);
            }
            nameET.setText(selectedPartner);
            tippableHoursET.setText(Double.toString(tippableHours));
            tipsPerHourET.setText(Double.toString(tipsPerHour));
            //Calculate the partner's tips based on tips = tippable hours * the dollar per hour amount
            double tips = tippableHours * tipsPerHour;
            //Round the value up or down and truncate to the second decimal
            //tips = Math.floor(tips * 100)/100;
            tipsET.setText(Double.toString(tips));

            cur.close();
            db.close();
            crsDB.close();
        } catch (Exception ex){
            android.util.Log.w(this.getClass().getSimpleName(),"Exception getting partner info: "+
                    ex.getMessage());
        }
    }

    /**
     * addClicked
     *
     * Adds or updates the partner (by name) to the SQlite database
     * @param v
     */
    public void addClicked(View v){
        android.util.Log.d(this.getClass().getSimpleName(), "add Clicked. Adding: " + this.nameET.getText().toString());
        try{
            PartnerDB db = new PartnerDB((Context)this);
            SQLiteDatabase plcDB = db.openDB();
            ContentValues hm = new ContentValues();
            hm.put("name", this.nameET.getText().toString());
            double hours = Double.parseDouble(this.tippableHoursET.getText().toString());
            hm.put("tippableHours", hours);
            double tipsPerHour = Double.parseDouble(this.tipsPerHourET.getText().toString());
            hm.put("tipsPerHour", tipsPerHour);
            //double tips = Double.parseDouble(this.tipsET.getText().toString());
            //hm.put("tips", tips);
            plcDB.insert("partner",null, hm);
            plcDB.close();
            db.close();
            String addedName = this.nameET.getText().toString();
            setupselectSpinner1();
            this.selectedPartner = addedName;
            this.selectSpinner1.setSelection(Arrays.asList(partners).indexOf(this.selectedPartner));
        } catch (Exception ex){
            android.util.Log.w(this.getClass().getSimpleName(),"Exception adding partner information: "+
                    ex.getMessage());
        }
    }

    /**
     * removeClicked
     *
     * Removes the selected partner from the SQlite database
     * @param v
     */
    public void removeClicked(View v){
        android.util.Log.d(this.getClass().getSimpleName(), "remove Clicked");
        String delete = "delete from partner where partner.name=?;";
        try {
            PartnerDB db = new PartnerDB((Context) this);
            SQLiteDatabase plcDB = db.openDB();
            plcDB.execSQL(delete, new String[]{this.selectedPartner});
            plcDB.close();
            db.close();
        }catch(Exception e){
            android.util.Log.w(this.getClass().getSimpleName()," error trying to delete partner");
        }
        this.selectedPartner = this.setupselectSpinner1();
        this.loadFields();
    }


    public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
        android.util.Log.d(this.getClass().getSimpleName(), "onEditorAction: keycode " +
                ((event == null) ? "null" : event.toString()) + " actionId " + actionId);
        if(actionId== EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE){
            android.util.Log.d(this.getClass().getSimpleName(),"entry is: "+v.getText().toString());
        }
        return false; // without returning false, the keyboard will not disappear or move to next field
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        android.util.Log.d(this.getClass().getSimpleName(), "onClick with which= " +which);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(parent.getId() == R.id.selectSpinner1) {
            this.selectedPartner = selectSpinner1.getSelectedItem().toString();
            android.util.Log.d(this.getClass().getSimpleName(), "selectSpinner1 item selected " + selectedPartner);
            this.loadFields();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        android.util.Log.d(this.getClass().getSimpleName(), "onNothingSelected");
    }

}
