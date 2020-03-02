package jp.picpie.keytest;

import android.util.Log;
import android.view.KeyEvent;

import java.util.ArrayList;
import java.util.Iterator;

public class KeyEventMan {
    final static String TAG = "KeyEventMan";
    ArrayList<KeyInEvent> keyEventArray;
    enum KeyType { KT_None, KT_CH_Mk, KT_CH_Br, KT_LS_Mk, KT_LS_Br, KT_RS_Mk, KT_RS_Br, KT_ES_Mk, KT_ES_Br, KT_EMODE, KT_ECAPS, KT_HIRA };

    static final int KC_EMODE = 0x1000;
    static final int KC_ECAPS = 0x1001;

    static int[] mKC_L_OSHIFTs = {KeyEvent.KEYCODE_MUHENKAN,0,0};
    static int[] mKC_R_OSHIFTs = {KeyEvent.KEYCODE_SPACE,KeyEvent.KEYCODE_HENKAN,0};
    static int[] mKC_E_SHIFTs =  {KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT, 0};
    static int[] mKC_E_MODEs =  {KC_EMODE, KeyEvent.KEYCODE_EISU, KeyEvent.KEYCODE_ZENKAKU_HANKAKU, 0};
    static int[] mKC_H_MODEs =  {KeyEvent.KEYCODE_KATAKANA_HIRAGANA, 0, 0};


    KeyOutTable kotbl;

    KeyEventMan() {
        keyEventArray = new ArrayList<KeyInEvent>();
        kotbl = new KeyOutTable();
    }

    public int getCodePage(){
        KeyInEvent kv0 = getKeyInEvent(0);
        KeyType kt0 = getKeyType( kv0);
        if( kt0 == KeyType.KT_EMODE ){
            return 1;
        }
        return 0;
    }

    public void setCodePage( int p ){
        keyEventArray.clear();
        switch( p ) {
            case 1: // 英数
                keyEventArray.add(new KeyInEvent(true, KC_EMODE));
                break;
            default:
                break;
        }
    }


    int search( int keycode ){
        for( int n = 0; n < keyEventArray.size(); ++n ){
            if( keyEventArray.get(n).keycode == keycode ){
                return n;
            }
        }
        return -1;
    }

    String addKeyEvent( boolean ismake, int keycode ){
        push( new KeyInEvent( ismake, keycode));
        dumpkeyarray();
        String str = evalKey();
        Log.d(TAG, "trans:"+str);
        collapse();
        dumpkeyarray();

        return str;
    }

    void dumpkeyarray(){
        StringBuffer sb = new StringBuffer();
        Iterator<KeyInEvent> ti = keyEventArray.iterator();
        while( ti.hasNext() ){
            KeyInEvent ko = ti.next();
            sb.append(String.format("%02x", ko.keycode));
            if( ko.ismake ) {
                sb.append("↓");
            }else{
                sb.append("↑");
            }
        }
        Log.d(TAG,sb.toString());
    }

    void push( KeyInEvent kv ){
        int inx = search( kv.keycode );
        if( kv.ismake ) {
            if (inx == -1) {
                keyEventArray.add(kv);
            }
        }else{
            if( inx != -1){
                keyEventArray.add( kv );
            }
        }
    }

    void collapse(){
        for( int n = keyEventArray.size() - 1; n >= 0; --n ){
            KeyInEvent kv = keyEventArray.get(n);
            if( kv.ismake != true ){
                for( int j = n-1; j >=0; --j ){
                    if( keyEventArray.get(j).keycode == kv.keycode ){
                        keyEventArray.get(j).ismake = false;
                    }
                }
                keyEventArray.remove(n);
            }
        }
    }

    String evalKey(){
        KeyInEvent kv0 = getKeyInEvent(0);
        KeyInEvent kv1 = getKeyInEvent(1);
        KeyInEvent kv2 = getKeyInEvent(2);
        KeyType kt0 = getKeyType( kv0);
        KeyType kt1 = getKeyType( kv1);
        KeyType kt2 = getKeyType( kv2);
        Log.d(TAG, kStr(kt0)+":"+kStr(kt1)+":"+kStr(kt2));
        String kstr = "?";

        if( kt0 == KeyType.KT_EMODE ){
            if( kt1 == KeyType.KT_ES_Mk ){
                if( kt2 == KeyType.KT_CH_Mk) {
                    kstr = kotbl.search(KeyOut.ShiftMode.SM_ESHIFT, kv2.keycode);
                    keyEventArray.remove(2);
                    return kstr;
                }
            }
            if( kt1 == KeyType.KT_CH_Mk && kv2!= null) {
                kstr = kotbl.search(KeyOut.ShiftMode.SM_EIJI, kv2.keycode);
                keyEventArray.remove(1);
                return kstr;
            }
            if( kt1 == KeyType.KT_HIRA ){
                keyEventArray.get(0).keycode = KC_EMODE;
                keyEventArray.remove(1);
                keyEventArray.remove(0);
                return "";
            }
            keyEventArray.get(0).keycode = KC_EMODE;
        }

        if( kt0== KeyType.KT_ES_Mk ){
            Log.d(TAG, "EM,CM");
            if( kt1 == KeyType.KT_CH_Mk ){
                Log.d(TAG, "EM,CM");
                // 文字キー確定
                kstr = kotbl.search(KeyOut.ShiftMode.SM_HSHIFT, kv1.keycode);
                keyEventArray.remove(1);
                return kstr;
            }
        }

        if( kt0==KeyType.KT_CH_Mk) {
            if( kt1 == KeyType.KT_CH_Br ){
                Log.d(TAG, "CM,CB");
                // 文字キー確定
                kstr = kotbl.search(KeyOut.ShiftMode.SM_None, keyEventArray.get(0).keycode);
                keyEventArray.remove(0);
                return kstr;
            }
            if( kt1 == KeyType.KT_CH_Mk ){
                Log.d(TAG, "CM,CM");
                // 文字キー確定
                kstr = kotbl.search(KeyOut.ShiftMode.SM_None, keyEventArray.get(0).keycode);
                keyEventArray.remove(0);
                return kstr;
            }
            if( kt1 == KeyType.KT_LS_Mk ){
                if( kt2 == KeyType.KT_CH_Br){
                    Log.d(TAG, "CM,LM,CB");
                    // 文字キー確定
                    kstr = kotbl.search(KeyOut.ShiftMode.SM_L_Oya, keyEventArray.get(0).keycode);
                    keyEventArray.remove(1);
                    keyEventArray.remove(0);
                    return kstr;
                }
                if( kt2 == KeyType.KT_CH_Mk){
                    Log.d(TAG, "CM,LM,CM");
                    // 文字キー確定
                    // 近い方にシフトをよせる
                    if( kv1.t - kv0.t < kv2.t - kv1.t) {
                        kstr = kotbl.search(KeyOut.ShiftMode.SM_L_Oya, keyEventArray.get(0).keycode);
                        keyEventArray.remove(1);
                        keyEventArray.remove(0);
                    }else{
                        kstr = kotbl.search(KeyOut.ShiftMode.SM_None, keyEventArray.get(0).keycode);
                        keyEventArray.remove(0);
                    }
                    return kstr;
                }
                if( kt2 == KeyType.KT_LS_Br){
                    Log.d(TAG, "CM,LM,LB");
                    // 文字キー確定
                    // 近い方にシフトをよせる
                    if( kv1.t - kv0.t < kv2.t - kv1.t) {
                        Log.d(TAG, "d0<d1");
                        kstr = kotbl.search(KeyOut.ShiftMode.SM_L_Oya, keyEventArray.get(0).keycode);
                        keyEventArray.remove(0);
                    }else{
                        Log.d(TAG, "d0>=d1");
                        kstr = kotbl.search(KeyOut.ShiftMode.SM_None, keyEventArray.get(0).keycode);
                        kstr = kstr + "○";
                        keyEventArray.remove(0);
                    }
                    return kstr;
                }
                if( kt2 == KeyType.KT_RS_Mk){
                    Log.d(TAG, "CM,LM,RM");
                    // 文字キー確定
                    kstr = kotbl.search(KeyOut.ShiftMode.SM_L_Oya, keyEventArray.get(0).keycode);
                    keyEventArray.remove(1);
                    keyEventArray.remove(0);
                    return kstr;
                }
                Log.d(TAG, "CM,LM,--");
            }
            if( kt1 == KeyType.KT_RS_Mk ){
                if( kt2 == KeyType.KT_CH_Br){
                    Log.d(TAG, "CM,RM,CB");
                    // 文字キー確定
                    kstr = kotbl.search(KeyOut.ShiftMode.SM_R_Oya, keyEventArray.get(0).keycode);
                    keyEventArray.remove(1);
                    return kstr;
                }
                if( kt2 == KeyType.KT_CH_Mk){
                    Log.d(TAG, "CM,RM,CM");
                    // 文字キー確定
                    // 近い方にシフトをよせる
                    if( kv1.t - kv0.t < kv2.t - kv1.t) {
                        kstr = kotbl.search(KeyOut.ShiftMode.SM_R_Oya, keyEventArray.get(0).keycode);
                        keyEventArray.remove(1);
                        keyEventArray.remove(0);
                    }else{
                        kstr = kotbl.search(KeyOut.ShiftMode.SM_None, keyEventArray.get(0).keycode);
                        keyEventArray.remove(0);
                    }
                    return kstr;
                }
                if( kt2 == KeyType.KT_RS_Br){
                    Log.d(TAG, "CM,RM,RB");
                    // 文字キー確定
                    // 近い方にシフトをよせる
                    if( kv1.t - kv0.t < kv2.t - kv1.t) {
                        Log.d(TAG, "d0<d1");
                        kstr = kotbl.search(KeyOut.ShiftMode.SM_R_Oya, keyEventArray.get(0).keycode);
                        keyEventArray.remove(0);
                    }else{
                        Log.d(TAG, "d0>=d1");
                        kstr = kotbl.search(KeyOut.ShiftMode.SM_None, keyEventArray.get(0).keycode);
                        kstr = kstr + "●";
                        keyEventArray.remove(0);
                    }
                    return kstr;
                }
                if( kt2 == KeyType.KT_LS_Mk){
                    Log.d(TAG, "CM,RM,LM");
                    // 文字キー確定
                    kstr = kotbl.search(KeyOut.ShiftMode.SM_R_Oya, keyEventArray.get(0).keycode);
                    keyEventArray.remove(1);
                    keyEventArray.remove(0);
                    return kstr;
                }
                Log.d(TAG, "CM,RM,--");
            }
        }

        if( kt0==KeyType.KT_LS_Mk ){
            if( kt1 == KeyType.KT_LS_Br ){
                Log.d(TAG, "LM,LB");
                // 単独シフト確定
                return "○";
            }
            if( kt1 == KeyType.KT_CH_Mk ){
                if( kt2 == KeyType.KT_LS_Br){
                    Log.d(TAG, "LM,CM,LB");
                    // 文字キー確定
                    kstr = kotbl.search(KeyOut.ShiftMode.SM_L_Oya, keyEventArray.get(1).keycode);
                    keyEventArray.remove(1);
                    keyEventArray.remove(0);
                    return kstr;
                }
                if( kt2 == KeyType.KT_CH_Br){
                    Log.d(TAG, "LM,CM,CB");
                    // 文字キー確定
                    kstr = kotbl.search(KeyOut.ShiftMode.SM_L_Oya, keyEventArray.get(1).keycode);
                    keyEventArray.remove(1);
                    keyEventArray.remove(0);
                    return kstr;
                }
                if( kt2 == KeyType.KT_CH_Mk){
                    Log.d(TAG, "LM,CM,CM");
                    // 文字キー確定
                    kstr = kotbl.search(KeyOut.ShiftMode.SM_L_Oya, keyEventArray.get(1).keycode);
                    keyEventArray.remove(1);
                    keyEventArray.remove(0);
                    return kstr;
                }
                Log.d(TAG, "LM,CM,--");
            }
        }


        if( kt0==KeyType.KT_RS_Mk ){
            if( kt1 == KeyType.KT_RS_Br ){
                Log.d(TAG, "RM,RB");
                // 単独シフト確定
                return "●";
            }
            if( kt1 == KeyType.KT_CH_Mk ){
                if( kt2 == KeyType.KT_RS_Br){
                    Log.d(TAG, "RM,CM,RB");
                    // 文字キー確定
                    kstr = kotbl.search(KeyOut.ShiftMode.SM_R_Oya, keyEventArray.get(1).keycode);
                    keyEventArray.remove(1);
                    keyEventArray.remove(0);
                    return kstr;
                }
                if( kt2 == KeyType.KT_CH_Br){
                    Log.d(TAG, "RM,CM,CB");
                    // 文字キー確定
                    kstr = kotbl.search(KeyOut.ShiftMode.SM_R_Oya, keyEventArray.get(1).keycode);
                    keyEventArray.remove(1);
                    keyEventArray.remove(0);
                    return kstr;
                }
                if( kt2 == KeyType.KT_CH_Mk){
                    Log.d(TAG, "RM,CM,CM");
                    // 文字キー確定
                    kstr = kotbl.search(KeyOut.ShiftMode.SM_R_Oya, keyEventArray.get(1).keycode);
                    keyEventArray.remove(1);
                    keyEventArray.remove(0);
                    return kstr;
                }
            }
        }

        return "";
    }

    KeyInEvent getKeyInEvent( int inx ){
        if( inx >= keyEventArray.size() ){
            return null;
        }
        return keyEventArray.get(inx);
    }

    boolean InThis(int keycode, int[] typekeys){
        for( int n=0; n < typekeys.length; ++n){
            if( typekeys[n]==0){
                return false;
            }
            if( keycode == typekeys[n]){
                return true;
            }
        }
        return false;
    }

    KeyType getKeyType( KeyInEvent kv ){
        if( kv == null ){
            return KeyType.KT_None;
        }

        if(  InThis( kv.keycode, mKC_L_OSHIFTs )){
            if( kv.ismake ){
                return KeyType.KT_LS_Mk;
            }
            return KeyType.KT_LS_Br;
        }
        if( InThis( kv.keycode, mKC_R_OSHIFTs ) ){
            if( kv.ismake ){
                return KeyType.KT_RS_Mk;
            }
            return KeyType.KT_RS_Br;
        }
        //if( kv.keycode == KeyEvent.KEYCODE_SHIFT_LEFT || kv.keycode == KeyEvent.KEYCODE_SHIFT_RIGHT){
        if( InThis( kv.keycode, mKC_E_SHIFTs ) ){
            if( kv.ismake ){
                return KeyType.KT_ES_Mk;
            }
            return KeyType.KT_ES_Br;
        }
        if( InThis( kv.keycode, mKC_E_MODEs ) ){
            if( kv.ismake ){
                return KeyType.KT_EMODE;
            }
        }
        if( InThis( kv.keycode, mKC_H_MODEs ) ){
            if( kv.ismake ){
                return KeyType.KT_HIRA;
            }
        }

        if( kv.ismake ){
            return KeyType.KT_CH_Mk;
        }
        return KeyType.KT_CH_Br;
    }

    String kStr( KeyType kt ){
        switch(kt){
            case KT_None: return "None";
            case KT_CH_Mk: return "CH_Mk";
            case KT_CH_Br: return "CH_Br";
            case KT_LS_Mk: return "LS_Mk";
            case KT_LS_Br: return "LS_Br";
            case KT_RS_Mk: return "RS_Mk";
            case KT_RS_Br: return "RS_Br";
        }
        return "--";
    }

}

class KeyInEvent {
    public boolean ismake;
    public int keycode;
    public long t;
    int[] O_SHIFTS = { 0xd5, 0x3e, 0 };

    KeyInEvent( boolean mb, int kc ) {
        ismake = mb;
        keycode = kc;
        t = System.currentTimeMillis();
    }

    public boolean isShift(){
        for( int n = 0; n < O_SHIFTS.length; ++n ) {
            if( O_SHIFTS[n] == 0 ){
                break;
            }
            if (keycode == O_SHIFTS[n]){
                return true;
            }
        }

        return false;
    }
}

class KeyOutTable {
    static final String TAG = "KeyOutTable";
    ArrayList<KeyOut> tbl;

    KeyOutTable() {
        tbl = new ArrayList<KeyOut>();
        setTable(KeyEvent.KEYCODE_1,"1","？","！", "", "1", "!");
        setTable(KeyEvent.KEYCODE_2,"2","／","―", "", "2", "\"");
        setTable(KeyEvent.KEYCODE_3,"3","～","…", "", "3", "#");
        setTable(KeyEvent.KEYCODE_4,"4","「","「」", "", "4", "$");
        setTable(KeyEvent.KEYCODE_5,"5","」","＊", "", "5", "%");
        setTable(KeyEvent.KEYCODE_6,"6","［］","［", "", "6", "&");
        setTable(KeyEvent.KEYCODE_7,"7","＋","］", "", "7", "'");
        setTable(KeyEvent.KEYCODE_8,"8","（）","（", "", "8", "(");
        setTable(KeyEvent.KEYCODE_9,"9","●","）", "", "9", ")");
        setTable(KeyEvent.KEYCODE_0,"0","：","『", "", "0", ":");
        setTable(KeyEvent.KEYCODE_MINUS,"-","＝","』", "", "-", "=");
        setTable(KeyEvent.KEYCODE_EQUALS,"^","￣","｛", "", "^", "~");
        setTable(KeyEvent.KEYCODE_YEN,"￥","｜","｝", "", "\\", "|");

        setTable(KeyEvent.KEYCODE_A,"う","を","ヴ", "", "a", "A");
        setTable(KeyEvent.KEYCODE_B,"へ","ぃ","べ",  "ぺ","b", "B");
        setTable(KeyEvent.KEYCODE_C,"す","ろ","ず", "", "c", "C");
        setTable(KeyEvent.KEYCODE_D,"て","な","で", "", "d", "D");
        setTable(KeyEvent.KEYCODE_E,"た","り","だ", "", "e", "E");
        setTable(KeyEvent.KEYCODE_F,"け","ゅ","げ", "", "f", "F");
        setTable(KeyEvent.KEYCODE_G,"せ","も","ぜ", "", "g", "G");
        setTable(KeyEvent.KEYCODE_H,"は","ば","み", "ぱ", "h", "H");
        setTable(KeyEvent.KEYCODE_I,"く","ぐ","る", "", "i", "I");
        setTable(KeyEvent.KEYCODE_J,"と","ど","お", "", "j", "J");
        setTable(KeyEvent.KEYCODE_K,"き","ぎ","の", "", "k", "K");
        setTable(KeyEvent.KEYCODE_L,"い","ぽ","ょ", "", "l", "L");
        setTable(KeyEvent.KEYCODE_M,"そ","ぞ","ゆ", "", "m", "M");
        setTable(KeyEvent.KEYCODE_N,"め","ぷ","ぬ", "", "n", "N");
        setTable(KeyEvent.KEYCODE_O,"つ","づ","ま", "", "o", "O");
        setTable(KeyEvent.KEYCODE_P,"，","ぴ","ぇ", "", "p", "P");
        setTable(KeyEvent.KEYCODE_Q,"。","ぁ","ゐ", "", "q", "Q");
        setTable(KeyEvent.KEYCODE_R,"こ","ゃ","ご", "", "r", "R");
        setTable(KeyEvent.KEYCODE_S,"し","あ","じ", "", "s", "S");
        setTable(KeyEvent.KEYCODE_T,"さ","れ","ざ", "", "t", "T");
        setTable(KeyEvent.KEYCODE_U,"ち","ぢ","に", "", "u", "U");
        setTable(KeyEvent.KEYCODE_V,"ふ","や","ぶ", "ぷ", "v", "V");
        setTable(KeyEvent.KEYCODE_W,"か","え","が", "", "w", "W");
        setTable(KeyEvent.KEYCODE_X,"ひ","ー","び", "ぴ", "x", "X");
        setTable(KeyEvent.KEYCODE_Y,"ら","ぱ","よ", "", "y", "Y");
        setTable(KeyEvent.KEYCODE_Z,"．","ぅ","ゑ", "", "z", "Z");
        setTable(KeyEvent.KEYCODE_COMMA,"ね","ぺ","む", "", ",", "<");
        setTable(KeyEvent.KEYCODE_PERIOD,"ほ","ぼ","わ", "ぽ", ".", ">");
        setTable(KeyEvent.KEYCODE_ALT_LEFT,"左Alt","","", "", "", "");
        setTable(KeyEvent.KEYCODE_ALT_RIGHT,"右Alt","","", "", "", "");
        setTable(KeyEvent.KEYCODE_SHIFT_LEFT,"左シフト","","", "", "", "");
        setTable(KeyEvent.KEYCODE_SHIFT_RIGHT,"右シフト","","", "", "", "");
        setTable(KeyEvent.KEYCODE_TAB,"Tab","","", "", "", "");
        setTable(KeyEvent.KEYCODE_SPACE,"Space","","", "　", " ", " ");
        setTable(KeyEvent.KEYCODE_SYM,"","","", "", "", "");
        setTable(KeyEvent.KEYCODE_EXPLORER,"","","", "", "", "");
        setTable(KeyEvent.KEYCODE_ENVELOPE,"","","", "", "", "");
        setTable(KeyEvent.KEYCODE_ENTER,"\n","","", "", "\n", "");
        setTable(KeyEvent.KEYCODE_DEL,"BS","","", "", "", "");
        setTable(KeyEvent.KEYCODE_GRAVE,"","","", "", "", "");
        setTable(KeyEvent.KEYCODE_LEFT_BRACKET,"、","、","、", "", "@", "`");
        setTable(KeyEvent.KEYCODE_RIGHT_BRACKET,"゛","゛","゜", "", "[", "{");
        setTable(KeyEvent.KEYCODE_BACKSLASH,"-","","", "", "]", "}");
        setTable(KeyEvent.KEYCODE_SEMICOLON,"ん","","っ", "", ";", "+");
        setTable(KeyEvent.KEYCODE_APOSTROPHE,"-","","", "", ":", "*");
        setTable(KeyEvent.KEYCODE_SLASH,"・","","ぉ", "", "/", "?");
        setTable(KeyEvent.KEYCODE_ESCAPE,"ESC","","", "", "", "");
        setTable(KeyEvent.KEYCODE_RO,"￥","","", "", "\\", "_");

        Log.d(TAG,"OkanaNone");
        ListTable(KeyOut.ShiftMode.SM_None);
        Log.d(TAG,"OkanaLoya");
        ListTable(KeyOut.ShiftMode.SM_L_Oya);
        Log.d(TAG,"OkanaRoya");
        ListTable(KeyOut.ShiftMode.SM_R_Oya);
        Log.d(TAG,"OkanaHShift");
        ListTable(KeyOut.ShiftMode.SM_HSHIFT);
        Log.d(TAG,"EijiNone");
        ListTable(KeyOut.ShiftMode.SM_EIJI);
        Log.d(TAG,"EijiShift");
        ListTable(KeyOut.ShiftMode.SM_ESHIFT);
    }

    void ListTable( KeyOut.ShiftMode sm ){
        Iterator<KeyOut> it = tbl.iterator();
        while( it.hasNext() ){
            KeyOut ko = it.next();
            if( ko.shift == sm ){
                Log.d(TAG,"{ \"key\":"+String.format("%d",ko.keycode)+", \"str\": \""+ko.str+"\" }");
            }
        }
    }

    void setTable( int keycode, String none, String left, String right, String hshift, String eiji, String eshift){
        tbl.add( new KeyOut( KeyOut.ShiftMode.SM_None, keycode, none) );
        tbl.add( new KeyOut( KeyOut.ShiftMode.SM_L_Oya, keycode, left) );
        tbl.add( new KeyOut( KeyOut.ShiftMode.SM_R_Oya, keycode, right) );
        tbl.add( new KeyOut( KeyOut.ShiftMode.SM_HSHIFT, keycode, hshift) );
        tbl.add( new KeyOut( KeyOut.ShiftMode.SM_EIJI, keycode, eiji) );
        tbl.add( new KeyOut( KeyOut.ShiftMode.SM_ESHIFT, keycode, eshift) );
    }

    String search( KeyOut.ShiftMode sft, int keycode ){
        Iterator<KeyOut> ti = tbl.iterator();
        while( ti.hasNext() ){
            KeyOut ko=ti.next();
            if( ko.shift == sft && ko.keycode==keycode){
                return ko.str;
            }
        }
        return "";
    }
}

class KeyOut {
    int keycode;
    ShiftMode shift;
    int[] codeseq;
    String str;
    enum ShiftMode {SM_None, SM_L_Oya, SM_R_Oya, SM_HSHIFT, SM_EIJI, SM_ESHIFT };

    KeyOut( ShiftMode _shift, int _kc, String _str ){
        keycode = _kc;
        shift = _shift;
        str = _str;
    }
}