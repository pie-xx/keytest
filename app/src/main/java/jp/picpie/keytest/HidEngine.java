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
        mMon.push( new HidKeyEvent( ismake, keycode));
        mMon.collapse();

        mStat.push( new HidKeyEvent( ismake, keycode));
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
                //    Log.d(TAG,str+"="+String.valueOf(key));
                    return key;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d(TAG,str+"="+e.toString());
                return -1;
            }
        }
        Log.d(TAG,str+"=notFound");
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

    boolean isSameType( String statEvname, HidKeyEvent kev ){
        //Log.d(TAG,"isSameType "+statEvname+" "+kev.toString());
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
                    //Log.d(TAG,"true "+String.valueOf(kev.keycode)+" in "+sb.toString());
                    return true;
                }
            }
            //Log.d(TAG,String.valueOf(kev.keycode)+" !in "+sb.toString());
            return false;
        } catch (JSONException e) {
            try {
                JSONArray ktypes = kgroup.getJSONArray("notin");
                for( int n=0; n < ktypes.length(); ++n){
                    String ktype = ktypes.getString(n);
                    sb.append(ktype+"("+String.valueOf(str2code(ktype, ctbl))+"),");
                    if( str2code(ktype, ctbl)==kev.keycode ){
                        //Log.d(TAG,String.valueOf(kev.keycode)+" !notin "+sb.toString());
                        return false;
                    }
                }
                //Log.d(TAG,"true "+String.valueOf(kev.keycode)+" notin "+sb.toString());
                return true;
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
        //Log.d(TAG,String.valueOf(kev.keycode)+" other "+sb.toString());
        return false;
    }

    boolean checkRules( JSONArray statEvs){
        try {
            for( int n=0; n < statEvs.length(); ++n ){
                HidKeyEvent cmpkev = mStat.get(n);
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
            Log.d(TAG, "checkRules "+sb.toString());
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
                    Log.d(TAG, "mStat "+mStat.dumpkeyarray() );
                    Log.d(TAG, "mMon "+mMon.dumpkeyarray() );
                    try {
                        JSONArray outary = rule.getJSONArray("out");
                        for( int m=0; m < outary.length(); ++m) {
                            JSONObject outprm = outary.getJSONObject(m);
                            String ctblename = outprm.getString("ctbl");
                            int cno = outprm.getInt("cno");
                            JSONArray k2s = defs.getJSONArray("k2s");
                            JSONArray ctbl = getObjFromArrayByName(k2s, ctblename).getJSONArray("ctbl");
                            HidKeyEvent cmpkev = mStat.get(cno);
                            Log.d(TAG, "mStat(" + String.valueOf(cno) + ")=key(" + String.valueOf(cmpkev.keycode) + ")/" + ctblename);
                            if (cmpkev != null) {
                                mOutput = mOutput + code2str(cmpkev.keycode, ctbl);
                                Log.d(TAG, "mOutput=" + mOutput);
                            }
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

class HidStateMachine{

    HidStateMachine(){

    }
}

class KeyInEventArray extends ArrayList<HidKeyEvent> {
    final static String TAG = "HidEngine";
    static final int LONGDISTCODE = 1000;
    static final int REPEATCODE = 1001;
    static long REPEATLIMIT = 500;

    KeyInEventArray() {
;
    }

    @Override
    public HidKeyEvent get(int index){
        try {
            return super.get(index);
        }catch (java.lang.IndexOutOfBoundsException e){
            //return new HidKeyEvent(false, -1);
            return null;
        }
    }

    @Override
    public HidKeyEvent remove(int index){
        try {
            return super.remove(index);
        }catch (java.lang.IndexOutOfBoundsException e){
        //    return new HidKeyEvent(false, -1);
            return null;
        }
    }

    void push( HidKeyEvent kv ){
        int inx = search( kv.keycode );
        if( kv.ismake ) {
            Log.d(TAG,"make inx="+String.valueOf(inx));
            if (inx == -1) {
                // どっちが近いか
                if( size() >= 2){
                    int cinx = size() - 1;
                    long t0 = get(cinx - 1).t;
                    long t1 = get(cinx).t;
                    long t2 = kv.t;

                    if( t2 - t1 > t1 - t0){
                        add( new HidKeyEvent( true, LONGDISTCODE) );
                    }
                }
                add( kv );
            }else{
                //todo: repeat key
                Log.d(TAG,"SameKey");
                int cinx = size() - 1;
                long t1 = get(cinx).t;
                if( kv.t - t1 > REPEATLIMIT){
                    add( new HidKeyEvent( true, REPEATCODE) );
                    Log.d(TAG, "REPEATCODE");
                }
            }
        }else{
            Log.d(TAG,"break inx="+String.valueOf(inx)+" size()="+String.valueOf(size()));
            if( inx != -1){
                if( size() >= 2){
                    int cinx = size() - 1;
                    long t0 = get(cinx - 1).t;
                    long t1 = get(cinx).t;
                    long t2 = kv.t;

                    if( t2 - t1 < t1 - t0){
                        add( new HidKeyEvent( false, LONGDISTCODE) );
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
            HidKeyEvent kv = get(n);
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
        Iterator<HidKeyEvent> ti = iterator();
        while( ti.hasNext() ){
            HidKeyEvent ko = ti.next();
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

class HidKeyEvent {
    public boolean ismake;
    public int keycode;
    public long t;

    HidKeyEvent( boolean mb, int kc ) {
        ismake = mb;
        keycode = kc;
        t = System.currentTimeMillis();
    }

    public String toString(){
        StringBuffer sb = new StringBuffer();
        if( ismake ){
            sb.append("↓");
        }else{
            sb.append("↑");
        }
        sb.append(String.valueOf(keycode));
        sb.append("("+String.valueOf(t)+"); ");
        return sb.toString();
    }
}
