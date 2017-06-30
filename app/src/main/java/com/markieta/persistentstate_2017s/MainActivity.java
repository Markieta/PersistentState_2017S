package com.markieta.persistentstate_2017s;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.provider.BaseColumns;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_FILE_NAME = "mySPfile";

    private static class MyDataEntry implements BaseColumns{
        static final String TABLE_NAME = "student_grades";
        static final String STUDENT_ID_FIELD = "student_id";
        static final String STUDENT_GRADE_FIELD = "student_grade";
    }

    private class MyDbHelper extends SQLiteOpenHelper{

        static final String DB_NAME = "MyCoolDatabase.db";
        static final int DB_VERSION = 1;

        private static final String SQL_CREATE_TABLE_QUERY = "CREATE TABLE " + MyDataEntry.TABLE_NAME + " (" +
                MyDataEntry._ID + " INTEGER PRIMARY KEY," + MyDataEntry.STUDENT_ID_FIELD + " TEXT," +
                MyDataEntry.STUDENT_GRADE_FIELD + " TEXT )";

        private static final String SQL_DELETE_QUERY = "DROP TABLE IF EXISTS " + MyDataEntry.TABLE_NAME;

        MyDbHelper(Context context){
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i("DEBUG", "Executing Query: SQL_CREATE_TABLE " + SQL_CREATE_TABLE_QUERY);
            db.execSQL(SQL_CREATE_TABLE_QUERY);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i("DEBUG", "Executing Query: SQL_DELETE_QUERY " + SQL_DELETE_QUERY);
            db.execSQL(SQL_DELETE_QUERY);
            onCreate(db);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadSharedPreferences();

        //handler for the Add button that will write records to the database
        //TODO: also add these to the ListView or LinearLayout as you have chosen
        Button saveGradeButton = (Button) findViewById(R.id.button);
        saveGradeButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                //delegate the actual work to a method...
                saveGrade();
            }
        });

        loadDatabase();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveSharedPreferences();
    }

    protected void saveSharedPreferences(){
        Log.i("DEBUG", "saveSharedPreferences was called");

        SharedPreferences sharedPreferences = getSharedPreferences(PREF_FILE_NAME, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        EditText studentID_EditText = (EditText) findViewById(R.id.editText_ID);
        long studentId = Long.parseLong(studentID_EditText.getText().toString());

        editor.putLong("studentID", studentId); //TODO: use a static constant for the key instead!
        //TODO: you will also save the grade for next time!!

        editor.apply();
    }

    protected void loadSharedPreferences(){
        Log.i("DEBUG", "loadSharedPreferences was called");

        SharedPreferences sharedPreferences = getSharedPreferences(PREF_FILE_NAME, 0); //mode 0 means private

        long studentId = sharedPreferences.getLong("studentID", -1);
        if(studentId > 0){
            EditText studentID_EditText = (EditText) findViewById(R.id.editText_ID);
            studentID_EditText.setText(""+studentId);
        }
        //TODO: get the grade as well!

    }

    protected void saveGrade(){
        /*
        here we write the student id and grade to the database
        ideally, any work with the database should be done in an AsyncTask
        because these are potentially long running operations if the database is large
        //TODO: put this in an AsyncTask
         */

        MyDbHelper helper = new MyDbHelper(this);
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues newRow = new ContentValues();

        EditText studentID_EditText = (EditText) findViewById(R.id.editText_ID);
        long studentId = Long.parseLong(studentID_EditText.getText().toString());
        EditText studentGrade_EditText = (EditText) findViewById(R.id.editText_Grade);
        String studentGrade = studentGrade_EditText.getText().toString();
        newRow.put(MyDataEntry.STUDENT_ID_FIELD, studentId);
        newRow.put(MyDataEntry.STUDENT_GRADE_FIELD, studentGrade);
        Log.i("DEBUG", "writing a new row to the database: "+ studentId + " " + studentGrade);

        long newRowId = db.insert(MyDataEntry.TABLE_NAME, null, newRow);
        Log.i("DEBUG", "result of database insertion: "+ newRowId);
    }

    protected void loadDatabase(){
        MyDbHelper helper = new MyDbHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();

        String[] query_columns = {
                MyDataEntry._ID,
                MyDataEntry.STUDENT_ID_FIELD,
                MyDataEntry.STUDENT_GRADE_FIELD
        };

        String selectQuery = MyDataEntry.STUDENT_ID_FIELD + " = ?";
        String[] selectionArgs = {" Filter string "};
        String sortOrder = MyDataEntry.STUDENT_ID_FIELD + " DESC";

        Cursor cursor = db.query(
                MyDataEntry.TABLE_NAME,
                query_columns,
                null,
                null,
                null,
                null,
                sortOrder
        );

        boolean hasMoreData = cursor.moveToFirst();
        while(hasMoreData){
            long key = cursor.getLong(cursor.getColumnIndexOrThrow(MyDataEntry._ID));
            String studentID = cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.STUDENT_ID_FIELD));
            String studentGrade = cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.STUDENT_GRADE_FIELD));

            System.out.println("RECORD KEY: " + key + " student id: " + studentID + " student grade: " + studentGrade);
            //TODO: for your lab you will populate an ArrayList that backs a ListView (or use a LinearLayout)

            hasMoreData = cursor.moveToNext();
        }

        cursor.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.max_records:
                buildDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void buildDialog() {
        final EditText recordsEditText = new EditText(this);
        recordsEditText.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        recordsEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

        AlertDialog.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }

        builder.setTitle("Enter Max Records Per Page")
                .setView(recordsEditText)
                .setPositiveButton(R.string.max_records_accept, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO: Update rows
                    }
                })
                .setNegativeButton(R.string.max_records_decline, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                }).show();
    }
}
