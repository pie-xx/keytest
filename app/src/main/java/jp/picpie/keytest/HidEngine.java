package jp.picpie.keytest;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class HidEngine {
    static final String TAG = "HidEngine";
    JSONObject defs;
    KeyInEventArray mMon;
    KeyInEventArray mStat;
    int mCodePageNo;

    String mOutput;

    JSONArray mCodePageS;

    HidEngine(){
        mMon = new KeyInEventArray();
        mStat = new KeyInEventArray();
        mOutput = "";
    }

    boolean LoadDefs( String defstr ){
        try {
            defs = new JSONObject( defstr );
        } catch (JSONException e) {
            e.printStackTrace();
            defs = null;
            return false;
        }

        try {
            mCodePageS = defs.getJSONArray("codepage");
            mCodePageNo = 0;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    void onKeyDown( boolean ismake, int keycode ){
        mMon.push( new AndroidKeyEvent( ismake, keycode));
        Log.d(TAG,"onKeyDown mMon-"+mMon.toString());
        mMon.collapse();

        mStat.push( new AndroidKeyEvent( ismake, keycode));
        Log.d(TAG,"onKeyDown mStat-"+mStat.toString());
        eval();
        mStat.collapse();
    }

    JSONObject getKgroup( String name ){
        try {
            JSONArray kgroups = mCodePageS.getJSONObject(mCodePageNo).getJSONArray("kgroup");
            for( int n=0; n < kgroups.length(); ++n){
                JSONObject kgroup = kgroups.getJSONObject(n);
                if( kgroup.getString("name").equals( name )){
                    return kgroup;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    int str2code(String str, JSONArray ctbl ){
        for( int n=0; n < ctbl.length(); ++n){
            try {
                JSONObject cdef = ctbl.getJSONObject(n);
                String cstr = cdef.getString("str");
                int key = cdef.getInt("key");
                if( str.equals(cstr)){
                    return key;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return -1;
            }
        }
        return -1;
    }

    String code2str(int kcode, JSONArray ctbl ){
        for( int n=0; n < ctbl.length(); ++n){
            try {
                JSONObject cdef = ctbl.getJSONObject(n);
                String cstr = cdef.getString("str");
                int key = cdef.getInt("key");
                if( key==kcode){
                    return cstr;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return "";
            }
        }
        return "";
    }

    JSONObject getObjFromArrayByName( JSONArray items, String name ){
        try {
            for( int n=0; n < items.length(); ++n){
                JSONObject k = items.getJSONObject(n);
                String itemname = k.getString("name");
                if( name.equals(itemname)){
                    return k;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    boolean isSameType( String statEvname, AndroidKeyEvent kev ){
        if( kev==null) {
            return false;
        }
        if( statEvname.startsWith("_")){
            if( kev.ismake ){
                return false;
            }
            statEvname = statEvname.substring(1);
        }else{
            if( ! kev.ismake ){
                return false;
            }
        }

        JSONObject kgroup = getKgroup( statEvname  );
        if( kgroup==null)
            return false;
        JSONArray ctbl=null;
        try {
            String ctblname = kgroup.getString("ctbl");
            JSONArray k2s = defs.getJSONArray("k2s");
            ctbl = getObjFromArrayByName( k2s, ctblname ).getJSONArray("ctbl");
        } catch (JSONException e) {
            e.printStackTrace();
            return true;
        }

        if( ctbl==null ){
            // keycode直接比較
            return true;
        }

        StringBuffer sb = new StringBuffer();
        try {
            JSONArray ktypes = kgroup.getJSONArray("in");
            for( int n=0; n < ktypes.length(); ++n){
                String ktype = ktypes.getString(n);
                sb.append(ktype+"("+String.valueOf(str2code(ktype, ctbl))+"),");
                if( str2code(ktype, ctbl)==kev.keycode ){
                    return true;
                }
            }
            return false;
        } catch (JSONException e) {
            try {
                JSONArray ktypes = kgroup.getJSONArray("notin");
                for( int n=0; n < ktypes.length(); ++n){
                    String ktype = ktypes.getString(n);
                    sb.append(ktype+"("+String.valueOf(str2code(ktype, ctbl))+"),");
                    if( str2code(ktype, ctbl)==kev.keycode ){
                        return false;
                    }
                }
                return true;
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    boolean checkRules( JSONArray statEvs){
        try {
            for( int n=0; n < statEvs.length(); ++n ){
                AndroidKeyEvent cmpkev = mStat.get(n);
                if( cmpkev == null ){
                    return false;
                }
                if( !isSameType( statEvs.getString(n), cmpkev)){
                    // 該当ルール処理
                    return false;
                }
            }
            StringBuffer sb = new StringBuffer();
            for( int n=0; n < statEvs.length(); ++n ){
                sb.append(statEvs.getString(n)+",");
            }
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    void eval(){
        mOutput = "";
        try {
            JSONArray rules = mCodePageS.getJSONObject(mCodePageNo).getJSONArray("rules");
            for( int n=0; n < rules.length(); ++n){
                JSONObject rule =(JSONObject) rules.get(n);
                JSONArray statsEvs = rule.getJSONArray("stat");
                if( checkRules(statsEvs) ){
                    try {
                        JSONArray outary = rule.getJSONArray("out");
                        for( int m=0; m < outary.length(); ++m) {
                            JSONObject outprm = outary.getJSONObject(m);
                            String ctblename = outprm.getString("ctbl");
                            int cno = outprm.getInt("cno");
                            JSONArray k2s = defs.getJSONArray("k2s");
                            JSONArray ctbl = getObjFromArrayByName(k2s, ctblename).getJSONArray("ctbl");
                            AndroidKeyEvent cmpkev = mStat.get(cno);
                            if (cmpkev != null) {
                                mOutput = mOutput + code2str(cmpkev.keycode, ctbl);
                            }
                            Log.d(TAG,mOutput +"-"+statsEvs.toString());
                        }
                    }catch (JSONException e){
                        String pagename = rule.getString("gopage");
                        setCodePage(pagename);
                    }

                    JSONArray rmnos = rule.getJSONArray("rmno");
                    for( int m=0; m < rmnos.length(); ++m){
                        int rmno = rmnos.getInt( m );
                        mStat.remove( rmno );
                    }
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    String getOutput(){
        return mOutput;
    }
    String getHid(){
        return StrToHID( mOutput);
    }

    String StrToHID( String str ){
        StringBuffer rtv = new StringBuffer();
        try {
            JSONObject s2h = defs.getJSONObject("s2h");
            JSONArray romaji = s2h.getJSONArray("romaji");
            for( int n=0; n < romaji.length(); ++n){
                JSONObject rt = romaji.getJSONObject(n);
                if(str.startsWith(rt.getString("str"))){
                    JSONArray ksq = rt.getJSONArray("seq");
                    for( int m=0; m < ksq.length(); ++m){
                        rtv.append( ksq.getString(m));
                    }
                    return rtv.toString();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "--";
    }

    ////////////////////////////////////////////////////
    void setCodePage(String pagename){
        try {
            for( int n =0; n < mCodePageS.length(); ++n){
                if( mCodePageS.getJSONObject(n).getString("name").equals( pagename )) {
                    mCodePageNo = n;
                    return;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

class HidKeyMachine{

    HidKeyMachine(){

    }
}

class HidKey {

}


class KeyInEventArray extends ArrayList<AndroidKeyEvent> {
    final static String TAG = "HidEngine";
    static final int LONGDISTCODE = 1000;
    static final int REPEATCODE = 1001;
    static final int LONGPUSHCODE = 1002;
    static long REPEATLIMIT = 500;
    static long LONGLIMIT = 800;

    @Override
    public AndroidKeyEvent get(int index){
        try {
            return super.get(index);
        }catch (java.lang.IndexOutOfBoundsException e){
            return null;
        }
    }

    @Override
    public AndroidKeyEvent remove(int index){
        try {
            return super.remove(index);
        }catch (java.lang.IndexOutOfBoundsException e){
            return null;
        }
    }

    void push( AndroidKeyEvent kv ){
        int inx = search( kv.keycode );
        if( kv.ismake ) {
            if (inx == -1) {
                // どっちが近いか
                if( size() >= 2){
                    int cinx = size() - 1;
                    long t0 = get(cinx - 1).t;
                    long t1 = get(cinx).t;
                    long t2 = kv.t;

                    if( t1 - t0 < t2 - t1 ){
                        add( new AndroidKeyEvent( true, LONGDISTCODE) );
                    }
                }
                add( kv );
            }else{
                Log.d(TAG,"SameKey");
                int cinx = size() - 1;
                long t1 = get(cinx).t;
                if( kv.t - t1 > REPEATLIMIT){
                    add( new AndroidKeyEvent( true, REPEATCODE) );
                }
            }
        }else{
            if( inx != -1){
                if( size() >= 2){
                    int cinx = size() - 1;
                    long t0 = get(cinx - 1).t;
                    long t1 = get(cinx).t;
                    long t2 = kv.t;

                    if( t1 - t0 < t2 - t1 ){
                        add( new AndroidKeyEvent( false, LONGDISTCODE ) );
                    }
                }else{
                    if( size() == 1 ) {
                        if (kv.t - get(0).t > LONGLIMIT) {
                            add(new AndroidKeyEvent(false, LONGPUSHCODE));
                        }
                    }
                }
                add( kv );
            }
        }
    }

    int search( int keycode ){
        for( int n = 0; n < size(); ++n ){
            if( get(n).keycode == keycode ){
                return n;
            }
        }
        return -1;
    }

    void collapse(){
        for( int n = size() - 1; n >= 0; --n ){
            AndroidKeyEvent kv = get(n);
            if( kv.keycode == LONGDISTCODE ||  kv.keycode == REPEATCODE){
                remove( n );
            }else {
                if (kv.ismake == false) {
                    for (int j = n - 1; j >= 0; --j) {
                        if (get(j).keycode == kv.keycode) {
                            get(j).ismake = false;
                        }
                    }
                    remove(n);
                }
            }
        }
    }

    String dumpkeyarray(){
        StringBuffer sb = new StringBuffer();
        Iterator<AndroidKeyEvent> ti = iterator();
        while( ti.hasNext() ){
            AndroidKeyEvent ko = ti.next();
            sb.append(String.format("%02x", ko.keycode));
            if( ko.ismake ) {
                sb.append("↓");
            }else{
                sb.append("↑");
            }
            sb.append(String.format("(%d)", ko.t));
        }
        return sb.toString();
    }
}

class AndroidKeyEvent {
    public boolean ismake;
    public int keycode;
    public long t;

    AndroidKeyEvent(boolean mb, int kc) {
        ismake = mb;
        keycode = kc;
        t = System.currentTimeMillis();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (ismake) {
            sb.append("↓");
        } else {
            sb.append("↑");
        }
        sb.append(String.valueOf(keycode));
        sb.append("(");
        sb.append(String.valueOf(t));
        sb.append("); ");
        return sb.toString();
    }
}