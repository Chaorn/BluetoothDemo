package com.charon.www.bluetoothchuying.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.charon.www.bluetoothchuying.R;


public class EditActivity extends AppCompatActivity {
    private EditText mEditView;
    private int mDeviceId;
    private boolean mEditing =false;
    public SharedPreferences pref ;
    final int maxLen = 24;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);
        mEditView = (EditText)findViewById(R.id.edit_name);
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar_edit);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);
        pref= getSharedPreferences("myPref", MODE_PRIVATE);
        mToolbar.setNavigationIcon(R.drawable.back32);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        final Intent intent = getIntent();
        mDeviceId = intent.getIntExtra("id",0);
        mEditView.setFilters(new InputFilter[]{filter});
        mEditView.setText(pref.getString("Name" + mDeviceId, "EditError"));
        //设置软键盘的完成键
        mEditView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE){
                    save(mDeviceId);
                }
                return false;
            }
        });
        Log.d("123", mDeviceId + "EditAc");
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        if (!mEditing) {
            //mEditing为false
            mEditView.setEnabled(false);
            menu.findItem(R.id.menu_save).setVisible(false);
            menu.findItem(R.id.menu_edit).setVisible(true);
        } else {
            mEditView.setEnabled(true);
            menu.findItem(R.id.menu_save).setVisible(true);
            menu.findItem(R.id.menu_edit).setVisible(false);
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item ) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                editInformation(false,mDeviceId);
                break;
            case R.id.menu_edit:
                editInformation(true,mDeviceId);
                mEditView.setSelection(mEditView.getText().length());
                break;
        }
        return true;
    }
    private void editInformation(final boolean clickEditing,int id){
        if (clickEditing){
            //点击编辑
            mEditing = true;
            invalidateOptionsMenu();
        }else {
            save(id);
        }
    }
    private void save(int id){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        mEditing = false;
        invalidateOptionsMenu();
        String newName;
        newName = mEditView.getText().toString();
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("Name"+id, newName);
        editor.commit();
        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
    }
    //设置edit的字符数
    InputFilter filter = new InputFilter() {

        @Override
        public CharSequence filter(CharSequence src, int start, int end, Spanned dest, int dstart, int dend) {
            int dindex = 0;
            int count = 0;

            while (count <= maxLen && dindex < dest.length()) {
                char c = dest.charAt(dindex++);
                if (c < 128) {
                    count = count + 1;
                } else {
                    count = count + 2;
                }
            }

            if (count > maxLen) {
                return dest.subSequence(0, dindex - 1);
            }

            int sindex = 0;
            while (count <= maxLen && sindex < src.length()) {
                char c = src.charAt(sindex++);
                if (c < 128) {
                    count = count + 1;
                } else {
                    count = count + 2;
                }
            }

            if (count > maxLen) {
                sindex--;
            }

            return src.subSequence(0, sindex);
        }
    };
}
