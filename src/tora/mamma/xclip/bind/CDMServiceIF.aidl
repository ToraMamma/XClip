package tora.mamma.xclip.bind;

import java.util.List;

interface CDMServiceIF {
    List getClipList();
    boolean registerClip(String str);
    void removeClip(int pos);
}