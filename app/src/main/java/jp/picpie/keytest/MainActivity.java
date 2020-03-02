package jp.picpie.keytest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    static HidEngine mHidEg;

    static final String TAG = "keytest";
    static KeyEventMan kem;
    TextView mKeyDisp;
    TextView mKeyCode;
    Button mEijiBtn;
    Button mKanaBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        kem = new KeyEventMan();
        final KeyEventMan _kem = kem;
        mKeyDisp = (TextView)findViewById(R.id.keydisp);
        mKeyCode = (TextView)findViewById(R.id.keycode);
        mEijiBtn = (Button)findViewById(R.id.eijibtn);
        mEijiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCodePage( 1 );
                dispMode();
            }
        });

        mKanaBtn = (Button)findViewById(R.id.kanabtn);
        mKanaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCodePage( 0 );
                dispMode();
            }
        });

        mHidEg = new HidEngine();
        try {
            InputStream inputStream = getAssets().open("keydef.json");
            byte[] buffer = new byte[10240];
            int length = 0;
            StringBuffer sb = new StringBuffer();
            while ((length = inputStream.read(buffer)) >= 0) {
                sb.append(new String( buffer, 0, length));
            }
            inputStream.close();

            mHidEg.LoadDefs( sb.toString() );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void setCodePage(int p){
        kem.setCodePage(p);
    }

    void dispMode(){
        switch( kem.getCodePage()){
            case 0:
                mKanaBtn.setEnabled(false);
                mEijiBtn.setEnabled(true);
                break;
            case 1:
                mKanaBtn.setEnabled(true);
                mEijiBtn.setEnabled(false);
                break;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        String buffer = mKeyDisp.getText().toString();
//        String instr = kem.addKeyEvent( e.getAction() == KeyEvent.ACTION_DOWN, e.getKeyCode() );
//        if( instr.equals("ESC") ){
//            buffer = "";
//        }
        mKeyCode.setText(String.format("%x", e.getKeyCode()));
//       mKeyDisp.setText(buffer+instr);
        dispMode();

        mHidEg.onKeyDown(e.getAction() == KeyEvent.ACTION_DOWN, e.getKeyCode() );
        if( ! mHidEg.getOutput().isEmpty()){
            mKeyDisp.setText(buffer+mHidEg.getOutput());
            if( mHidEg.getOutput().equals("ESC")){
                mKeyDisp.setText("");
            }
        }
        //return super.dispatchKeyEvent(e);
        return false;
    }
}
