package com.ngbj.browser2.event;

public class CleanHistoryEvent {
    int index;
    public CleanHistoryEvent( int index){
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
